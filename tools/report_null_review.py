"""Report no-construction review changes against immutable null observations."""
from pathlib import Path
from collections import defaultdict
import json,gzip,statistics
R=Path(__file__).resolve().parent.parent;O=R/'docs/null-review'
def read(path):return [json.loads(s) for s in gzip.decompress(path.read_bytes()).decode().splitlines()]
report={'scope':'Consumed prose regression. Reference reconstruction, not semantic precision or a fresh conversation holdout.','groups':{},'invariants':{}}
for role in ['development','reserved']:
    data={'baseline':read(R/'docs/null-construction'/(role+'-null.jsonl.gz'))}
    data.update({s:read(O/(role+'-'+s+'.jsonl.gz')) for s in ['visible','stored','ordered']})
    for stage,rows in data.items():
        base=data['baseline'];assert len(rows)==len(base)
        report['invariants'][role+'/'+stage]={'observations':len(rows),'constructed':sum(r['constructed'] for r in rows),'space_changes':sum((a['space'],a['remaining'])!=(b['space'],b['remaining']) for a,b in zip(base,rows)),'hidden_defaults':sum(r['preferred']>1 for r in rows)}
    for condition in ['full','initials','mixed','partial']:
        for addons in [False,True]:
            key=role+'/'+condition+'/addons='+str(addons).lower();report['groups'][key]={}
            baseline=[r for r in data['baseline'] if r['condition']==condition and r['addons']==addons]
            for stage,rows in data.items():
                part=[r for r in rows if r['condition']==condition and r['addons']==addons]
                report['groups'][key][stage]={'n':len(part),'rank1':sum(r['rank']==1 for r in part),'recall8':sum(0<r['rank']<=8 for r in part),'recall_all':sum(r['rank']>0 for r in part),'space_hits':sum(r['hit'] for r in part),'mrr':sum(1/r['rank'] if r['rank'] else 0 for r in part)/len(part),'lost8':sum(0<b['rank']<=8 and not 0<r['rank']<=8 for b,r in zip(baseline,part)),'gained8':sum(not 0<b['rank']<=8 and 0<r['rank']<=8 for b,r in zip(baseline,part))}
(O/'summary.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report['invariants'],indent=2))
for k,v in report['groups'].items():
    if k.startswith('reserved') and k.endswith('true'):print(k,v)
