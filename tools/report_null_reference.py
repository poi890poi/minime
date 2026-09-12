"""Summarize all frozen phone observations and copy their evidence without cherry-picking."""
from pathlib import Path
import json,shutil,hashlib,argparse
p=argparse.ArgumentParser();p.add_argument('--run',default='phone',choices=['phone','phone-delayed']);args=p.parse_args()
R=Path(__file__).resolve().parent.parent;O=R/'docs/null-construction';S=R/'artifacts/null-construction'/args.run;D=O/args.run;D.mkdir(exist_ok=True)
refs={r['id']:r for r in json.loads((O/'reference-sample.json').read_text(encoding='utf-8'))}
records=json.loads((S/'observations.json').read_text(encoding='utf-8'));out=[]
for r in records:
    ref=refs[r['id']];stage='settled' if args.run=='phone-delayed' else 'typed';typed=next((s for s in r['steps'] if s['stage']==stage),{});space=next((s for s in r['steps'] if s['stage']=='space'),{})
    values=typed.get('visible',[])
    if r['provider']=='minime':
        # The legacy flat dump includes a container label. A candidate leaf also
        # emits its text immediately before its description; the container does not.
        candidates=list(dict.fromkeys(s[len(prefix):] for i,s in enumerate(values) for prefix in ['Candidate ','Exact input '] if s.startswith(prefix) and i>0 and values[i-1]==s[len(prefix):]))
    else:
        stop=next((i for i,v in enumerate(values) if v in ['q','Q']),len(values))
        candidates=[s for s in dict.fromkeys(values[:stop]) if s not in ['其他候選鍵','選取中文鍵盤','中文鍵盤','英文鍵盤','語音輸入','…']]
    # Candidate extraction follows the existing audited parity harness. These are
    # visible items, not a claim that eight candidates fit either phone layout.
    output=space.get('text','');out.append(dict(id=r['id'],provider=r['provider'],status=r['status'],genre=ref['genre'],condition=ref['condition'],raw=ref['raw'],target=ref['target'],visible_candidates=candidates,visible_reference=ref['target'] in candidates,space=output,space_reference=output.rstrip()==ref['target'],error=r.get('error')))
manifest={}
for path in S.iterdir():
    if path.suffix not in ['.json','.png','.txt']:continue
    shutil.copyfile(path,D/path.name);manifest[path.name]=hashlib.sha256(path.read_bytes()).hexdigest()
(D/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
(O/('reference-comparison'+('-delayed' if args.run=='phone-delayed' else '')+'.json')).write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for genre in sorted({r['genre'] for r in out}):
    for provider in ['google','minime']:
        rows=[r for r in out if r['genre']==genre and r['provider']==provider];valid=[r for r in rows if r['status']=='observed']
        print(genre,provider,'valid',len(valid),'/',len(rows),'visible',sum(r['visible_reference'] for r in valid),'space',sum(r['space_reference'] for r in valid))
