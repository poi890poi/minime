import gzip,json,statistics
from pathlib import Path
OUT=Path('docs/japanese-pipeline-performance'); RAW=Path('artifacts/japanese-pipeline')
def stats(a):
    a=sorted(a)
    return {'n':len(a),'mean_ms':statistics.mean(a)/1e6,'p50_ms':a[len(a)//2]/1e6,'p95_ms':a[int(len(a)*.95)]/1e6,'p99_ms':a[int(len(a)*.99)]/1e6,'max_ms':max(a)/1e6}
report={'runs':{},'gate_ms':50,'physical_touch_measured':False}; expected={}
for name in ['baseline','candidate','repeat','focused','focused-repeat']:
    p=RAW/(name+'-phone.jsonl')
    rows=[json.loads(line) for line in p.read_text(encoding='utf-8-sig').splitlines()]
    native=[r for r in rows if r['kind']=='native']; pipeline=[r for r in rows if r['kind']=='pipeline']
    assert len(native)==3933 and all(r['equal'] for r in native)
    assert len(pipeline)==192
    for r in pipeline:
        key=(r['cycle'],r['id']); value=(r['committed'],r['candidates'])
        if name=='baseline':expected[key]=value
        else:assert expected[key]==value,(name,key)
    groups={}
    for cycle in range(3):
        v=[r for r in pipeline if r['cycle']==cycle]
        groups[str(cycle)]={k:stats([r[k] for r in v]) for k in ['elapsed_ns','last_key_ns','dispatch_ns','addon_and_conversion_ns']}
    groups['warm']=stats([r['elapsed_ns'] for r in pipeline if r['cycle']>0])
    groups['sources']={source:stats([r['elapsed_ns'] for r in pipeline if r['cycle']>0 and r['id'].split(':')[0]==source]) for source in sorted({r['id'].split(':')[0] for r in pipeline})}
    groups['summary']=next(r for r in rows if r['kind']=='summary')
    report['runs'][name]=groups
    (OUT/(name+'-phone.jsonl.gz')).write_bytes(gzip.compress(p.read_bytes(),mtime=0))
report['regex_only_warm_gate_pass']=all(report['runs'][name]['warm']['p95_ms']<=50 for name in ['candidate','repeat'])
report['focused_warm_gate_pass']=all(report['runs'][name]['warm']['p95_ms']<=50 for name in ['focused','focused-repeat'])
report['pipeline_acceptance_comparisons']=192*(len(report['runs'])-1)
(OUT/'phone-results.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({name:{'warm':r['warm'],'passes':{c:round(r[c]['elapsed_ns']['p95_ms'],3) for c in ['0','1','2']}} for name,r in report['runs'].items()},indent=2))
