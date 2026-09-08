"""Exhaustive source-key retrieval coverage, not a language-quality accuracy claim."""
from pathlib import Path
import gzip,hashlib,json,re
root=Path(__file__).resolve().parent.parent
out=root/'docs/speculation'
rows=set();sources={};excluded=0
for name in ('addons.tsv','geography.tsv'):
    path=root/'app/src/main/assets'/name
    sources[str(path.relative_to(root)).replace('\\','/')]=hashlib.sha256(path.read_bytes()).hexdigest()
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#'):continue
        pack,key,target,source,category=line.split('\t')
        if not re.fullmatch("[a-z0-9]+(?:[- ']+[a-z0-9]+)*",key):excluded+=1;continue
        parts=re.split("[- ']+",key);raw=''.join(parts)
        # Generated Chinese initial aliases are measured as exact source aliases,
        # but are never expanded into fabricated source syllable boundaries.
        variants={'full':raw}
        if len(raw)>1:variants['prefix']=raw[:-1]
        if len(parts)>1:
            variants.update(initial=''.join(p[0] for p in parts),
                **{'mixed-left':''.join(p if i%2==0 else p[0] for i,p in enumerate(parts)),
                   'mixed-right':''.join(p if i%2 else p[0] for i,p in enumerate(parts))})
        for form,value in variants.items():rows.add((pack,form,value,target))
data=''.join('\t'.join(row)+'\n' for row in sorted(rows)).encode('utf-8')
with (out/'addon-inputs.tsv.gz').open('wb') as f:
    with gzip.GzipFile(filename='',fileobj=f,mode='wb',mtime=0) as z:z.write(data)
manifest={'sources':sources,'cases':len(rows),'queries':len({r[2] for r in rows}),
    'excluded_non_roman_rows':excluded,'sha256':hashlib.sha256((out/'addon-inputs.tsv.gz').read_bytes()).hexdigest(),
    'contract':'Every source row; deduplicated pack/form/input/output. Complete, trailing prefix, initial and mixed source units. Reference outputs score retrieval only and never enter matching. Ambiguous keys may exceed the eight display slots.'}
(out/'addon-corpus-manifest.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
print(json.dumps(manifest))
