"""Fetch pinned public source evidence and dialogue archives into evaluation-only storage."""
from pathlib import Path
import urllib.request,urllib.parse,json,hashlib,zipfile,io
ROOT=Path(__file__).resolve().parents[2];DEST=ROOT/'artifacts/language-contract-benchmark/conversation-sources';DEST.mkdir(parents=True,exist_ok=True)
def fetch(url):
    req=urllib.request.Request(url,headers={'User-Agent':'MinIME corpus provenance audit'})
    return urllib.request.urlopen(req,timeout=90).read()
log=[]
for repo,branch in [('nu-dialogue/real-persona-chat','28d0b6b3865b29cabc26c230a2db37cdf315e937'),('megagonlabs/asdc','f37d6575c73602adcb8ae3687520726793baa672'),('Taiwanese-Corpus/Lan-Lai-Oh-Taigi','3a8ffeb6e2e9d5c7eeb54173a41d6ad06b325d8b'),('SuiSiann/SuiSiann-TsitTshing','242cf1ef7ce4679e7301f08353bc054791b79840')]:
    folder=DEST/('suisiann-thousand' if repo.startswith('SuiSiann/') else repo.split('/')[-1]);folder.mkdir(exist_ok=True)
    tree=json.loads((folder/'tree.json').read_text(encoding='utf8')) if (folder/'tree.json').exists() else json.loads(fetch('https://api.github.com/repos/'+repo+'/git/trees/'+branch+'?recursive=1'))
    assert tree['sha']==branch,'Cached source must match the frozen revision'
    (folder/'tree.json').write_text(json.dumps(tree),encoding='utf8');rev=tree['sha']
    url='https://codeload.github.com/'+repo+'/zip/'+rev
    # Text-only RealPersonaChat and ASDC archives; avoid Taiwanese MP3/PDF archive.
    if not repo.startswith('Taiwanese-Corpus'):
        payload=(folder/'source.zip').read_bytes() if (folder/'source.zip').exists() else fetch(url);(folder/'source.zip').write_bytes(payload)
        archive=zipfile.ZipFile(io.BytesIO(payload))
        for n in archive.namelist():
            relative=Path(*Path(n).parts[1:])
            if relative.name in ['LICENSE','LICENSE.txt','README.md'] and len(relative.parts)==1:(folder/relative).write_bytes(archive.read(n))
        log.append({'repo':repo,'revision':rev,'url':url,'sha256':hashlib.sha256(payload).hexdigest(),'bytes':len(payload)})
        print(repo,rev,len(payload),flush=True)
    else:
        selected=[x['path'] for x in tree['tree'] if x['path'].startswith('csv/') or x['path']=='README.md']
        for name in selected:
            if name.endswith('.csv') or name=='README.md':
                u='https://raw.githubusercontent.com/'+repo+'/'+rev+'/'+urllib.parse.quote(name);p=folder/name;payload=p.read_bytes() if p.exists() else fetch(u);p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(payload)
                log.append({'repo':repo,'revision':rev,'url':u,'sha256':hashlib.sha256(payload).hexdigest(),'bytes':len(payload)})
        print(repo,rev,'CSV evidence downloaded; license not assumed',flush=True)
frozen=ROOT/'docs/language-contract-benchmark/conversations/manifest.json'
if frozen.exists():
    expected={x['url']:x['sha256'] for x in json.loads(frozen.read_text(encoding='utf8'))['source_downloads']}
    for item in log:
        assert expected.get(item['url'])==item['sha256'],'Downloaded bytes differ from the frozen source: '+item['url']
(DEST/'downloads.json').write_text(json.dumps(log,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
