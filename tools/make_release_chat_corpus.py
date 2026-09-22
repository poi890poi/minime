"""Freeze chat-derived evaluation without consulting runtime candidate output."""
from pathlib import Path
from collections import defaultdict,Counter
from functools import lru_cache
import re,json,gzip,hashlib
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'docs/release-hardening'
seed='release-hardening-20260922-v1'
order=lambda s:hashlib.sha256((seed+':'+s).encode()).digest()
source=OUT/'source/g0v_slack_rand0m.txt'
readings=defaultdict(set)
for line in (ROOT/'app/src/main/assets/zh_tw.tsv').read_text(encoding='utf-8').splitlines():
    p=line.split('\t');units=tuple(p[0].split("'"))
    if len(units)==len(p[2]):readings[p[2]].add(units)
@lru_cache(None)
def reading(text):
    exact=readings.get(text)
    if exact:return next(iter(exact)) if len(exact)==1 else None
    ways=[set() for _ in range(len(text)+1)];ways[0].add(())
    for end in range(1,len(text)+1):
        for start in range(max(0,end-12),end):
            for prefix in ways[start]:
                for units in readings.get(text[start:end],()):
                    ways[end].add(prefix+units)
                    if len(ways[end])>2:break
                if len(ways[end])>2:break
            if len(ways[end])>2:break
    return next(iter(ways[-1])) if len(ways[-1])==1 else None
eligible=[];excluded=Counter();unique=set()
lines=source.read_text(encoding='utf-8').splitlines()
for number,line in enumerate(lines,1):
    text=line.strip()
    if text in unique:excluded['duplicate']+=1;continue
    unique.add(text)
    if not re.fullmatch('[\u3400-\u9fff]{3,20}',text):excluded['not_3_to_20_Han_only']+=1;continue
    units=reading(text)
    if units is None:excluded['ambiguous_or_unavailable_reading']+=1;continue
    eligible.append((number,text,units))
selected=sorted(eligible,key=lambda r:order(r[1]))[:512]
rows=[];references=[]
for number,text,units in selected:
    spans=[(0,len(text),'chat-excerpt')]
    small=[(start,end,'chat-short-span') for start in range(len(text)) for end in range(start+2,min(len(text),start+6)+1) if end-start<len(text)]
    spans+=sorted(small,key=lambda r:order(text+':'+str(r[:2])))[:2]
    for start,end,genre in spans:
        target=text[start:end];part=units[start:end];identity=f'g0v-file-line-{number}/{start}:{end}'
        references.append(dict(id=identity,line=number,text=text,start=start,end=end,target=target,reading=part,source_exact=target in readings))
        for condition,raw in [('full',''.join(part)),('initials',''.join(u[0] for u in part)),('mixed',''.join(u if i%2==0 else u[0] for i,u in enumerate(part)))]:
            rows.append([genre,identity,condition,raw,target,str(len(target))])
payload=('genre\tid\tcondition\traw\ttarget\tglyphs\n'+'\n'.join('\t'.join(r) for r in rows)+'\n').encode()
(OUT/'chat-inputs.tsv.gz').write_bytes(gzip.compress(payload,mtime=0))
(OUT/'chat-references.json.gz').write_bytes(gzip.compress(json.dumps(references,ensure_ascii=False).encode(),mtime=0))
manifest=dict(baseline='fd728d5',seed=seed,source_lines=len(lines),eligible_lines=len(eligible),selected_lines=len(selected),exclusions=dict(excluded),rows=len(rows),groups=dict(Counter(r[0]+'/'+r[2] for r in rows)),source_exact_references=sum(r['source_exact'] for r in references),references=len(references),source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),reading_source_sha256=hashlib.sha256((ROOT/'app/src/main/assets/zh_tw.tsv').read_bytes()).hexdigest(),input_sha256=hashlib.sha256(payload).hexdigest(),plan_sha256=hashlib.sha256((OUT/'PLAN.md').read_bytes()).hexdigest(),scope='One evaluation-only edited chat source document. Lost conversation boundaries; shared reading lineage; no natural-conversation accuracy or fresh holdout claim.')
(OUT/'chat-manifest.json').write_bytes((json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode())
print(json.dumps(manifest,ensure_ascii=False,indent=2))
