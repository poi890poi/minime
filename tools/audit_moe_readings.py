"""Local-only pronunciation joins against unchanged official MOE archives."""
import collections
import csv
import hashlib
from io import BytesIO
import json
from pathlib import Path
import re
import unicodedata
from zipfile import ZipFile
import openpyxl

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / 'artifacts/moe-reading-reference'
ARCHIVES = {
    'concise': ('dict_concised_2014_20260626.zip', 'fc83d27eb3fbf6fcfdb791e7d05ef60946b58ef8e8857ed165b612217b392806'),
    'elementary': ('dict_mini_2019_20260626.zip', '3067e61841f90a1ba1edff99607b6cb1a081ec1868b6820b6b9a78bed5a2f06a'),
}


def single_han(text):
    return len(text) == 1 and unicodedata.name(text, '').startswith(('CJK UNIFIED IDEOGRAPH-', 'CJK COMPATIBILITY IDEOGRAPH-'))


def zhuyin(raw):
    value = str(raw or '').strip()
    if not re.fullmatch('[ㄅ-ㄩˊˇˋ˙ˉ]+', value):
        return None
    if value.startswith('˙'):
        value = value[1:] + '˙'
    if value[-1] not in 'ˊˇˋ˙ˉ':
        value += 'ˉ'
    return value if re.fullmatch('[ㄅ-ㄩ]+[ˊˇˋ˙ˉ]', value) else None


def pinyin(raw):
    value = unicodedata.normalize('NFD', str(raw or '').strip().lower())
    value = ''.join(c for c in value if c not in '\u0304\u0301\u030c\u0300')
    value = unicodedata.normalize('NFC', value).replace('ü', 'v')
    value = "'".join(value.split())
    return value if re.fullmatch("[a-zv]+(?:'[a-zv]+)*", value) else None


def reference(kind):
    filename, expected = ARCHIVES[kind]
    path = CACHE / filename
    assert hashlib.sha256(path.read_bytes()).hexdigest() == expected
    stats = collections.Counter()
    glyphs, words = collections.defaultdict(set), collections.defaultdict(set)
    unresolved = set()
    with ZipFile(path) as archive:
        assert len(archive.namelist()) == 1 and archive.namelist()[0].endswith('.xlsx')
        data = archive.read(archive.namelist()[0])
        workbook = openpyxl.load_workbook(BytesIO(data), read_only=True, data_only=True)
        assert len(workbook.worksheets) == 1
        rows = workbook.active.iter_rows(values_only=True)
        columns = {v: i for i, v in enumerate(next(rows))}
        title, bopomofo = ('字詞名', '注音一式') if kind == 'concise' else ('單字', '注音')
        for row in rows:
            stats['source_rows'] += 1
            word = str(row[columns[title]] or '').strip()
            if not word:
                stats['empty_titles'] += 1
                continue
            if single_han(word):
                stats['single_han_rows'] += 1
                reading = zhuyin(row[columns[bopomofo]])
                if reading:
                    glyphs[word].add(reading)
                else:
                    stats['unparsed_single_han_bopomofo'] += 1
            else:
                stats['non_single_han_rows'] += 1
            if kind == 'concise':
                reading = pinyin(row[columns['漢語拼音']])
                if reading:
                    words[word].add(reading.replace("'", ''))
                else:
                    stats['unparsed_word_pinyin'] += 1
                    unresolved.add(word)
        workbook.close()
    stats['distinct_single_han_glyphs'] = len(glyphs)
    stats['single_han_reading_pairs'] = sum(map(len, glyphs.values()))
    stats['single_han_glyphs_with_multiple_readings'] = sum(len(v) > 1 for v in glyphs.values())
    return glyphs, words, unresolved, dict(stats), hashlib.sha256(data).hexdigest()


def main():
    refs = {kind: reference(kind) for kind in ARCHIVES}
    source = ROOT / 'artifacts/mcbopomofo-audit/upstream'
    tiers = {}
    for tier in (1, 2, 3):
        pairs = set()
        for line in (source / f'heterophony{tier}.list').read_text(encoding='utf-8').splitlines():
            if not line.strip() or line.startswith('#'):
                continue
            word, raw = line.split()
            reading = zhuyin(raw)
            assert single_han(word) and reading, 'Unexpected source tier row'
            pairs.add((word, reading))
        tiers[tier] = pairs
    comparison, detail = {}, []
    for kind, (glyphs, _, _, _, _) in refs.items():
        comparison[kind] = {}
        for tier, pairs in tiers.items():
            counts = collections.Counter(source_pairs=len(pairs))
            for word, reading in sorted(pairs):
                status = 'glyph_uncovered' if word not in glyphs else 'reading_present' if reading in glyphs[word] else 'reading_absent_on_covered_glyph'
                counts[status] += 1
                detail.append(dict(reference=kind, tier=tier, word=word, reading=reading, status=status))
            comparison[kind][str(tier)] = dict(counts)
    _, words, unresolved, _, _ = refs['concise']
    evaluation = collections.defaultdict(collections.Counter)
    source_input = ROOT / 'docs/conversation-ranking/corpus/inputs.tsv'
    for group, _, raw, word in csv.reader(source_input.open(encoding='utf-8'), delimiter='\t'):
        if not group.startswith('zh-') or not group.endswith('-full'):
            continue
        status = ('unparsed_reference_reading' if word in unresolved else 'word_uncovered' if word not in words
                  else 'spelling_present' if raw in words[word] else 'spelling_absent_on_covered_word')
        for bucket in ('all', group, 'single-glyph' if single_han(word) else 'multi-glyph'):
            evaluation[bucket]['cases'] += 1
            evaluation[bucket][status] += 1
            if word in words:
                evaluation[bucket]['reference_word_has_multiple_spellings'] += len(words[word]) > 1
        detail.append(dict(reference='concise-evaluation', group=group, word=word, raw=raw, status=status))
    report = dict(scope='Independent pronunciation-reference audit only; no production extraction, relabelling or frequency claim',
        source_id='moe-dictionaries', attribution='中華民國教育部（Ministry of Education, R.O.C.）',
        licence='CC-BY-ND-3.0-TW; original archives and full usage notices retained locally',
        reference_inventory={k: dict(v[3], xlsx_sha256=v[4], archive=ARCHIVES[k][0], archive_sha256=ARCHIVES[k][1]) for k, v in refs.items()},
        priority_comparison=comparison, evaluation_comparison={k: dict(v) for k, v in evaluation.items()},
        source_input_sha256=hashlib.sha256(source_input.read_bytes()).hexdigest(),
        tier_hashes={str(t): hashlib.sha256((source / f'heterophony{t}.list').read_bytes()).hexdigest() for t in tiers})
    (CACHE / 'joins.json').write_bytes((json.dumps(detail, ensure_ascii=False, indent=2) + '\n').encode())
    report['local_joins_sha256'] = hashlib.sha256((CACHE / 'joins.json').read_bytes()).hexdigest()
    (CACHE / 'summary.json').write_bytes((json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
