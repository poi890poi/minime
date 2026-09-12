"""Download a pinned candidate for audit only; never writes production assets."""
from pathlib import Path
import urllib.request,json,hashlib,gzip
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'docs/whole-input-quality/ngram-source';OUT.mkdir(parents=True,exist_ok=True)
PIN='2931fe68b3008f87071cbaa28248162d0d596324'
base='https://huggingface.co/datasets/taiwan-corpora/twngrams/resolve/'+PIN+'/'
rows=[]
for name in ['README.md','NOTICES.md','LICENSE','token_1gram.tsv','token_2gram.tsv','token_3gram.tsv','token_4gram.tsv']:
    target=OUT/(name+'.gz' if name.endswith('.tsv') else name)
    if target.exists():data=gzip.decompress(target.read_bytes()) if name.endswith('.tsv') else target.read_bytes()
    else:
        with urllib.request.urlopen(base+name,timeout=60) as response:data=response.read()
        target.write_bytes(gzip.compress(data,mtime=0) if name.endswith('.tsv') else data)
    rows.append(dict(file=target.name,url=base+name,original_bytes=len(data),sha256=hashlib.sha256(data).hexdigest()))
    print(name,len(data),flush=True)
(OUT/'manifest.json').write_text(json.dumps(dict(source='taiwan-corpora/twngrams',pin=PIN,role='audit only; no production promotion',files=rows),indent=2)+'\n',encoding='utf-8')
