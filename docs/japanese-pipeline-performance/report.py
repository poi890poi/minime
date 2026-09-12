import csv,gzip,hashlib,json,statistics
from pathlib import Path
OUT=Path('docs/japanese-pipeline-performance'); RAW=Path('artifacts/japanese-pipeline')
def stats(a):
    a=sorted(a)
    return {'n':len(a),'mean_ms':statistics.mean(a)/1e6,'p50_ms':a[len(a)//2]/1e6,'p95_ms':a[int(len(a)*.95)]/1e6,'p99_ms':a[int(len(a)*.99)]/1e6,'max_ms':max(a)/1e6}
def read(p):
    with p.open(encoding='utf-8') as f:return list(csv.DictReader(f,delimiter='\t'))
reference=read(RAW/'baseline-1.tsv'); expected={int(r['clause']):(r['digest'],r['preferred']) for r in reference}
with gzip.open('docs/japanese-provider-integration/stream.jsonl.gz','rt',encoding='utf-8') as f:
    ids=list(dict.fromkeys(json.loads(line)['id'] for line in f))
report={'baseline':'8f2003b','runtime':'Microsoft OpenJDK 17.0.11; Windows; -Xmx1g',
        'scope':'Deferred core dispatch/delivery, no native conversion or Android scheduling; consumed stream', 'runs':{}}
for p in sorted(RAW.glob('*-[12].tsv')):
    rows=read(p); assert len(rows)==1280
    assert all(expected[int(r['clause'])]==(r['digest'],r['preferred']) for r in rows),p
    report['runs'][p.stem]={phase:{k:stats([int(r[k]) for r in rows if (int(r['cycle'])==0)==(phase=='first')]) for k in ['dispatch_ns','lookup_delivery_ns']} for phase in ['first','warm']}
    report['runs'][p.stem]['sources']={source:{k:stats([int(r[k]) for r in rows if int(r['cycle'])>0 and ids[int(r['clause'])].split(':')[0]==source]) for k in ['dispatch_ns','lookup_delivery_ns']} for source in sorted({s.split(':')[0] for s in ids})}
    (OUT/(p.name+'.gz')).write_bytes(gzip.compress(p.read_bytes(),mtime=0))
with gzip.open('docs/java-lookup-performance/candidate-2.tsv.gz','rt',encoding='utf-8') as f:old=list(csv.DictReader(f,delimiter='\t'))
new=read(RAW/'lookup.tsv'); assert len(new)==len(old)==40785
assert all((a['row'],a['pass'],a['digest'])==(b['row'],b['pass'],b['digest']) for a,b in zip(old,new))
(OUT/'lookup.tsv.gz').write_bytes(gzip.compress((RAW/'lookup.tsv').read_bytes(),mtime=0))
report['ordered_lookup_metadata_comparisons']=len(new)
report['ordered_pipeline_candidate_and_preferred_comparisons']=1280*len(report['runs'])
report['hashes']={p:hashlib.sha256(Path(p).read_bytes()).hexdigest() for p in ['docs/japanese-provider-integration/stream.jsonl.gz','docs/japanese-pipeline-performance/clauses.txt','docs/java-lookup-performance/inputs.tsv']}
(OUT/'results.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
