"""Targeted diagnostic stratum: hash-selected English/Chinese collisions."""
import hashlib,json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
source=root/'docs/candidate-usefulness/mixed-english-before.json'
affected=json.loads(source.read_text(encoding='utf-8'))['affected']
seed='english-interference-20260919'
# Selection conditions on the known MinIME failure stratum, never Google outputs.
# Report only this diagnostic stratum, not relative overall English frequency.
affected.sort(key=lambda r:hashlib.sha256((seed+'|'+r['genre']+'|'+r['id']+'|'+r['raw']).encode()).digest())
selected=[];seen=set()
for row in affected:
    if row['raw'] in seen:continue
    seen.add(row['raw']);selected.append({k:row[k] for k in ('genre','id','condition','raw','target')})
    if len(selected)==16:break
out=root/'docs/candidate-usefulness/english-phone-plans';out.mkdir(exist_ok=True)
plans=[dict(id=f'english-interference-{i:02}',mode='pinyin',source=row,actions=[
    dict(type=row['raw'],label='typed',capture=True),dict(key='EXPAND',label='expanded',capture=True),
    dict(key='COLLAPSE',label='collapsed',capture=False),dict(key='SPACE',label='accepted',capture=False)]) for i,row in enumerate(selected)]
for i in range(0,len(plans),8):
    (out/f'batch-{i//8+1:02}.json').write_text(json.dumps(plans[i:i+8],ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(out/'manifest.json').write_text(json.dumps(dict(seed=seed,cases=len(plans),
    scope='Known MinIME English-first diagnostic stratum; reused Han-target corpus; no population frequency claim; selected before observing Google.',
    source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
    files={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(out.glob('batch-*.json'))}),indent=2)+'\n',encoding='utf-8')
