"""Aggregate the predeclared paired gate without publishing raw typing data."""
import hashlib,json
from pathlib import Path
root=Path(__file__).parent
runs={tag:json.loads((root/f'{tag}.json').read_text()) for tag in ('a1','b1','b2','a2')}
groups={}
for mode in runs['a1']['groups']:
    cells={tag:run['groups'][mode] for tag,run in runs.items()}
    gate=[]
    for metric in ('editor_submission','space_submission'):
        for percentile in ('p95_ms','p99_ms'):
            deltas=[cells['b'+n][metric][percentile]-cells['a'+n][metric][percentile] for n in ('1','2')]
            if all(d>0 for d in deltas):gate.append(dict(metric=metric,percentile=percentile,deltas_ms=deltas))
    for metric,key in [('candidate_submission','observed'),('candidate_deadline','submitted_before_next_up')]:
        deltas=[cells['b'+n][metric][key]-cells['a'+n][metric][key] for n in ('1','2')]
        if all(d<0 for d in deltas):gate.append(dict(metric=metric,field=key,deltas=deltas))
    groups[mode]=dict(cells=cells,repeated_regressions=gate)
result=dict(scope='Reused fixed development queries, injected events and frame submission, not human hits or release acceptance.',
    execution_order=['a1','b1','b2','a2'],samples={k:v['samples'] for k,v in runs.items()},groups=groups,
    gate='reject' if any(g['repeated_regressions'] for g in groups.values()) else 'needs_candidate_gain_and_broader_followup',
    source_hashes={k:hashlib.sha256((root/f'{k}.json').read_bytes()).hexdigest() for k in runs})
(root/'comparison.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print('Gate:',result['gate'])
for mode,g in groups.items():
    print(mode,'repeated regressions',g['repeated_regressions'])
    for tag in ('a1','b1','a2','b2'):
        v=g['cells'][tag]
        print(tag,'raw',*(round(v['editor_submission'][k],3) for k in ('p95_ms','p99_ms')),
              'space',*(round(v['space_submission'][k],3) for k in ('p95_ms','p99_ms')),
              'candidate',*(round(v['candidate_submission'][k],3) for k in ('p95_ms','p99_ms')),
              'observed',v['candidate_submission']['observed'],'timely',v['candidate_deadline']['submitted_before_next_up'])
