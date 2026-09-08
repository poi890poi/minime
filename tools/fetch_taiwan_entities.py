"""Paginated encyclopedia category graph snapshot; no individual entity allowlists."""
from pathlib import Path
import concurrent.futures,datetime,gzip,hashlib,io,json,re,threading,time,urllib.parse,urllib.request
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'third_party/taiwan_encyclopedia';CACHE=ROOT/'artifacts/taiwan-category-cache'
# Topic roots are schema, never per-entity selections. Historical periods are
# queried directly so modern nationality metadata cannot exclude their people.
ROOTS={
 'people':['台灣日治時期人物','台灣清治時期人物','臺灣明鄭時期人物','台灣荷西殖民時期人物','台灣原住民人物','台灣作家','台灣藝術家','台灣科學家'],
 'performers':['台灣歌手','台灣演員','台灣音樂家','台灣導演'],
 'history':['台灣歷史'], 'indigenous':['台灣原住民族'],
 'animals':['台灣動物','台灣特有種'], 'plants':['台灣植物'],
 'foods':['台灣小吃','台灣食品'], 'beverages':['台灣飲料'],
 'films':['台灣電影作品'], 'books':['台灣小說','台灣文學作品'],
 'songs':['台語歌曲','台灣歌曲']}
DEPTH=2
UA='MinIME-source-import/0.6.4 (https://github.com/poi890poi/minime; category metadata only)'
NETWORK_LOCK=threading.Lock()
def request(url):
    # Shared pacing applies to every worker, including retries/redirect lookup.
    with NETWORK_LOCK:
        time.sleep(0.6)
        return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':UA}),timeout=35).read()
def encode(data):return (json.dumps(data,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n').encode('utf-8')
def compress(data):
    output=io.BytesIO()
    with gzip.GzipFile(fileobj=output,mode='wb',mtime=0) as z:z.write(data)
    return output.getvalue()
def fetch(category):
    cache=CACHE/(hashlib.sha256(category.encode()).hexdigest()+'.json.gz')
    if cache.exists():
        saved=json.loads(gzip.decompress(cache.read_bytes()).decode('utf-8'))
        if saved['members'] or saved.get('soft_redirect_checked'):return saved
    members=[];pages=[];continuation={}
    while True:
        params=dict(action='query',list='categorymembers',cmtitle=category,cmnamespace='0|14',cmlimit=500,format='json',formatversion=2)
        params.update(continuation);url='https://zh.wikipedia.org/w/api.php?'+urllib.parse.urlencode(params)
        for attempt in range(3):
            try:
                data=request(url)
                parsed=json.loads(data)
                if 'error' in parsed:raise ValueError(parsed['error'])
                break
            except Exception:
                if attempt==2:raise
                time.sleep(5*(attempt+1))
        members.extend(parsed['query']['categorymembers']);pages.append(dict(url=url,sha256=hashlib.sha256(data).hexdigest()))
        continuation=parsed.get('continue')
        if not continuation:break
    result=dict(category=category,members=members,requests=pages)
    if not members:
        url='https://zh.wikipedia.org/w/api.php?'+urllib.parse.urlencode(dict(action='query',titles=category,redirects=1,prop='revisions',rvprop='content',rvslots='main',format='json',formatversion=2))
        data=request(url);parsed=json.loads(data);page=parsed['query']['pages'][0];target=page['title']
        content=page.get('revisions',[{}])[0].get('slots',{}).get('main',{}).get('content','')
        redirect=re.search(r'\{\{\s*(?:分類重定向|分类重定向|category redirect|cr)\s*\|\s*(?:1\s*=\s*)?([^|}]+)',content,re.I)
        if redirect:target=redirect.group(1).strip();target=target if target.startswith('Category:') else 'Category:'+target
        result['soft_redirect_checked']=True
        result['redirect_checked']=True;result['requests'].append(dict(url=url,sha256=hashlib.sha256(data).hexdigest()))
        if target!=category:
            resolved=fetch(target);result['members']=resolved['members'];result['redirect_target']=target
    cache.write_bytes(compress(encode(result)));return result
def main():
    OUT.mkdir(exist_ok=True);CACHE.mkdir(exist_ok=True)
    roots=[(sector,'Category:'+title) for sector,titles in ROOTS.items() for title in titles]
    pending={cat for _,cat in roots};nodes={};errors={}
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for depth in range(DEPTH+1):
            futures={pool.submit(fetch,cat):cat for cat in sorted(pending)}
            for future in concurrent.futures.as_completed(futures):
                cat=futures[future]
                try:nodes[cat]=future.result()
                except Exception as e:errors[cat]=str(e)
                if len(nodes)%40==0:print('depth',depth,'categories',len(nodes),'errors',len(errors),flush=True)
            pending={m['title'] for cat in pending if cat in nodes for m in nodes[cat]['members'] if m['ns']==14}-set(nodes)-set(errors)
            print('completed depth',depth,'categories',len(nodes),'next frontier',len(pending),flush=True)
    # Traverse separately per root to retain coverage strata and detect graph cycles.
    entities={};frontiers={};counts={}
    for sector,root in roots:
        visited=set();level={root};found=set()
        for depth in range(DEPTH+1):
            next_level=set()
            for cat in sorted(level-visited):
                visited.add(cat)
                for member in nodes.get(cat,{}).get('members',[]):
                    if member['ns']==14:next_level.add(member['title']);continue
                    key=str(member['pageid']);found.add(key)
                    entity=entities.setdefault(key,dict(pageid=member['pageid'],title=member['title'],sectors=set(),roots=set()))
                    entity['sectors'].add(sector);entity['roots'].add(root)
            level=next_level-visited
        frontiers[root]=sorted(level);counts[root]=len(found)
    for e in entities.values():e['sectors']=sorted(e['sectors']);e['roots']=sorted(e['roots'])
    snapshot=dict(format=1,source='zh.wikipedia.org category metadata',license='CC-BY-SA-4.0',retrieved_utc=datetime.datetime.utcnow().isoformat()+'Z',depth=DEPTH,roots=ROOTS,root_counts=counts,frontiers=frontiers,errors=errors,nodes=nodes,entities=entities,
      limitations='Category ancestry is topic evidence, not popularity or verified biography. All pages within depth are paginated; deeper frontiers are explicit. No modern citizenship gate. No article text or individual names selected.')
    payload=encode(snapshot);(OUT/'snapshot.json.gz').write_bytes(compress(payload))
    manifest={k:v for k,v in snapshot.items() if k not in ('nodes','entities')};manifest.update(entity_count=len(entities),category_count=len(nodes),snapshot_sha256=hashlib.sha256((OUT/'snapshot.json.gz').read_bytes()).hexdigest())
    (OUT/'source.json').write_bytes(encode(manifest));print(json.dumps(dict(entities=len(entities),categories=len(nodes),root_counts=counts,errors=errors),ensure_ascii=False),flush=True)
if __name__=='__main__':main()
