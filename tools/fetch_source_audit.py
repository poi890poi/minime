"""Read-only, cached evidence collection for source assessment, never an importer.

Fetches complete GitHub inventories and a hash-selected article sample. No upstream
code is executed and no candidate snapshot is connected to the production compiler.
"""
from pathlib import Path
import concurrent.futures,gzip,hashlib,io,json,time,urllib.parse,urllib.request

ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'artifacts/source-audit';OUT.mkdir(parents=True,exist_ok=True)
HEADERS={'User-Agent':'MinIME-source-quality-audit (https://github.com/poi890poi/minime)'}

def fetch(url):
    target=OUT/(hashlib.sha256(url.encode()).hexdigest()+'.bin')
    if target.exists():return target.read_bytes()
    for attempt in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url,headers=HEADERS),timeout=40) as response:raw=response.read(16*1024*1024+1)
            if len(raw)>16*1024*1024:raise ValueError('Audit download exceeds 16 MiB')
            target.write_bytes(raw);return raw
        except Exception:
            if attempt==2:raise
            time.sleep(attempt+1)

def save(name,value):
    (OUT/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def github(repo):
    commit=json.loads(fetch('https://api.github.com/repos/'+repo+'/commits/HEAD'))
    sha=commit['sha'];tree=json.loads(fetch('https://api.github.com/repos/'+repo+'/git/trees/'+sha+'?recursive=1'))
    assert not tree.get('truncated'),'Incomplete GitHub inventory'
    result={'repo':repo,'commit':sha,'date':commit['commit']['committer']['date'],'tree':tree['tree']}
    save(repo.split('/')[-1]+'-inventory.json',result)
    return result

def main():
    jobs={
      'taiwan-md':lambda:github('frank890417/taiwan-md'),
      'kemdict':lambda:github('kemdict/kemdict'),
      'cip':lambda:save('cip.json',json.loads(fetch('https://data.cip.gov.tw/API/v1/dump/datastore/A53000000A-111027-001'))),
      'taicol':lambda:(OUT/'taicol.zip').write_bytes(fetch('https://ipt.taibif.tw/archive.do?r=taibnet_com_all&v=1.13')),
      'music':lambda:(OUT/'music.html').write_bytes(fetch('https://opendata.culture.tw/frontsite/barrierFree/openDataDetail/581')),
    }
    results={};errors={}
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        futures={pool.submit(fn):name for name,fn in jobs.items()}
        for future in concurrent.futures.as_completed(futures):
            name=futures[future]
            try:results[name]=future.result();print(name,'downloaded',flush=True)
            except Exception as e:errors[name]=str(e);print(name,'ERROR',e,flush=True)
    samples=[]
    if 'taiwan-md' in results:
        inv=results['taiwan-md'];groups={}
        for e in inv['tree']:
            p=e['path'].split('/')
            if e['type']=='blob' and len(p)==3 and p[0]=='knowledge' and p[-1].endswith('.md') and not p[-1].startswith('_'):
                groups.setdefault(p[1],[]).append(e['path'])
        # A fixed eight per source category, without seeing article contents.
        for group,paths in sorted(groups.items()):
            for path in sorted(paths,key=lambda p:hashlib.sha256(('minime-source-audit-v1:'+p).encode()).hexdigest())[:8]:
                samples.append(('taiwan-md',inv['repo'],inv['commit'],path))
        save('taiwan-md-sample-plan.json',{'seed':'minime-source-audit-v1','category_counts':{g:len(p) for g,p in groups.items()},'sample_paths':[s[3] for s in samples]})
    if 'kemdict' in results:
        inv=results['kemdict']
        for e in inv['tree']:
            if e['type']=='blob' and (e['path'].startswith('dicts/kisaragi/') or e['path'] in ('LICENSE.md','README.org','.gitmodules')):
                samples.append(('kemdict',inv['repo'],inv['commit'],e['path']))
    def sample(item):
        source,repo,sha,path=item;url='https://raw.githubusercontent.com/'+repo+'/'+sha+'/'+urllib.parse.quote(path)
        return {'source':source,'path':path,'url':url,'text':fetch(url).decode('utf8')}
    collected=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        futures={pool.submit(sample,s):s for s in samples}
        for future in concurrent.futures.as_completed(futures):
            try:collected.append(future.result())
            except Exception as e:errors[futures[future][3]]=str(e)
    save('sample-records.json',sorted(collected,key=lambda r:(r['source'],r['path'])))
    try:
        import re
        catalog=json.loads(fetch('https://www.bamid.gov.tw/OpenData.aspx?SN=0FBD3266D50B85F7').decode('utf-8-sig'))
        links=sorted({url for item in catalog for url in re.findall(r'https?://[^()]+\.csv',item.get('相關檔案',''))})
        save('music-catalog.json',catalog)
        music=[]
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            futures={pool.submit(fetch,url):url for url in links}
            for future in concurrent.futures.as_completed(futures):
                url=futures[future]
                try:
                    raw=future.result();music.append(dict(url=url,sha256=hashlib.sha256(raw).hexdigest(),text=raw.decode('utf-8-sig')))
                except Exception as e:errors[url]=str(e)
        save('music-records.json',sorted(music,key=lambda r:r['url']));print('music CSVs',len(music),'of',len(links),flush=True)
    except Exception as e:errors['music-catalog']=str(e)
    save('fetch-errors.json',errors)
    print('samples',len(collected),'errors',len(errors),flush=True)

if __name__=='__main__':main()
