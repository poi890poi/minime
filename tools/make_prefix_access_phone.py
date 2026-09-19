"""Select one reproducible recovered development case per input condition."""
import csv
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
before_path = root / 'artifacts/prefix-access/development-before.tsv'
after_path = root / 'artifacts/prefix-access/development-retention.tsv'

def load(path):
    with path.open(encoding='utf-8') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))

before, after = load(before_path), load(after_path)
assert len(before) == len(after) == 21000
recovered = []
for old, new in zip(before, after):
    assert (old['raw'], old['target'], old['condition']) == (new['raw'], new['target'], new['condition'])
    assert new['role'] == 'development'
    if int(old['composition_rank']) == 0 and int(new['composition_rank']) > 0:
        recovered.append(new)

selected = []
for condition in sorted({row['condition'] for row in recovered}):
    row = min((row for row in recovered if row['condition'] == condition),
              key=lambda row: hashlib.sha256(('prefix-phone-20260919\t' + row['raw'] + '\t' + row['target']).encode()).digest())
    selected.append(dict(raw=row['raw'], text=row['target'], remaining=row['suffix'], condition=condition))
assert len(selected) > 1
target = root / 'docs/prefix-access/evidence/prefix-retention-cases.json'
target.write_bytes((json.dumps(selected, ensure_ascii=False, indent=2) + '\n').encode())
manifest = dict(selection='one minimum SHA-256 per recovered development input condition; fixed seed prefix-phone-20260919',
                role='mechanical Android integration, not natural conversation accuracy',
                inputs={str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest() for path in (before_path, after_path)},
                fixture_sha256=hashlib.sha256(target.read_bytes()).hexdigest(), cases=len(selected), recovered=len(recovered))
(root / 'docs/prefix-access/phone-plan.json').write_bytes((json.dumps(manifest, indent=2) + '\n').encode())
print(json.dumps(manifest))
