"""Audit pronunciation ambiguity in frozen evaluation inputs; never relabel them."""
import collections
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main():
    source = ROOT / 'app/src/main/assets/zh_tw.tsv'
    inputs = ROOT / 'docs/conversation-ranking/corpus/inputs.tsv'
    readings = collections.defaultdict(set)
    frequencies = collections.defaultdict(set)
    first = {}
    for row in csv.reader(source.open(encoding='utf-8'), delimiter='\t'):
        reading, _, word, score, _ = row
        readings[word].add(reading)
        frequencies[word].add(float(score))
        if word not in first or first[word][0] < float(score):
            first[word] = (float(score), reading)
    groups = collections.defaultdict(collections.Counter)
    for row in csv.reader(inputs.open(encoding='utf-8'), delimiter='\t'):
        if not row[0].startswith('zh-') or not row[0].endswith('-full'):
            continue
        group, _, raw, word = row
        assert raw == first[word][1].replace("'", ''), 'Generator reconstruction mismatch'
        assert len(frequencies[word]) == 1, 'Frequency-tie assumption does not hold'
        for key in ('all', group, 'single-glyph' if len(word) == 1 else 'multi-glyph'):
            groups[key]['full_spelling_word_cases'] += 1
            groups[key]['multiple_distinct_source_pinyin_readings'] += len(readings[word]) > 1
            groups[key]['source_file_order_resolves_equal_frequency'] += len(readings[word]) > 1
    priority = {}
    for path in sorted((ROOT / 'artifacts/mcbopomofo-audit/upstream').glob('heterophony*.list')):
        records = collections.defaultdict(list)
        for row in path.read_text(encoding='utf-8').splitlines():
            cells = row.split()
            if len(cells) >= 2:
                records[cells[0]].append(cells[1])
        multiple = [values for values in records.values() if len(set(values)) > 1]
        priority[path.name] = dict(glyphs=len(records), glyphs_with_multiple_priority_readings=len(multiple),
                                   readings_overwritten_by_single_value_map=sum(len(set(v)) - 1 for v in multiple),
                                   sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    report = dict(scope='Evaluation annotation and source-parser audit only; ambiguity is not proof that a chosen reading is wrong.',
                  groups={k: dict(v) for k, v in groups.items()}, upstream_priority_lists=priority,
                  warning='Do not relabel this evaluated corpus with the trial source priorities and call the resulting score independent.',
                  inputs={str(p.relative_to(ROOT)).replace('\\', '/'): hashlib.sha256(p.read_bytes()).hexdigest()
                          for p in (source, inputs, ROOT / 'tools/make_conversation_corpus.py')})
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
