"""Compare the frozen Japanese kana retrieval audit without changing target labels."""
import csv, gzip, hashlib, io, json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
folder = ROOT/'docs/japanese-output'
before_path = ROOT/'docs/two-language-modes/japanese-final.tsv.gz'
after_path = folder/'retrieval.tsv.gz'
with gzip.open(before_path, 'rt', encoding='utf-8') as f:
    before = list(csv.DictReader(f, delimiter='\t'))
with gzip.open(after_path, 'rt', encoding='utf-8') as f:
    after = list(csv.DictReader(f, delimiter='\t'))
assert len(before) == len(after)
keys = ('condition', 'id', 'reading', 'input', 'target')
assert all(all(a[k] == b[k] for k in keys) for a,b in zip(before,after)), 'Audit inputs differ'
report = {}
for condition in sorted({row['condition'] for row in after}):
    report[condition] = {}
    for label, rows in [('before', before), ('after', after)]:
        ranks = [int(row['rank']) for row in rows if row['condition'] == condition]
        report[condition][label] = dict(count=len(ranks), available=sum(r>0 for r in ranks), top8=sum(0<r<=8 for r in ranks))
report['lost_available'] = sum(int(a['rank'])>0 and int(b['rank'])==0 for a,b in zip(before,after))
report['gained_available'] = sum(int(a['rank'])==0 and int(b['rank'])>0 for a,b in zip(before,after))
report['evidence_sha256'] = {p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
                             for p in (before_path, after_path, ROOT/'docs/japanese-coverage/inputs.tsv')}
(folder/'retrieval.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
