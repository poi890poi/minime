"""Validate entity identity independently of category ancestry (e.g. people vs albums)."""
from pathlib import Path
import concurrent.futures,gzip,hashlib,json,time,urllib.parse
from fetch_taiwan_entities import request,compress,encode,ROOT
OUT=ROOT/'third_party/taiwan_encyclopedia';CACHE=ROOT/'artifacts/taiwan-type-cache'
def fetch(titles):
    params=dict(action='wbgetentities',sites='zhwiki',titles='|'.join(titles),props='claims|sitelinks',sitefilter='zhwiki',format='json')
    url='https://www.wikidata.org/w/api.php?'+urllib.parse.urlencode(params)
    cache=CACHE/(hashlib.sha256(url.encode()).hexdigest()+'.json.gz')
    if cache.exists():return json.loads(gzip.decompress(cache.read_bytes()).decode())
    for attempt in range(3):
        try:
            raw=request(url);data=json.loads(raw)
            if 'error' in data:raise ValueError(data['error'])
            break
        except Exception:
            if attempt==2:raise
            time.sleep(5*(attempt+1))
    entities={}
    for key,e in data['entities'].items():
        title=e.get('sitelinks',{}).get('zhwiki',{}).get('title')
        if not title:continue
        claims={}
        for prop in ('P31','P569','P570','P106','P21','P27'):
            claims[prop]=[c['mainsnak']['datavalue']['value'] for c in e.get('claims',{}).get(prop,[]) if c.get('rank')!='deprecated' and 'datavalue' in c['mainsnak']]
        entities[title]=dict(id=key,claims=claims)
    result=dict(requested_titles=titles,url=url,response_sha256=hashlib.sha256(raw).hexdigest(),entities=entities)
    cache.write_bytes(compress(encode(result)));return result
def main():
    CACHE.mkdir(exist_ok=True)
    snapshot=json.loads(gzip.decompress((OUT/'snapshot.json.gz').read_bytes()).decode());batches=[];batch=[]
    for title in sorted({e['title'] for e in snapshot['entities'].values()}):
        if len(batch)==50 or len(urllib.parse.quote('|'.join(batch+[title])))>6500:batches.append(batch);batch=[]
        batch.append(title)
    if batch:batches.append(batch)
    records=[];errors=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        futures={pool.submit(fetch,b):b for b in batches}
        for f in concurrent.futures.as_completed(futures):
            try:records.append(f.result())
            except Exception as e:errors.append(dict(titles=futures[f],error=str(e)))
            if (len(records)+len(errors))%20==0:print(len(records), '/',len(batches),'batches; errors',len(errors),flush=True)
    result=dict(source='Wikidata structured claims linked by Chinese Wikipedia sitelink',license='CC0-1.0',snapshot_sha256=hashlib.sha256((OUT/'snapshot.json.gz').read_bytes()).hexdigest(),requests=sorted(records,key=lambda x:x['url']),errors=errors)
    (OUT/'entity-types.json.gz').write_bytes(compress(encode(result)));print('complete',len(records),'errors',len(errors),flush=True)
if __name__=='__main__':main()
