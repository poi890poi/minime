"""Freeze source-wide retrieval contracts; no production data mutations."""
import collections, gzip, hashlib, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/suggestion-coverage'
seed='suggestion-units-20260919'
def digest(s):return hashlib.sha256((seed+'/'+s).encode()).digest()
source=ROOT/'app/src/main/assets/zh_tw.tsv'
entries=sorted({(p[0],p[2]) for line in source.read_text(encoding='utf-8').splitlines() if len(p:=line.split('\t'))==5})
phrases=collections.defaultdict(list)
for reading,text in entries:
    if "'" in reading:phrases[text].append(reading)
families=sorted(phrases,key=digest)
selected={text:('development' if i%2==0 else 'reserved') for i,text in enumerate(families[:12000])}
rows=[]
for reading,text in entries:
    role='reserved' if int.from_bytes(digest(text)[:2],'big')%2 else 'development'
    if text in selected:role=selected[text]
    units=reading.split("'")
    cases={'full':''.join(units)}
    if len(text)==1:
        for end in range(1,len(reading)):cases[f'glyph-prefix-{end}']=reading[:end]
    if text in selected:
        cases.update(initials=''.join(s[0] for s in units),mixed=''.join(s if i%2==0 else s[0] for i,s in enumerate(units)),
                     mixed_reverse=''.join(s[0] if i%2==0 else s for i,s in enumerate(units)),
                     partial_units=''.join(s[:max(1,len(s)-1)] for s in units),
                     final_partial=''.join(units[:-1])+units[-1][:max(1,len(units[-1])-1)],
                     separated="'".join(units),separated_initials="'".join(s[0] for s in units),
                     separated_mixed="'".join(s if i%2==0 else s[0] for i,s in enumerate(units)))
    for condition,raw in cases.items():rows.append([role,'glyph' if len(text)==1 else 'phrase',reading,text,condition,raw,str(len(units))])
OUT.mkdir(exist_ok=True)
for role in ['development','reserved']:
    chosen=sorted((r for r in rows if r[0]==role),key=lambda r:(r[5],r[2],r[3],r[4]))
    data=('role\tkind\treading\ttarget\tcondition\traw\tunits\n'+'\n'.join('\t'.join(r) for r in chosen)+'\n').encode()
    (OUT/f'{role}.tsv.gz').write_bytes(gzip.compress(data,mtime=0))
manifest={'seed':seed,'baseline':'311b1cb','source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
          'plan_sha256':hashlib.sha256((OUT/'PLAN.md').read_bytes()).hexdigest(),
          'script_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
          'source_pairs':len(entries),'selected_phrase_families':len(selected),'roles':'Split by text identity; seen-source retrieval, not natural language accuracy.',
          'counts':dict(collections.Counter(r[0]+'/'+r[1]+'/'+r[4] for r in rows)),
          'corpora':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(OUT.glob('*.tsv.gz'))}}
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print(json.dumps({'rows':len(rows),'source_pairs':len(entries),'unique_queries':len({r[5] for r in rows})}))
