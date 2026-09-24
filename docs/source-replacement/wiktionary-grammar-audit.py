"""Bounded, complete category discovery. Never edits production assets."""
import argparse,collections,datetime,hashlib,json,time,urllib.parse,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'artifacts/source-audit/wiktionary-grammar'
API='https://en.wiktionary.org/w/api.php'
ROOT_CATEGORY='Category:English contractions'


def walk(fetch,root_category=ROOT_CATEGORY):
    queue=collections.deque([root_category]);seen=set();members={};calls=0
    while queue:
        category=queue.popleft()
        if category in seen:continue
        seen.add(category)
        if len(seen)>64:raise ValueError('Category traversal exceeds audit budget')
        continuation={};tokens=set()
        while True:
            calls+=1
            if calls>100:raise ValueError('Request count exceeds audit budget')
            params=dict(action='query',format='json',formatversion=2,list='categorymembers',cmtitle=category,cmlimit=500,cmprop='ids|title|type',cmtype='page|subcat',**continuation)
            response=fetch(params)
            if 'error' in response or 'query' not in response or 'categorymembers' not in response['query']:raise ValueError('Incomplete category response')
            for item in response['query']['categorymembers']:
                kind=item['type'];pageid=item['pageid'];title=item['title']
                if kind=='subcat':queue.append(title)
                elif kind!='page':raise ValueError('Unexpected member type')
                key=(category,pageid)
                if key in members:raise ValueError('Repeated category member during pagination')
                members[key]=dict(category=category,pageid=pageid,title=title,type=kind,ns=item['ns'])
            if 'continue' not in response:break
            continuation=response['continue'];token=json.dumps(continuation,sort_keys=True)
            if not continuation or token in tokens:raise ValueError('Invalid continuation cycle')
            tokens.add(token)
    return dict(categories=sorted(seen),members=[members[k] for k in sorted(members)],requests=calls)


def fetch_snapshot(round_number,out=OUT,root_category=ROOT_CATEGORY):
    folder=out/f'round-{round_number}';folder.mkdir(parents=True,exist_ok=False)
    requests=[]
    def fetch(params):
        url=API+'?'+urllib.parse.urlencode(params)
        request=urllib.request.Request(url,headers={'User-Agent':'MinIMESourceAudit/1.0 (https://github.com/poi890poi/minime)'})
        with urllib.request.urlopen(request,timeout=30) as response:raw=response.read(4*1024*1024+1)
        if len(raw)>4*1024*1024:raise ValueError('Response exceeds size bound')
        name=f'{len(requests):03d}.json';(folder/name).write_bytes(raw)
        requests.append(dict(file=name,url=url,utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),sha256=hashlib.sha256(raw).hexdigest()))
        (folder/'requests.json').write_text(json.dumps(requests,indent=2)+'\n')
        time.sleep(.3)
        return json.loads(raw)
    result=walk(fetch,root_category)
    (folder/'snapshot.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return result


def main(kind='contractions'):
    out=OUT if kind=='contractions' else ROOT/'artifacts/source-audit/wiktionary-pronouns'
    category=ROOT_CATEGORY if kind=='contractions' else 'Category:English pronouns'
    out.mkdir(parents=True,exist_ok=True)
    before=fetch_snapshot(1,out,category);after=fetch_snapshot(2,out,category)
    if before['categories']!=after['categories'] or before['members']!=after['members']:raise ValueError('Membership changed during capture; do not admit this snapshot')
    raw=(json.dumps(before,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()
    (out/'snapshot.json').write_bytes(raw)
    print(json.dumps(dict(categories=len(before['categories']),memberships=len(before['members']),unique_pages=len({r['pageid'] for r in before['members'] if r['type']=='page'}),sha256=hashlib.sha256(raw).hexdigest(),stable_two_passes=True)))


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--category',choices=['contractions','pronouns'],default='contractions')
    main(parser.parse_args().category)
