"""Freeze prose reconstruction inputs without observing decoder output."""
from pathlib import Path
from collections import defaultdict,Counter
from functools import lru_cache
import re,json,gzip,hashlib
ROOT=Path(__file__).resolve().parent.parent;OUT=ROOT/'docs/construction-confidence'
seed='construction-confidence-20260912-v1'
order=lambda s:hashlib.sha256((seed+':'+s).encode()).digest()
syllables={p[1]:p[0] for row in (ROOT/'app/src/main/assets/syllables.tsv').read_text(encoding='utf-8').splitlines() for p in [row.split('\t')]}
readings=defaultdict(set)
for name in ['BPMFBase.txt','BPMFMappings.txt']:
    for line in (ROOT/'third_party/mcbopomofo'/name).read_text(encoding='utf-8').splitlines():
        p=line.split()
        if len(p)<2 or not re.fullmatch('[\u3400-\u9fff]+',p[0]):continue
        units=[syllables.get(re.sub('[ˊˇˋ˙ˉ]','',b)) for b in (p[1:2] if name=='BPMFBase.txt' else p[1:])]
        if all(units) and len(units)==len(p[0]):readings[p[0]].add(tuple(units))
@lru_cache(None)
def reading(text):
    # Prefer a source-attested complete reading; otherwise require a unique
    # reading across all available segmentations. Reject rather than guess.
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
existing=json.loads(gzip.decompress((ROOT/'docs/sources/audit-2026-09-08/sample-records.json.gz').read_bytes()))
existing=[r for r in existing if r['source']=='taiwan-md' and r['path'].startswith('knowledge/') and r['path'].split('/')[1] not in ['About','resources']]
development=sorted(existing,key=lambda r:order(r['path']))[:40]
reserved=json.loads(gzip.decompress((OUT/'reserved-articles.json.gz').read_bytes()))
rows=[];excluded=Counter();docs={}
for role,articles in [('development',development),('reserved',reserved)]:
    docs[role]=[]
    for a in articles:
        docs[role].append({'path':a['path'],'url':a['url'],'text_sha256':hashlib.sha256(a['text'].encode()).hexdigest()})
        prose=re.sub(r'^---.*?---\s*','',a['text'],flags=re.S)
        prose='\n'.join(l for l in prose.splitlines() if l.strip() and not re.match(r'^\s*(?:[#>|*`-]|\d+\.)',l))
        prose=re.sub(r'\[([^\]]+)\]\([^)]*\)',r'\1',prose)
        spans=[]
        for i,m in enumerate(re.finditer('[\u3400-\u9fff]+',prose)):
            target=m.group();identity=a['path']+':'+str(i)
            if not 3<=len(target)<=16:excluded[role+'/outside-length']+=1;continue
            units=reading(target)
            if not units:excluded[role+'/unreadable-or-ambiguous']+=1;continue
            if len(''.join(units))>96:excluded[role+'/outside-input-limit']+=1;continue
            spans.append((identity,target,units))
        for identity,target,units in sorted(spans,key=lambda x:order(x[0]))[:32]:
            for condition,raw in [('full',''.join(units)),('partial',''.join(units[:-1])),('initials',''.join(u[0] for u in units)),('mixed',''.join(u if i%2 else u[0] for i,u in enumerate(units)))]:
                rows.append(dict(role=role,genre='taiwan-md-prose',document=a['path'],id=identity,condition=condition,raw=raw,target=target,syllables=units))
payload=(json.dumps(rows,ensure_ascii=False,separators=(',',':'))+'\n').encode()
with (OUT/'inputs.json.gz').open('wb') as f:
    with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as z:z.write(payload)
manifest={'seed':seed,'rows':len(rows),'groups':dict(Counter(r['role']+'/'+r['condition'] for r in rows)),'documents':docs,'exclusions':dict(excluded),'uncompressed_sha256':hashlib.sha256(payload).hexdigest(),
          'annotation':'source Han reference; original McBopomofo unique reading; no Rime-generated labels; shared lexical lineage and ambiguity exclusions limit generalization',
          'genres_missing':['natural Taiwan Mandarin conversation'],'rules':'3–16 Han contiguous prose spans, at most 32 hash-selected spans per preselected document; full, omit-last-syllable, initials, alternating-full-and-initial input'}
(OUT/'corpus-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in manifest.items() if k!='documents'},ensure_ascii=False,indent=2))
