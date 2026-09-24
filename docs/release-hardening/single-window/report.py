"""Frozen four-mode paired screen; public output contains aggregates and hashes."""
import csv,hashlib,json,re,sys
from pathlib import Path
root=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(root/'tools'))
from report_touch_latency import report
here=root/'artifacts/single-window'
log=(here/'timing.log').read_text(encoding='utf-8-sig')
parts=re.split(r'Starting single-window-(a1|b1|b2|a2)\s*',log)
runs={};identities={};workloads={}
for index in range(1,len(parts),2):
    tag,body=parts[index:index+2]
    matches=re.findall(r'Session evidence: .*?device-tests[\\/]([0-9a-f-]{36})',body)
    if len(matches)!=1 or 'OK (1 test)' not in body or 'Final display OFF verified' not in body:
        raise ValueError('Incomplete timing session: '+tag)
    session=matches[0];path=root/'artifacts/device-tests'/session/'touch-latency.tsv'
    with path.open(encoding='utf-8') as stream: rows=list(csv.DictReader(stream,delimiter='\t'))
    runs[tag]=report(rows,tag)
    workloads[tag]=[(r['mode'],r['interval_ms'],r['query'],r['action']) for r in rows]
    if len(rows)!=464:raise ValueError('Unexpected fixed-screen action count')
    identities[tag]=dict(session=session,touch_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
if set(runs)!={'a1','b1','b2','a2'}:raise ValueError('All four completed runs required')
if any(w!=workloads['a1'] for w in workloads.values()):raise ValueError('Paired workloads differ')
groups={};failures=[]
for group in runs['a1']['groups']:
    cells={tag:value['groups'][group] for tag,value in runs.items()};regressions=[]
    for metric in ('editor_submission','space_submission'):
        for tag,cell in cells.items():
            if cell[metric]['unobserved']:failures.append([group,tag,metric,'missing frame'])
        for percentile in ('p95_ms','p99_ms'):
            delta=[cells['b'+n][metric][percentile]-cells['a'+n][metric][percentile] for n in ('1','2')]
            if all(x>2 for x in delta):regressions.append(dict(metric=metric,percentile=percentile,deltas_ms=delta))
    gains={p:[cells['b'+n]['candidate_submission'][p]-cells['a'+n]['candidate_submission'][p] for n in ('1','2')]
           for p in ('p95_ms','p99_ms')}
    for metric,key in [('candidate_submission','observed'),('candidate_deadline','submitted_before_next_up')]:
        delta=[cells['b'+n][metric][key]-cells['a'+n][metric][key] for n in ('1','2')]
        if all(x<0 for x in delta):regressions.append(dict(metric=metric,field=key,deltas=delta))
    groups[group]=dict(cells=cells,repeated_regressions=regressions,candidate_deltas_ms=gains)
result=dict(scope='Reused fixed development queries. Injected input and frame submission only; no physical touch or release acceptance.',
    order=['a1','b1','b2','a2'],samples={k:r['samples'] for k,r in runs.items()},identities=identities,
    groups=groups,missing=failures,
    gate='reject' if failures or any(g['repeated_regressions'] for g in groups.values()) else 'screen_survives; inspect candidate gains and run broad language shards')
(here/'comparison.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(result['gate'])
for name,g in groups.items():
    print(name,'candidate deltas',g['candidate_deltas_ms'],'regressions',g['repeated_regressions'])
