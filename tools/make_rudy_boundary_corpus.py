"""Freeze source-supported readings independently of boundary-recovery logic."""
from pathlib import Path
import collections,gzip,hashlib,json,re
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'docs/rudy-reading-boundaries'
source=ROOT/'third_party/rudy/extracted-names.json.gz';base=ROOT/'app/src/main/assets/zh_tw.tsv'
names=json.loads(gzip.decompress(source.read_bytes()))['names'];readings=collections.defaultdict(set)
for line in base.read_text(encoding='utf-8').splitlines():
    p=line.split('\t');readings[p[2]].add(p[0])
def derive(word):
    if word in readings:return sorted(readings[word])
    result=[];at=0
    while at<len(word):
        for end in range(len(word),at,-1):
            known=readings.get(word[at:end],set())
            if len(known)==1:result.append(next(iter(known)));at=end;break
        else:return []
    return ["'".join(result)]
rows=set();supported=set();all_tags=0
for word,data in sorted(names.items()):
    for tag in data['pinyin']:
        all_tags+=1;flat=re.sub(r"[ '\-]+",'',tag)
        # Full source tags are tested even without an independent boundary reference.
        rows.add(('all-upstream','full',tag,word,tag))
        for reading in derive(word):
            if reading.replace("'",'')!=flat:continue
            supported.add((word,tag));units=reading.split("'")
            for condition,raw in [('full',flat),('initials',''.join(u[0] for u in units)),
                                  ('mixed',''.join(u if i%2==0 else u[0] for i,u in enumerate(units))),
                                  ('reverse-mixed',''.join(u[0] if i%2==0 else u for i,u in enumerate(units))),
                                  ('separated',reading)]:rows.add(('source-supported',condition,raw,word,reading))
data=('group\tcondition\traw\ttarget\treading\n'+'\n'.join('\t'.join(r) for r in sorted(rows))+'\n').encode()
(OUT/'inputs.tsv.gz').write_bytes(gzip.compress(data,mtime=0))
(OUT/'input-manifest.json').write_text(json.dumps({'baseline':'7efab7f','tags':all_tags,'independently_supported_tags':len(supported),'rows':len(rows),
    'counts':dict(collections.Counter(r[0]+'/'+r[1] for r in rows)),
    'scope':'Known-source retrieval contracts, not language accuracy; expected boundaries from licensed word readings or unambiguous units, not the new segmentation helper.',
    'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'reading_source_sha256':hashlib.sha256(base.read_bytes()).hexdigest(),
    'corpus_sha256':hashlib.sha256(data).hexdigest(),'plan_sha256':hashlib.sha256((OUT/'PLAN.md').read_bytes()).hexdigest()},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'tags':all_tags,'supported':len(supported),'rows':len(rows)}))
