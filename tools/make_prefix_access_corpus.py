"""Freeze previously unused phrase families under new longer-input conditions."""
import collections, csv, gzip, hashlib, json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
out=root/'docs/prefix-access'
seed='prefix-access-20260919'
def digest(s): return hashlib.sha256((seed+'/'+s).encode()).digest()
exposed=set()
for role in ('development','reserved'):
    with gzip.open(root/f'docs/suggestion-coverage/{role}.tsv.gz','rt',encoding='utf-8') as f:
        for row in csv.DictReader(f,delimiter='\t'):
            if row['kind']=='phrase' and row['condition']!='full':exposed.add(row['target'])
entries=collections.defaultdict(list)
source=root/'app/src/main/assets/zh_tw.tsv'
for line in source.read_text(encoding='utf-8').splitlines():
    reading,_,text,frequency,_=line.split('\t')
    if text not in exposed and 2<=len(reading.split("'"))<=5 and len(reading.replace("'",''))<=24:
        entries[text].append((reading,float(frequency)))
syllables=sorted({s.split('\t')[0] for s in (root/'app/src/main/assets/syllables.tsv').read_text(encoding='utf-8').splitlines()})
families=sorted(entries,key=digest)[:6000]
frequency_order=sorted(families,key=lambda t:(max(f for _,f in entries[t]),t))
band={t:str(min(4,i*5//len(families))) for i,t in enumerate(frequency_order)}
rows=[]
for i,text in enumerate(families):
    reading,frequency=max(entries[text],key=lambda pair:(pair[1],pair[0]))
    units=reading.split("'");h=digest(text)
    suffix=[syllables[int.from_bytes(h[2:4],'big')%len(syllables)],syllables[int.from_bytes(h[4:6],'big')%len(syllables)]]
    forms=dict(full=''.join(units),initials=''.join(s[0] for s in units),
               mixed=''.join(s if j%2==0 else s[0] for j,s in enumerate(units)),
               mixed_reverse=''.join(s[0] if j%2==0 else s for j,s in enumerate(units)),
               partial_units=''.join(s[:max(1,len(s)-1)] for s in units),
               separated_initials="'".join(s[0] for s in units),
               separated_mixed="'".join(s if j%2==0 else s[0] for j,s in enumerate(units)))
    for condition,prefix in forms.items():
        separator="'" if condition.startswith('separated') else ''
        tail=separator.join(s[0] if condition=='initials' else s for s in suffix)
        raw=prefix+separator+tail
        rows.append([('development' if i%2==0 else 'validation'),reading,text,str(frequency),band[text],condition,raw,str(len(prefix)),tail])
out.mkdir(exist_ok=True)
for role in ('development','validation'):
    selected=sorted((r for r in rows if r[0]==role),key=lambda r:(r[6],r[2],r[5]))
    data=('role\treading\ttarget\tfrequency\tfrequency_band\tcondition\traw\tconsumed\tsuffix\n'+'\n'.join('\t'.join(r) for r in selected)+'\n').encode()
    (out/f'{role}.tsv.gz').write_bytes(gzip.compress(data,mtime=0))
manifest=dict(seed=seed,baseline='96db0d6',families=len(families),excluded_prior_partial_families=len(exposed),
              rows=len(rows),roles='Seen-source, new suffix conditions; validation frozen before variant results.',
              frequency_bands='0 lowest to 4 highest source frequency quintiles; not independent conversational frequencies.',
              source_sha256=hashlib.sha256(source.read_bytes().replace(b'\r\n',b'\n')).hexdigest(),
              files={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(out.glob('*.tsv.gz'))})
(out/'manifest.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
print(json.dumps(manifest))
