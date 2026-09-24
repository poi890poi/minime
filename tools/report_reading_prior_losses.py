"""Diagnose the frozen, rejected source-reading-prior trial; never writes assets."""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('trial', type=Path)
    parser.add_argument('inventories', type=Path)
    parser.add_argument('engine_summary', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--unbounded-source', type=Path)
    args = parser.parse_args()
    source = ROOT / 'app/src/main/assets/zh_tw.tsv'
    manifest = json.loads((ROOT / 'docs/chinese-recovery/weights-manifest.json').read_text())
    assert digest(source) == manifest['source_sha256'], 'Changed production source'
    assert digest(args.trial) == manifest['variants']['reading-priors'], 'Unpinned trial bytes'

    def counts(path):
        with path.open(encoding='utf-8') as stream:
            return {tuple(p[:3]): float(p[3]) for p in csv.reader(stream, delimiter='\t')}

    old, new = counts(source), counts(args.trial)
    assert old.keys() == new.keys(), 'Changed source reading identities'
    penalized = {(k[0], k[2]) for k in old if new[k] < old[k]}
    with args.inventories.open(encoding='utf-8') as stream:
        rows = list(csv.DictReader(stream, delimiter='\t'))
    lost = [r for r in rows if int(r['before_rank']) > 0 and int(r['after_rank']) == 0]
    gained = [r for r in rows if int(r['before_rank']) == 0 and int(r['after_rank']) > 0]
    report = json.loads(args.engine_summary.read_text(encoding='utf-8-sig'))
    assert len(lost) == report['lost_query_identities']
    overrides = {}
    if args.unbounded_source:
        original = (ROOT / 'core/src/main/java/dev/minime/core/ReadingUnitIndex.java').read_text(encoding='utf-8')
        assert args.unbounded_source.read_text(encoding='utf-8') == original.replace('CANDIDATES=128', 'CANDIDATES=Integer.MAX_VALUE')
        overrides['ReadingUnitIndex'] = digest(args.unbounded_source)
    report.update(
        scope='Coverage diagnosis of rejected reading-prior trial; not semantic precision or production admission',
        source_rows=len(old),
        source_rows_with_reduced_weight=sum(new[k] < old[k] for k in old),
        source_rows_with_increased_weight=sum(new[k] > old[k] for k in old),
        gained_query_identities=len(gained),
        lost_by_input_length=dict(collections.Counter(str(len(r['query'])) for r in lost)),
        lost_by_kind=dict(collections.Counter(r['kind'] for r in lost)),
        lost_with_unchanged_all_complete_reading_access=sum(r['complete_before'] == r['complete_after'] for r in lost),
        lost_with_matching_source_penalty=sum(any(text == r['text'] and reading.replace("'", '').startswith(r['query'])
                                                for reading, text in penalized) for r in lost),
        gained_by_kind=dict(collections.Counter(r['kind'] for r in gained)),
        runtime_sources={name: digest(ROOT / 'core/src/main/java/dev/minime/core' / (name + '.java'))
                         for name in ('PhoneticDictionary', 'ReadingUnitIndex', 'ReadingIndex', 'FirstGlyphIndex')},
        isolated_runtime_overrides=overrides,
        hashes={str(p.resolve().relative_to(ROOT)).replace('\\', '/'): digest(p) for p in (
            source, args.trial, ROOT / 'docs/glyph-ranking/inputs.txt', args.inventories,
            ROOT / 'core/src/test/java/dev/minime/core/ReadingPriorAudit.java')})
    args.output.write_bytes((json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
