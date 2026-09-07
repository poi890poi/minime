import json,urllib.parse,urllib.request,hashlib
from pathlib import Path
out=Path('docs/addons-learning');original=json.loads((out/'wikipedia-expanded-holdout.json').read_text(encoding='utf-8'))
seen={m['pageid'] for r in original['records'] for m in r['members']}
records=[];errors=[]
# A fresh tail sample from the same independent categories, after the promotion rule was proposed.
for category in original['records']:
 if len(category['members'])<30:continue
 params=urllib.parse.parse_qs(urllib.parse.urlparse(category['url']).query);params['cmlimit']=['100'];url='https://zh.wikipedia.org/w/api.php?'+urllib.parse.urlencode({k:v[0] for k,v in params.items()})
 try:
  raw=urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'MinIMEResearch/0.6 (https://github.com/poi890poi/minime)'}),timeout=35).read()
  members=[m for m in json.loads(raw)['query']['categorymembers'] if m['pageid'] not in seen]
  for m in members:seen.add(m['pageid'])
  records.append(dict(category=category['category'],url=url,sha256=hashlib.sha256(raw).hexdigest(),members=members));print(category['category'],len(members),flush=True)
 except Exception as error:errors.append(dict(category=category['category'],error=str(error)))
(out/'wikipedia-fresh-holdout.json').write_text(json.dumps(dict(role='fresh holdout after general source-member promotion; not used in compilation',records=records,errors=errors),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
