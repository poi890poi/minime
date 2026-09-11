"""Recompute consumed regression and phone summaries, preserving failed runs."""
from pathlib import Path
import gzip,json,statistics
HERE=Path(__file__).resolve().parent
def read(path):return [json.loads(x) for x in gzip.open(path,'rt',encoding='utf8')]
def dump(name,value):(HERE/name).write_text(json.dumps(value,indent=2)+'\n',encoding='utf8')
def clean(x,skip):
    if isinstance(x,dict):return {k:clean(v,skip) for k,v in x.items() if k not in skip}
    if isinstance(x,list):return [clean(v,skip) for v in x]
    return x
def quantiles(a):
    a=sorted(a)
    return dict(n=len(a),mean=statistics.mean(a),p50=statistics.median(a),p95=a[int(len(a)*.95)],p99=a[int(len(a)*.99)],max=max(a))
def main():
    initial=read(HERE/'stream-initial.jsonl.gz');final=read(HERE/'stream.jsonl.gz')
    assert [clean(r,('expected',)) for r in initial]==[clean(r,('expected',)) for r in final], 'Typed stream changed'
    result={}
    for stem in ['completion-development','probes-development','completion-holdout','probes-holdout','ajimee-development']:
        a=read(HERE.parent/'japanese-determinism'/f'kazuma-indexed-stable-{stem}.jsonl.gz');b=read(HERE/f'kazuma-portable-{stem}.jsonl.gz')
        assert len(a)==len(b)
        result[stem]={'rows':len(a),'candidate_list_changes':sum(clean(x,('engine_ns','wall_ns'))!=clean(y,('engine_ns','wall_ns')) for x,y in zip(a,b)),
            'beyond_candidate_lists':sum(clean(x,('engine_ns','wall_ns','candidates'))!=clean(y,('engine_ns','wall_ns','candidates')) for x,y in zip(a,b))}
    dump('portable-regression.json',result)
    rows=read(HERE/'phone-final.jsonl.gz');native=[r for r in rows if r['kind']=='native']
    assert len(native)==3933 and all(r['equal'] for r in native)
    summary={}
    for kind in ['native','pipeline']:
        summary[kind]={};summary[kind+'_by_source']={}
        fields=['native_ns','jni_ns'] if kind=='native' else ['elapsed_ns','last_key_ns','dispatch_ns','addon_and_conversion_ns']
        for cycle in range(3):
            samples=[r for r in rows if r['kind']==kind and r['cycle']==cycle]
            summary[kind][str(cycle)]={k:quantiles([r[k]/1e6 for r in samples]) for k in fields}
            summary[kind+'_by_source'][str(cycle)]={source:{k:quantiles([r[k]/1e6 for r in samples if r['id'].split(':')[0]==source]) for k in fields}
                for source in sorted(set(r['id'].split(':')[0] for r in samples))}
    dump('phone-summary.json',summary)
    assert not any(r['beyond_candidate_lists'] for r in result.values())
    admission=next(r for r in rows if r['kind']=='summary')
    print('PASS 18730 regression outcomes; 3933 ordered phone comparisons; typed inputs unchanged')
    print('Admission:',admission)
    if not admission['pipeline_gate']:print('HOLD: whole-burst pipeline latency gate remains failed; no production activation')
if __name__=='__main__':main()
