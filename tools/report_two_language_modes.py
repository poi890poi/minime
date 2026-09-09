"""Reproduce paired mode, source retrieval and timing aggregates from raw runs."""
from pathlib import Path
import collections, csv, gzip, json, math, statistics

ROOT=Path(__file__).resolve().parent.parent
RUN=ROOT/'artifacts/two-language-modes'; OUT=ROOT/'docs/two-language-modes'

def stream(path):
    return gzip.open(path,'rt',encoding='utf-8-sig') if str(path).endswith('.gz') else path.open(encoding='utf-8-sig')

def save(name, data):
    (OUT/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def archive(path):
    with gzip.GzipFile(filename='',mode='wb',fileobj=(OUT/(path.name+'.gz')).open('wb'),mtime=0) as out:out.write(path.read_bytes())

def retrieval(path):
    result=collections.defaultdict(collections.Counter)
    for row in csv.DictReader(stream(path),delimiter='\t'):
        c=result[row['condition']];rank=int(row['rank']);c['n']+=1
        for label,limit in [('top3',3),('top8',8),('available',100000)]: c[label]+=0<rank<=limit
        if 'han' in row:c['paired']+=bool(row['han'])
    return dict(result)

def timings(path, column, divisor):
    groups=collections.defaultdict(list)
    for row in csv.DictReader(stream(path),delimiter='\t'):groups[row['mode']].append(float(row[column])/divisor)
    result={}
    for mode,values in groups.items():
        ordered=sorted(values)
        result[mode]={'n':len(values),'mean_ms':statistics.mean(values),'p50_ms':statistics.median(values),
            'p95_ms':ordered[math.ceil(len(values)*.95)-1],'max_ms':max(values)}
    return result

def main():
    inputs=[s.split('\t') for s in (ROOT/'docs/input-modes/coverage-inputs.tsv').read_text(encoding='utf8').splitlines()]
    results={};outputs={}
    for stage in ['baseline','policy','search','final','pooled','compact','branch','scoped']:
        stats=collections.defaultdict(collections.Counter);rows={}
        for line in stream(RUN/(stage+'.jsonl.gz')):
            row=json.loads(line);p=inputs[row['row']];rows[row['row'],row['mode']]=row
            c=stats[row['mode']+'|'+p[0]+'|'+p[3]];c['n']+=1
            values=[v[0] for v in row['candidates'][1:]];rank=values.index(p[5])+1 if p[5] in values else 100000
            for label,limit in [('top3',3),('top8',8),('available',99999)]:c[label]+=rank<=limit
            c['default']+=row['candidates'][row['preferred']][0]==p[5]
        outputs[stage]=rows;results[stage]=dict(stats)
        (OUT/(stage+'.jsonl.gz')).write_bytes((RUN/(stage+'.jsonl.gz')).read_bytes())
    for stage in ['pooled','compact','branch']:assert outputs[stage]==outputs['final'], 'Representation optimization changed candidate output: '+stage
    for stage in outputs:
        for key,value in outputs['baseline'].items():
            if key[1] in ('chinese','english'):assert outputs[stage][key]==value, (stage,key)
    save('coverage-results.json',results)
    retrievals={}
    for label,path in {
        'taihoa-baseline':RUN/'taihoa-baseline.tsv','taihoa-final':RUN/'taihoa-final.tsv',
        'beginner-baseline':ROOT/'docs/taiwanese-coverage/current-retrieval.tsv.gz','beginner-final':RUN/'beginner-final.tsv',
        'japanese-baseline':ROOT/'docs/japanese-coverage/current-retrieval.tsv.gz','japanese-final':RUN/'japanese-final.tsv'
    }.items():
        retrievals[label]=retrieval(path)
        if path.parent==RUN:archive(path)
    assert sum(c['n'] for c in retrievals['taihoa-final'].values())==103020
    assert sum(c['n'] for c in retrievals['japanese-final'].values())==24996
    save('retrieval-results.json',retrievals)
    perf={}
    for stage in ['baseline','final','pooled','compact','branch','compact-repeat','branch-repeat']:
        path=RUN/('lookup-'+stage+'.tsv');perf['lookup-'+stage]=timings(path,'elapsed_ns',1e6);archive(path)
    for stage,path in [('phone-baseline',ROOT/'docs/japanese-coverage/phone-latency.tsv.gz'),('phone-final',RUN/'phone-latency.tsv')]:
        perf[stage]=timings(path,'last_key_us',1000)
    perf['phone-balanced']=timings(RUN/'phone-balanced-latency.tsv','last_key_us',1000)
    if (RUN/'phone-release-latency.tsv').exists():
        perf['phone-release']=timings(RUN/'phone-release-latency.tsv','last_key_us',1000);archive(RUN/'phone-release-latency.tsv')
    archive(RUN/'phone-latency.tsv');archive(RUN/'phone-balanced-latency.tsv');archive(RUN/'branch-interleaved.tsv');save('performance.json',perf)
    print('PASS mode output parity, independent stages, source counts and per-mode timing aggregates')

if __name__=='__main__':main()
