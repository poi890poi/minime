"""Strict parity gate, separate from descriptive changes to historical tie order."""
from pathlib import Path
import gzip, json, statistics
HERE = Path(__file__).resolve().parent
OLD = HERE.parent / 'japanese-engine-benchmark'

def read(path):
    return [json.loads(s) for s in gzip.open(path, 'rt', encoding='utf8')]

def clean(x, omit=('engine_ns', 'wall_ns')):
    if isinstance(x, dict): return {k: clean(v, omit) for k,v in x.items() if k not in omit}
    if isinstance(x, list): return [clean(v, omit) for v in x]
    return x

def quantiles(a):
    a=sorted(a)
    return dict(n=len(a), mean=statistics.mean(a), p50=statistics.median(a),
                p95=a[int(len(a)*.95)], p99=a[int(len(a)*.99)], max=max(a))

def main():
    result={'parity': {}, 'historical_order_changes': {}, 'performance': {}}
    stems=[f'{c}-{r}' for r in ['development','holdout'] for c in ['completion','probes']]+['ajimee-development']
    failures=0
    for stem in stems:
        a=read(HERE/f'kazuma-stable-{stem}.jsonl.gz')
        b=read(HERE/f'kazuma-indexed-stable-{stem}.jsonl.gz')
        assert len(a)==len(b)
        changed=sum(clean(x)!=clean(y) for x,y in zip(a,b))
        failures+=changed
        result['parity'][stem]={'rows':len(a),'changed':changed}
        old=read(OLD/f'kazuma-{stem}.jsonl.gz')
        assert len(old)==len(a)
        result['historical_order_changes'][stem]={
            'rows':len(a),'changed':sum(clean(x)!=clean(y) for x,y in zip(old,a)),
            'changed_beyond_candidate_lists':sum(clean(x,('engine_ns','wall_ns','candidates'))!=clean(y,('engine_ns','wall_ns','candidates')) for x,y in zip(old,a))}
    for engine in ['kazuma-stable','kazuma-indexed-stable']:
        result['performance'][engine]={}
        for p in [1,2,3]:
            a=read(HERE/f'{engine}-perf-{p}.jsonl.gz')
            result['performance'][engine][str(p)]={cycle:{
                'engine_ms':quantiles([x['engine_ns']/1e6 for x in a if x['cycle']==cycle]),
                'wall_ms':quantiles([x['wall_ns']/1e6 for x in a if x['cycle']==cycle])} for cycle in ['first','repeat']}
    (HERE/'summary.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
    print(json.dumps(result,indent=2))
    if failures: raise SystemExit('FAIL exact parity: '+str(failures))
    print('PASS complete ordered parity over',sum(v['rows'] for v in result['parity'].values()),'regression records')

if __name__=='__main__': main()
