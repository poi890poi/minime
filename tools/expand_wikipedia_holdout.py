"""One deterministic subcategory layer, to cover sparsely populated parent categories."""
import hashlib,json,urllib.parse,urllib.request
from pathlib import Path
root=Path(__file__).resolve().parent.parent/'docs/addons-learning'
data=json.loads((root/'wikipedia-holdout.json').read_text(encoding='utf-8'))
def fetch(category,namespace):
    query=urllib.parse.urlencode(dict(action='query',list='categorymembers',cmtitle=category,cmnamespace=namespace,cmlimit=100 if namespace==14 else 30,format='json',formatversion=2))
    url='https://zh.wikipedia.org/w/api.php?'+query
    raw=urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'MinIMEResearch/0.6 (https://github.com/poi890poi/minime)'}),timeout=35).read()
    return dict(category=category,url=url,sha256=hashlib.sha256(raw).hexdigest(),members=json.loads(raw)['query']['categorymembers'])
for parent in list(data['records']):
    try:
        subs=fetch('Category:'+parent['category'],14)
        children=sorted(subs['members'],key=lambda m:hashlib.sha256(m['title'].encode()).digest())[:3]
        for child in children:
            record=fetch(child['title'],0);record['parent']=parent['category'];data['records'].append(record)
            print(child['title'],len(record['members']),flush=True)
    except Exception as error:data['errors'].append(dict(category=parent['category'],error=str(error)))
data['selection']='Original parent titles plus three subcategories per parent selected by SHA256(title), one layer, max 30 titles each. Frozen before evaluation.'
(root/'wikipedia-expanded-holdout.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
