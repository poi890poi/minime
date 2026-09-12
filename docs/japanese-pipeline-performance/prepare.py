import gzip,hashlib,json
from pathlib import Path
OUT=Path('docs/japanese-pipeline-performance')
p=Path('docs/japanese-provider-integration/stream.jsonl.gz')
clauses={}
with gzip.open(p,'rt',encoding='utf-8') as f:
    for line in f:
        row=json.loads(line);clauses[row['id']]=row['raw']
text='\n'.join(clauses.values())+'\n'
(OUT/'clauses.txt').write_text(text,encoding='utf-8',newline='\n')
manifest={'baseline':'8f2003b','source':p.as_posix(),'source_sha256':hashlib.sha256(p.read_bytes()).hexdigest(),
          'clauses_sha256':hashlib.sha256(text.encode()).hexdigest(),'clauses':len(clauses),
          'selection':'All prior phone clauses, last raw prefix per ID, unchanged source order',
          'role':'consumed performance regression; no fresh accuracy holdout'}
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
