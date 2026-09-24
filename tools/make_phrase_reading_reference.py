"""Freeze the declared diagnostic strata before new phone observations."""
import collections
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'artifacts/phrase-reading-prior'


def main():
    paths = [ROOT / 'artifacts/moe-reading-reference/before.tsv', OUT / 'modern.tsv',
             ROOT / 'artifacts/moe-reading-reference/references.json',
             ROOT / 'artifacts/reading-prior-followup/reference-plan.json']
    def table(path):
        with path.open(encoding='utf-8') as stream:
            return {r['raw']: r for r in csv.DictReader(stream, delimiter='\t')}
    before, after = map(table, paths[:2])
    refs = json.loads(paths[2].read_text(encoding='utf-8'))
    used = {r['source']['raw'] for r in json.loads(paths[3].read_text(encoding='utf-8'))}
    groups = collections.defaultdict(list)
    for raw, expected in refs.items():
        old, new = before[raw]['space'], after[raw]['space']
        if old in expected and new not in expected:
            groups['complete-reading-loss'].append(raw)
        elif raw not in used:
            if old not in expected and new in expected:
                groups['complete-reading-gain'].append(raw)
            elif old != new and old in expected and new in expected:
                groups['both-supported-changed'].append(raw)
            elif old == new:
                groups['unchanged-control'].append(raw)
    seed = 'phrase-reading-reference-20260924'
    selected = []
    for name in ('complete-reading-loss', 'complete-reading-gain', 'both-supported-changed', 'unchanged-control'):
        values = sorted(groups[name], key=lambda q: hashlib.sha256((seed + '|' + q).encode()).digest())
        selected.extend((name, q) for q in (values if name == 'complete-reading-loss' else values[:8]))
    plan = [dict(id=f'phrase-reading-{i:02d}', mode='pinyin', source=dict(stratum=name, raw=raw),
                 actions=[dict(type=raw, label='typed', capture=True), dict(key='SPACE', label='accepted', capture=True)])
            for i, (name, raw) in enumerate(selected)]
    data = (json.dumps(plan, ensure_ascii=False, indent=2) + '\n').encode()
    (OUT / 'reference-plan.json').write_bytes(data)
    manifest = dict(seed=seed, cases=len(plan), eligible={k: len(v) for k, v in groups.items()},
                    selected=dict(collections.Counter(k for k, _ in selected)),
                    plan_sha256=hashlib.sha256(data).hexdigest(),
                    inputs={p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths})
    (OUT / 'reference-manifest.json').write_bytes((json.dumps(manifest, indent=2) + '\n').encode())
    print(json.dumps(manifest, indent=2))


if __name__ == '__main__':
    main()
