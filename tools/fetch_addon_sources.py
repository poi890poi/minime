"""Fetch public source snapshots. Runtime IME never downloads data."""
from pathlib import Path
import urllib.request, urllib.parse, json, hashlib
ROOT=Path(__file__).resolve().parent.parent
def get(url):
    return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'MinIME-source-import/0.1'}),timeout=50).read()
def save(folder,name,url):
    p=ROOT/'third_party'/folder;p.mkdir(exist_ok=True)
    data=get(url);(p/name).write_bytes(data)
    (p/(name+'.source.json')).write_text(json.dumps(dict(url=url,sha256=hashlib.sha256(data).hexdigest()),indent=2)+'\n',encoding='utf-8')
    print(name,len(data))
if __name__=='__main__':
    repo='https://api.github.com/repos/ChhoeTaigi/ChhoeTaigiDatabase'
    revision=json.loads(get(repo+'/commits/master'))['sha']
    base='https://raw.githubusercontent.com/ChhoeTaigi/ChhoeTaigiDatabase/'+revision+'/'
    save('itaigi','itaigi.csv',base+'ChhoeTaigiDatabase/ChhoeTaigi_iTaigiHoataiTuichiautian.csv')
    save('itaigi','README.md',base+'README.md')
    query='''SELECT ?item ?itemLabel WHERE {
      ?item wdt:P31 wd:Q11424; wdt:P495 wd:Q865.
      SERVICE wikibase:label { bd:serviceParam wikibase:language "zh-tw,zh-hant,zh,en". }
    } ORDER BY ?item LIMIT 60'''
    try:save('wikidata','taiwan-films.json','https://query.wikidata.org/sparql?format=json&query='+urllib.parse.quote(query))
    except Exception as error:print('Wikidata query unavailable:',error)
