"""Output parity and descriptive results; no scoring data reaches a provider."""
import collections, json, statistics
from evaluate import HERE,read,dump
OLD=HERE.parent/'japanese-engine-benchmark'
def quantiles(a):
    a=sorted(a)
    return {'n':len(a),'p50':statistics.median(a),'p95':a[min(len(a)-1,int(len(a)*.95))],
            'p99':a[min(len(a)-1,int(len(a)*.99))],'max':max(a)} if a else {}
def behavior(x):
    if isinstance(x,dict):return {k:behavior(v) for k,v in x.items() if k not in ['engine_ns','wall_ns']}
    if isinstance(x,list):return [behavior(v) for v in x]
    return x
def main():
    result={'screen':{},'parity':{},'performance':{}}
    for mode in ['cps','minime','graph','lexical','lexical-fullprefix','indexed-cps','indexed-lexical','indexed-lexical-fullprefix']:
        for unit in ['clause','word']:
            p=HERE/f'{mode}-{unit}.jsonl.gz'
            if not p.exists():continue
            a=read(p);meta=json.loads((HERE/f'{mode}-{unit}-load.json').read_text(encoding='utf8'))
            groups=collections.defaultdict(list)
            for x in a:groups[x['source']+'/'+x['condition']].append(x)
            result['screen'][mode+'/'+unit]={'attempted':len(a),'planned':meta['planned'],'unrun':meta['unrun'],
                'timeouts':sum(x['timeout'] for x in a),'stopped':meta['stopped'],
                'completed_engine_ms':quantiles([x['engine_ns']/1e6 for x in a if not x['timeout']]),
                'groups':{k:{'n':len(v),'hits':sum(x['hit'] for x in v)} for k,v in groups.items()}}
    for role in ['development','holdout']:
        for command in ['completion','probes']:
            stem=f'{command}-{role}'
            a=read(OLD/f'kazuma-{stem}.jsonl.gz');b=read(HERE/f'kazuma-indexed-{stem}.jsonl.gz')
            assert len(a)==len(b)
            changes=[i for i,(x,y) in enumerate(zip(a,b)) if behavior(x)!=behavior(y)]
            result['parity'][stem]={'rows':len(a),'changed':len(changes)}
            result['parity'][stem]['changed_completion_or_hit_outcome']=sum(any(x.get(k)!=y.get(k) for k in ['success','initial_hit','recovered','counts','committed']) for x,y in zip(a,b))
    a=read(OLD/'kazuma-ajimee-development.jsonl.gz');b=read(HERE/'kazuma-indexed-ajimee-development.jsonl.gz')
    assert a==b
    result['parity']['ajimee']={'rows':len(a),'changed':0}
    for unit in ['clause','word']:
        a=read(HERE/f'cps-{unit}.jsonl.gz');b=read(HERE/f'indexed-cps-{unit}.jsonl.gz')
        assert len(a)==len(b)
        result['parity']['screen-'+unit]={'rows':len(a),'changed':sum(x['choices']!=y['choices'] for x,y in zip(a,b))}
        for mode in ['lexical','lexical-fullprefix']:
            a=read(HERE/f'{mode}-{unit}.jsonl.gz');b=read(HERE/f'indexed-{mode}-{unit}.jsonl.gz')
            compared=0
            for x,y in zip(a,b):
                if not x['timeout'] and not y['timeout']:assert x['choices']==y['choices'];compared+=1
            result['parity'][mode+'-'+unit]={'completed_compared':compared,'changed':0,'unavailable_not_counted':len(a)-compared}
    for mode in ['kazuma','kazuma-indexed']:
        result['performance'][mode]={}
        for p in [1,2,3]:
            path=HERE/f'{mode}-perf-{p}.jsonl.gz'
            if not path.exists():continue
            a=read(path)
            result['performance'][mode][str(p)]={cycle:{'engine_ms':quantiles([x['engine_ns']/1e6 for x in a if x['cycle']==cycle]),
                'wall_ms':quantiles([x['wall_ns']/1e6 for x in a if x['cycle']==cycle])} for cycle in ['first','repeat']}
    dump(HERE/'summary.json',result)
    print('Exact behavior parity:',result['parity'])
    for k,v in result['performance'].items():
        for p,t in v.items():print(k,p,t['repeat']['engine_ms'])
    changes=sum(v.get('changed',0) for v in result['parity'].values())
    if changes:raise SystemExit('FAIL exact candidate-order parity: '+str(changes)+' changed rows. Experiment must not be integrated.')

if __name__=='__main__':main()
