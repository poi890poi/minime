"""Freeze stratified source-reading reference probes before phone observation."""
import collections
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'artifacts/reading-prior-followup'
INPUT = OUT / 'inventories.tsv'
QUERIES = ROOT / 'docs/glyph-ranking/inputs.txt'
SYLLABLES = ROOT / 'app/src/main/assets/syllables.tsv'


def main():
    frozen = json.loads((ROOT / 'docs/chinese-recovery/reading-prior-summary.json').read_text(encoding='utf-8'))
    for path in (INPUT, QUERIES):
        key = str(path.relative_to(ROOT)).replace('\\', '/')
        assert hashlib.sha256(path.read_bytes()).hexdigest() == frozen['hashes'][key], 'Changed diagnostic input'
    syllables = {line.split('\t')[0] for line in SYLLABLES.read_text(encoding='utf-8').splitlines()}
    queries = set(QUERIES.read_text().splitlines())
    changes = collections.defaultdict(list)
    with INPUT.open(encoding='utf-8') as stream:
        for row in csv.DictReader(stream, delimiter='\t'):
            changes[row['query']].append(row)
    groups = {
        'changed-complete-top': [q for q in queries & syllables if any(int(r['before_rank']) == 1 for r in changes[q])],
        'changed-partial-first8': [q for q in queries - syllables if any(0 < int(r['before_rank']) <= 8 or 0 < int(r['after_rank']) <= 8 for r in changes[q])],
        'unchanged-complete-control': [q for q in queries & syllables if not changes[q]],
    }
    seed = 'reading-reference-20260924'
    selected = [(stratum, q) for stratum, values in groups.items()
                for q in sorted(values, key=lambda s: hashlib.sha256((seed + '|' + s).encode()).digest())[:8]]
    plan = [dict(id='reading-%02d' % i, mode='pinyin', source=dict(stratum=stratum, raw=raw),
                 actions=[dict(type=raw, label='typed', capture=True), dict(key='SPACE', label='accepted', capture=True)])
            for i, (stratum, raw) in enumerate(selected)]
    data = (json.dumps(plan, ensure_ascii=False, indent=2) + '\n').encode('utf-8')
    (OUT / 'reference-plan.json').write_bytes(data)
    manifest = dict(seed=seed, eligible={k: len(v) for k, v in groups.items()},
                    selected=dict(collections.Counter(k for k, _ in selected)), cases=len(plan),
                    plan_sha256=hashlib.sha256(data).hexdigest(),
                    inputs={str(p.relative_to(ROOT)).replace('\\', '/'): hashlib.sha256(p.read_bytes()).hexdigest()
                            for p in (INPUT, QUERIES, SYLLABLES)})
    (OUT / 'reference-manifest.json').write_bytes((json.dumps(manifest, indent=2) + '\n').encode())
    print(json.dumps(manifest, indent=2))


if __name__ == '__main__':
    main()
