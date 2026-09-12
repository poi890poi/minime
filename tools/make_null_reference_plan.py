"""Hash-selected reference observation sample, before result inspection."""
from pathlib import Path
import hashlib,json,gzip,re
R=Path(__file__).resolve().parent.parent;O=R/'docs/null-construction'
def order(r):return hashlib.sha256(('null-google-v1\0'+json.dumps(r,ensure_ascii=False,sort_keys=True)).encode()).hexdigest()
inputs=json.loads(gzip.decompress((R/'docs/construction-confidence/inputs.json.gz').read_bytes()))
selected=[]
for condition in ['full','initials','mixed','partial']:
    rows=[r for r in inputs if r['role']=='development' and r['condition']==condition]
    selected += [dict(r,genre='zh-prose') for r in sorted(rows,key=order)[:3]]
for genre in ['en-gum-conversation','en-gum-essay']:
    rows=[]
    for line in (R/'docs/conversation-ranking/corpus/gum-test.tsv').read_text(encoding='utf-8').splitlines():
        g,identity,raw,target=line.split('\t')
        if g!=genre:continue
        for at,word in enumerate(raw.split()):
            if re.fullmatch('[a-z]{2,16}',word):rows.append(dict(genre=g,document=identity,position=at,raw=word,target=word,condition='full'))
    selected+=sorted(rows,key=order)[:3]
plan=[]
for i,row in enumerate(selected):
    identity='null-ref-'+str(i);row['id']=identity
    plan.append(dict(id=identity,mode='pinyin',field='private',actions=[dict(type=row['raw'],label='typed',capture=True),dict(key='SPACE',label='space')]))
(O/'reference-sample.json').write_text(json.dumps(selected,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(O/'reference-plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
# Keep the original timed run reproducible; delayed follow-up uses every same case.
for case in plan:case['actions'].insert(1,dict(waitMs=1000,label='settled',capture=True))
(O/'reference-delayed-plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Frozen',len(plan),'Chinese/English reference cases; same Pinyin board, private editor, centered touches.')
