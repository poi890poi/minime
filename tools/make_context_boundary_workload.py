"""Sample query identities before timing; no target labels in scoring workload."""
import csv,hashlib,json,collections
from pathlib import Path
root=Path(__file__).resolve().parents[1];source=root/'artifacts/context-backoff/before.tsv';out=root/'docs/context-boundary'
queries={}
with source.open(encoding='utf-8') as f:
    for row in csv.DictReader(f,delimiter='\t'):
        key=(row['context'],row['raw'])
        queries.setdefault(key,row['outputs'].split('|')[:8])
chosen=sorted(queries,key=lambda k:hashlib.sha256(('context-boundary-20260919/'+repr(k)).encode()).digest())[:1024]
rows=[(key[0],v.rsplit(':',1)[0]) for key in chosen for v in queries[key] if v]
(out/'workload.tsv').write_text(''.join(context+'\t'+word+'\n' for context,word in rows),encoding='utf-8')
(out/'workload-manifest.json').write_text(json.dumps({'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
    'queries':len(chosen),'pairs':len(rows),'lengths':dict(sorted(collections.Counter(len(w) for c,w in rows).items())),
    'selection':'First eight alternatives from 1024 hash-selected unique baseline context/raw queries; targets are not used.',
    'sha256':hashlib.sha256((out/'workload.tsv').read_bytes()).hexdigest()},indent=2)+'\n',encoding='utf-8')
print(len(rows))
