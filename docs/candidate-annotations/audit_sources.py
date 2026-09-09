"""Read-only source availability audit; no production selection or ranking."""
from pathlib import Path
from collections import defaultdict
import csv, hashlib, json, tarfile, unicodedata

ROOT = Path(__file__).resolve().parents[2]
normalize = lambda value: unicodedata.normalize('NFC', value.strip())

def has_han(value):
    return any('\u3400' <= c <= '\u9fff' or 0x20000 <= ord(c) <= 0x323af for c in value)

def audit():
    inputs = ['app/src/main/assets/addons.tsv', 'third_party/itaigi/itaigi.csv',
              'third_party/jmdict/jmdict-eng-common.json.tgz']
    rows = [line.split('\t') for line in (ROOT / inputs[0]).read_text(encoding='utf8').splitlines()
            if line and not line.startswith('#')]
    with (ROOT / inputs[1]).open(encoding='utf-8-sig') as stream:
        itaigi = list(csv.DictReader(stream))
    examples = defaultdict(set)
    for entry in itaigi:
        if entry['PojUnicode'].strip() and has_han(entry['HanLoTaibunPoj']):
            examples[normalize(entry['PojUnicode'])].add(entry['HanLoTaibunPoj'].strip())
    groups = defaultdict(set)
    for row in rows:
        if row[0] == 'poj':
            groups[row[4]].add(normalize(row[2]))
    all_poj = set().union(*groups.values())
    with tarfile.open(ROOT / inputs[2]) as archive:
        data = json.load(archive.extractfile(next(m for m in archive.getmembers() if m.name.endswith('.json'))))
    selected = [word for word in data['words'] if any(set(s['partOfSpeech']) & {'exp', 'int'} for s in word['sense'])]
    readings, compatible, common_han, kana_usually = set(), set(), set(), set()
    for word in selected:
        for kana in word['kana']:
            if not kana['common']:
                continue
            senses = [s for s in word['sense'] if set(s['partOfSpeech']) & {'exp', 'int'}
                      and ('*' in s['appliesToKana'] or kana['text'] in s['appliesToKana'])]
            if not senses:
                continue
            readings.add(kana['text'])
            if any('uk' in s['misc'] for s in senses):
                kana_usually.add(kana['text'])
            for spelling in word['kanji']:
                if ('*' in kana['appliesToKanji'] or spelling['text'] in kana['appliesToKanji']) and any(
                        '*' in s['appliesToKanji'] or spelling['text'] in s['appliesToKanji'] for s in senses):
                    compatible.add(kana['text'])
                    if spelling['common'] and has_han(spelling['text']):
                        common_han.add(kana['text'])
    return dict(
        sha256={name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest() for name in inputs},
        poj_unique_outputs=len(all_poj),
        poj_exact_NFC_reading_with_HanLo_from_existing_itaigi=len(all_poj & examples.keys()),
        poj_groups={name: dict(outputs=len(values), exact_itaigi_HanLo_example_available=len(values & examples.keys()))
                    for name, values in groups.items()},
        itaigi_records=len(itaigi), itaigi_unique_HanLo_readings=len(examples),
        itaigi_readings_multiple_examples=sum(len(values) > 1 for values in examples.values()),
        jmdict_common_records=len(data['words']), jmdict_expression_or_interjection_records=len(selected),
        jmdict_common_expression_readings=len(readings), with_compatible_spelling=len(compatible),
        with_compatible_common_Han_spelling=len(common_han), readings_with_kana_usually_flag=len(kana_usually),
        limitations='Exact NFC POJ join measures availability, not editorial/common-glyph approval or sense alignment. '
                    'Japanese counts precede ASCII alias filters. This audit cannot establish community agreement.')

if __name__ == '__main__':
    print(json.dumps(audit(), ensure_ascii=False, indent=2))
