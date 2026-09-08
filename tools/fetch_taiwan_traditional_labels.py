"""Resolve changed names from explicit source labels, never conversion guesses."""
import concurrent.futures,gzip,hashlib,json,sys,time,urllib.parse,re
from fetch_taiwan_entities import request,compress,encode,ROOT
sys.path.insert(0,str(ROOT/'third_party/opencc_python'))
from opencc import OpenCC
OUT=ROOT/'third_party/taiwan_encyclopedia';CACHE=ROOT/'artifacts/taiwan-label-cache'
def fetch(ids):
    url='https://www.wikidata.org/w/api.php?'+urllib.parse.urlencode(dict(action='wbgetentities',ids='|'.join(ids),props='labels',languages='zh-tw|zh-hant',format='json'))
    path=CACHE/(hashlib.sha256(url.encode()).hexdigest()+'.json.gz')
    if path.exists():return json.loads(gzip.decompress(path.read_bytes()).decode())
    for attempt in range(3):
        try:
            raw=request(url);data=json.loads(raw)
            if 'error' in data:raise ValueError(data['error'])
            break
        except Exception:
            if attempt==2:raise
            time.sleep(5*(attempt+1))
    result=dict(url=url,response_sha256=hashlib.sha256(raw).hexdigest(),entities=data['entities'])
    path.write_bytes(compress(encode(result)));return result
def main():
    CACHE.mkdir(exist_ok=True);types=json.loads(gzip.decompress((OUT/'entity-types.json.gz').read_bytes()).decode());converter=OpenCC('s2tw');ids=set()
    for batch in types['requests']:
        for title,e in batch['entities'].items():
            word=re.sub(r'\s*[（(][^()（）]*[)）]$','',title).strip()
            if converter.convert(word)!=word:ids.add(e['id'])
    ids=sorted(ids);records=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        for result in pool.map(fetch,[ids[i:i+50] for i in range(0,len(ids),50)]):records.append(result);print(len(records),'label batches',flush=True)
    (OUT/'traditional-labels.json.gz').write_bytes(compress(encode(dict(source='Wikidata explicit zh-tw/zh-hant labels only',license='CC0-1.0',types_sha256=hashlib.sha256((OUT/'entity-types.json.gz').read_bytes()).hexdigest(),requests=records))))
    print('complete',len(ids),'entities')
if __name__=='__main__':main()
