"""Summarize raw OS-injected timing telemetry; missing samples stay missing."""
import argparse, csv, gzip, json, math, statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def summary(group, endpoint, origin='up_ns'):
    values = sorted((int(r[endpoint])-int(r[origin]))/1e6 for r in group if int(r[endpoint]))
    result = dict(n=len(group), observed=len(values), unobserved=len(group)-len(values))
    if values:
        assert min(values)>=0
        result.update(mean_ms=statistics.mean(values),p50_ms=statistics.median(values),
                      p95_ms=values[math.ceil(.95*len(values))-1],p99_ms=values[math.ceil(.99*len(values))-1],max_ms=max(values))
    return result


def episode(row):
    return (row['mode'],row['interval_ms'],row.get('query_id') or row['query'])

def validate_workload(rows,corpus,mode,shard):
    """Compare injected actions with the frozen inventory, independently of frames."""
    if shard not in range(4):raise ValueError('Invalid shard')
    positions={};selected=[]
    for q in corpus:
        if q['mode']!=mode:continue
        key=(q['source'],q['condition']);index=positions.get(key,0);positions[key]=index+1
        if index%4==shard:selected.append(q)
    if not selected:raise ValueError('Empty workload')
    fields=['mode','interval_ms','query','expected','action','query_id','source','genre','condition']
    expected=[]
    for interval in ['150','60']:
        for q in selected:
            for n in range(1,len(q['raw'])+2):
                expected.append((mode,interval,q['raw'],q['raw'][:n],'space' if n>len(q['raw']) else 'key',q['id'],q['source'],q['genre'],q['condition']))
    actual=[tuple(r.get(k,'') for k in fields) for r in rows]
    if actual!=expected:raise ValueError('Injected action sequence differs from the frozen mode/shard workload')
    previous_up=0
    for r in rows:
        down,up=int(r['down_ns']),int(r['up_ns'])
        if not previous_up<down<=up:raise ValueError('Invalid/nonmonotonic injected action timestamps')
        previous_up=up
    return dict(mode=mode,shard=shard,queries_per_cadence=len(selected),actions=len(rows),status='exact action sequence matches frozen corpus')

def measurements(group,adjacent):
    keys=[r for r in group if r['action']=='key'];spaces=[r for r in group if r['action']=='space']
    # Use original adjacency, never stitch across intervening queries/strata.
    members={id(r) for r in group}
    deadlines=[(a,b) for a,b in adjacent if id(a) in members and a['action']=='key' and episode(a)==episode(b)]
    intervals=[dict(up_ns=a['up_ns'],next_up_ns=b['up_ns']) for a,b in deadlines]
    on_time=sum(0<int(a['candidate_submit_ns'])<int(b['up_ns']) for a,b in deadlines)
    late=sum(int(a['candidate_submit_ns'])>=int(b['up_ns']) for a,b in deadlines)
    return dict(editor_callback=summary(keys,'editor_callback_ns'),
        editor_submission=summary(keys,'editor_submit_ns'),candidate_submission=summary(keys,'candidate_submit_ns'),
        pressed_submission=summary(keys,'pressed_submit_ns','down_ns'),space_submission=summary(spaces,'editor_submit_ns'),
        injected_key_interval=summary(intervals,'next_up_ns'),
        candidate_deadline=dict(letters=len(deadlines),submitted_before_next_up=on_time,
            observed_submission_at_or_after_next_up=late,unobserved=len(deadlines)-on_time-late))

def report(rows,build):
    groups={};strata={};adjacent=list(zip(rows,rows[1:]))
    for mode in dict.fromkeys(r['mode'] for r in rows):
        for interval in ('150','60'):
            group=[r for r in rows if r['mode']==mode and r['interval_ms']==interval]
            groups[mode+'/'+interval]=measurements(group,adjacent)
    if any(r.get('query_id') for r in rows):
        if not all(all(r.get(k) for k in ['query_id','source','genre','condition']) for r in rows):raise ValueError('Incomplete language timing labels')
        fields=['mode','source','genre','condition','interval_ms']
        for key in dict.fromkeys(tuple(r[k] for k in fields) for r in rows):
            group=[r for r in rows if tuple(r[k] for k in fields)==key]
            strata['/'.join(key)]=dict(labels=dict(zip(fields,key)),queries=len({r['query_id'] for r in group}),**measurements(group,adjacent))
    return dict(baseline=build,samples=len(rows),groups=groups,strata=strata,
        limitations=['injected events, not physical digitizer latency','frame callback delivery, not display presentation',
            'one session, small sample; no acceptance certification','same-process test editor, not Chrome',
            'candidate metrics conditional on observing a fresh matching frame before the next action; unchanged rows and supersession are not distinguished',
            'source conditions independently sampled; latency differences between conditions are not isolated error effects',
            'four Space samples per source/condition/shard; p95/p99 equal the maximum, not stable tail estimates'])

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input',type=Path,default=ROOT/'artifacts/touch-latency-corrected.tsv')
    parser.add_argument('--output',type=Path,default=ROOT/'docs/touch-latency')
    parser.add_argument('--build',default='5bcb90f / 0.7.6')
    parser.add_argument('--workload-mode')
    parser.add_argument('--workload-shard',type=int)
    parser.add_argument('--workload-corpus',type=Path,default=ROOT/'docs/release-hardening/language-timing/inputs.tsv')
    args=parser.parse_args();out=args.output
    with args.input.open(encoding='utf-8') as stream:rows=list(csv.DictReader(stream,delimiter='\t'))
    result=report(rows,args.build)
    if args.workload_mode is not None or args.workload_shard is not None:
        if args.workload_mode is None or args.workload_shard is None:parser.error('Both workload mode and shard are required')
        with args.workload_corpus.open(encoding='utf-8') as f:corpus=list(csv.DictReader(f,delimiter='\t'))
        result['workload_validation']=validate_workload(rows,corpus,args.workload_mode,args.workload_shard)
    out.mkdir(parents=True,exist_ok=True)
    sources=[(args.input,'samples.tsv.gz')]
    if args.input==ROOT/'artifacts/touch-latency-corrected.tsv':sources.append((ROOT/'artifacts/touch-latency-rejected-ondraw.tsv','rejected-ondraw.tsv.gz'))
    for source,target in sources:
        with (out/target).open('wb') as f:
            with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as compressed:compressed.write(source.read_bytes())
    (out/'summary.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    for key,value in result['groups'].items():print(key,'raw p95',round(value['editor_submission'].get('p95_ms',float('nan')),2),'candidates',value['candidate_submission'])

if __name__=='__main__':main()
