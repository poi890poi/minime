"""Freeze hash-selected existing regression inputs, without looking at outputs."""
from pathlib import Path
import hashlib,json,re
root=Path(__file__).resolve().parent.parent
out=root/'docs/suggestion-latency';out.mkdir(exist_ok=True)
sources=['docs/conversation-ranking/corpus/inputs.tsv','docs/taiwan-quality/entity-lookup-inputs.tsv']
groups={}
for name in sources:
    for line in (root/name).read_text(encoding='utf8').splitlines():
        p=line.split('\t')
        if len(p)<4:continue
        group=p[0] if 'conversation-ranking' in name else p[0]+'-'+p[1]
        for raw in p[2].split():
            if re.fullmatch("[a-zv]+(?:'[a-zv]+)*",raw) and len(raw)<=48:groups.setdefault(group,set()).add(raw)
rows=[]
for group,values in sorted(groups.items()):
    selected=sorted(values,key=lambda r:hashlib.sha256(('latency-20260909:'+group+':'+r).encode()).digest())[:16]
    for raw in selected:
        for condition,value in [('full',raw),('typing-half',raw[:max(1,len(raw)//2)].rstrip("'")),('typing-first',raw[:1])]:
            rows.append((group,condition,value))
payload=''.join('\t'.join(r)+'\n' for r in rows).encode('utf8')
(out/'inputs.tsv').write_bytes(payload)
(out/'corpus.json').write_text(json.dumps(dict(seed='latency-20260909',rows=len(rows),sources={p:hashlib.sha256((root/p).read_bytes()).hexdigest() for p in sources},sha256=hashlib.sha256(payload).hexdigest(),scope='Hash-selected existing regression queries, with deterministic typing prefixes; not a fresh language-quality holdout. Genres and source packs remain separate. No complaint words or outputs selected the inputs.'),indent=2)+'\n',encoding='utf8')
print(len(rows),'frozen queries')
# Device uses the same input bytes and selects one triple per source/mode group.
(root/'app/src/androidTest/assets/latency-inputs.tsv').write_bytes(payload)
native={}
for line in (root/sources[0]).read_text(encoding='utf8').splitlines():
    native.setdefault(line.split('\t')[0],[]).append(line)
selected=[line for group in sorted(native) for line in sorted(native[group],key=lambda s:hashlib.sha256(('latency-native-20260909:'+s).encode()).digest())[:32]]
(out/'native-inputs.tsv').write_text('\n'.join(selected)+'\n',encoding='utf8')
