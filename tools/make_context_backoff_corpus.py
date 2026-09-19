"""Freeze conditional ranking probes; never changes production language data."""
import collections,gzip,hashlib,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'docs/context-backoff';OUT.mkdir(exist_ok=True)
pins={}
def read(name):
    data=(ROOT/name).read_bytes();pins[name]=hashlib.sha256(data).hexdigest();return data
readings=collections.defaultdict(set)
read('app/src/main/assets/context.tsv')  # Frozen runtime counts; not used to select probes.
for line in read('app/src/main/assets/zh_tw.tsv').decode().splitlines():
    p=line.split('\t');readings[p[2]].add(p[0])
rows=[]
def add(genre,identity,context,target,reading):
    units=reading.split("'")
    for condition,raw in [('full',''.join(units)),('initials',''.join(u[0] for u in units)),
                          ('mixed',''.join(u if i%2==0 else u[0] for i,u in enumerate(units)))]:
        rows.append([genre,identity,condition,context[-3:],raw,target])
for name in ['docs/pinyin-boundary-holdout.tsv','docs/gap-implementation/pinyin-validation.tsv']:
    for line in read(name).decode().splitlines():
        if not line or line.startswith('#'):continue
        identity,context,raw,target=line.split('\t')
        compatible=sorted(r for r in readings[target] if r.replace("'",'')==raw)
        if compatible:add('encyclopedic-regression',name+':'+identity,context,target,compatible[0])
texts={}
for line in read('docs/pinyin-fresh-holdout.tsv').decode().splitlines():
    p=line.split('\t');texts[p[3]]=p[3]
essays=json.loads(gzip.decompress(read('docs/construction-confidence/inputs.json.gz')))
documents=sorted({r['document'] for r in essays},key=lambda x:hashlib.sha256(('context-backoff-20260919/'+x).encode()).digest())[:64]
selected=set(documents)
families=collections.defaultdict(set)
for target in texts:families['authored-conversation-regression'].add(('scenario:'+hashlib.sha256(target.encode()).hexdigest()[:12],target))
for item in essays:
    if item['document'] in selected:families['essay-regression'].add((item['id'],item['target']))
missing=collections.Counter()
for genre,values in sorted(families.items()):
    for identity,text in sorted(values):
        for start in range(1,len(text)):
            for end in range(start+1,min(len(text),start+6)+1):
                target=text[start:end]
                if not re.fullmatch('[\u3400-\u9fff]+',target):continue
                if target not in readings:missing[genre]+=1;continue
                # Deterministic alias selection, not frequency or reference-output tuning.
                add(genre,f'{identity}/{start}:{end}',text[:start],target,sorted(readings[target])[0])
payload='genre\tid\tcondition\tcontext\traw\ttarget\n'+'\n'.join('\t'.join(r) for r in rows)+'\n'
data=payload.encode();(OUT/'inputs.tsv.gz').write_bytes(gzip.compress(data,mtime=0))
manifest={'baseline':'5b704a4','scope':'Previously evaluated sources; dictionary-covered conditional ranking; no fresh holdout.',
          'source_sha256':pins,'input_sha256':hashlib.sha256(data).hexdigest(),'plan_sha256':hashlib.sha256((OUT/'PLAN.md').read_bytes()).hexdigest(),
          'counts':dict(collections.Counter(r[0]+'/'+r[2] for r in rows)), 'missing_substrings':dict(missing),
          'conversation_scenarios':len(texts),'essay_documents':documents}
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'rows':len(rows),'groups':manifest['counts'],'conversation_scenarios':len(texts)}))
