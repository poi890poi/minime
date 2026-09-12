"""Fetch only the pre-frozen, pinned evaluation articles; no production importer."""
from pathlib import Path
import json,urllib.request,urllib.parse,hashlib,concurrent.futures,gzip
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'docs/construction-confidence'
plan=json.loads((OUT/'reserved-documents.json').read_text(encoding='utf-8'))
cache=ROOT/'artifacts/construction-validation';cache.mkdir(exist_ok=True)
def fetch(path):
    url='https://raw.githubusercontent.com/'+plan['repo']+'/'+plan['revision']+'/'+urllib.parse.quote(path)
    target=cache/(hashlib.sha256(url.encode()).hexdigest()+'.md')
    if not target.exists():
        with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'MinIME-evaluation'}),timeout=35) as r:data=r.read(1024*1024+1)
        assert len(data)<=1024*1024;target.write_bytes(data)
    data=target.read_bytes()
    return dict(source='taiwan-md',path=path,url=url,sha256=hashlib.sha256(data).hexdigest(),text=data.decode('utf-8'))
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:rows=list(pool.map(fetch,plan['paths']))
with (OUT/'reserved-articles.json.gz').open('wb') as f:
    with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as z:z.write((json.dumps(rows,ensure_ascii=False)+'\n').encode())
print('Fetched all',len(rows),'pre-frozen evaluation articles')
