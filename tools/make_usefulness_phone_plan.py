"""Freeze paired visible-candidate observations, without selecting on model output."""
import argparse, csv, gzip, hashlib, io, json
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--collapse',action='store_true');args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
out=root/('docs/candidate-usefulness/phone-plans-corrected' if args.collapse else 'docs/candidate-usefulness/phone-plans');out.mkdir(parents=True,exist_ok=True)
original=root/'docs/chinese-recovery/phone-plans'
selected=[c['source'] for i in (1,2) for c in json.loads((original/f'batch-{i:02}.json').read_text(encoding='utf-8'))]
used={r['raw'] for r in selected}
source=root/'docs/chinese-recovery/corpus.tsv.gz'
rows=list(csv.DictReader(io.StringIO(gzip.decompress(source.read_bytes()).decode()),delimiter='\t'))
remaining=[r for r in rows if r['genre']=='authored-conversation-regression' and r['raw'] not in used]
remaining.sort(key=lambda r:hashlib.sha256(('candidate-usefulness-20260919/'+r['id']+'/'+r['raw']).encode()).digest())
selected+=remaining[:6]
assert len(selected)==24
plans=[dict(id=f'useful-{i:02}',mode='pinyin',source=row,actions=[
    dict(type=row['raw'],label='typed',capture=True),
    dict(key='EXPAND',label='expanded',capture=True),
    dict(key='SPACE',label='accepted',capture=False)]) for i,row in enumerate(selected)]
if args.collapse:
    for case in plans:case['actions'].insert(2,dict(key='COLLAPSE',label='collapsed',capture=False))
for i in range(0,24,12):
    (out/f'batch-{i//12+1:02}.json').write_bytes((json.dumps(plans[i:i+12],ensure_ascii=False,indent=2)+'\n').encode())
manifest=dict(cases=24,conversation=12,essay=12,seed='candidate-usefulness-20260919',
    action_repair='Collapse expanded candidates before Space; Google has no Space in expanded view' if args.collapse else None,
    role='Previously exposed authored/essay regression, paired experience audit, not fresh natural-language accuracy',
    corpus_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
    files={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(out.glob('batch-*.json'))})
(out/'manifest.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
print(json.dumps(manifest))
