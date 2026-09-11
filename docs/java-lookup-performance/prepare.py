"""Freeze consumed inputs without selecting for speed or reference agreement."""
import csv, gzip, hashlib, json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/java-lookup-performance'
sources = ['docs/language-contract-benchmark/inputs.tsv',
           'docs/language-contract-benchmark/conversations/inputs.tsv']
groups = {}
for source in sources:
    for i, row in enumerate(csv.reader((ROOT/source).open(encoding='utf-8'), delimiter='\t')):
        group, doc, item, condition, raw = row[:5]
        pack = 'japanese' if group.startswith('japanese') else 'poj' if group.startswith(('poj','taiwanese')) else 'taiwan' if group.startswith(('zh','chinese')) else None
        if pack is None: continue
        record = [pack, group, condition, f'{source}:{i+1}', raw]
        key = (group, condition)
        digest = hashlib.sha256(('\t'.join(record)).encode()).hexdigest()
        groups.setdefault(key, []).append((digest, record))
rows = [record for key in sorted(groups) for _,record in sorted(groups[key])[:256]]
phone = 'docs/japanese-provider-integration/stream.jsonl.gz'
with gzip.open(ROOT/phone, 'rt', encoding='utf-8') as f:
    for i,line in enumerate(f):
        row=json.loads(line)
        rows.append(['japanese','phone-replay','typing',f'{phone}:{i+1}',row['raw']])
with (OUT/'inputs.tsv').open('w', encoding='utf-8', newline='') as f:
    csv.writer(f, delimiter='\t', lineterminator='\n').writerows(rows)
with (OUT/'phone-inputs.tsv').open('w', encoding='utf-8', newline='') as f:
    csv.writer(f, delimiter='\t', lineterminator='\n').writerows(rows[::16])
manifest={'baseline':'dce6ddb','role':'consumed regression, not accuracy holdout',
 'selection':'SHA-256 ordering, at most 256 per source group/condition; all prior phone keys',
 'rows':len(rows),'sources':{p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in sources+[phone]},
 'inputs_sha256':hashlib.sha256((OUT/'inputs.tsv').read_bytes()).hexdigest()}
manifest['phone']={'selection':'Every sixteenth frozen row, beginning at row zero',
 'rows':len(rows[::16]),'sha256':hashlib.sha256((OUT/'phone-inputs.tsv').read_bytes()).hexdigest()}
manifest['assets']={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
 for p in sorted((ROOT/'app/build/generated/minimeAssets').glob('addon-*.bin'))}
p=ROOT/'app/build/generated/minimeAssets/japanese-basic.tsv'
manifest['assets'][p.relative_to(ROOT).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
print(json.dumps(manifest))
