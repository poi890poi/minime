"""Aggregate timestamp-only diagnostic decoder requests; never score missing work as fast."""
import argparse,collections,csv,hashlib,json,math,statistics
from pathlib import Path
from report_touch_latency import validate_workload


def distribution(values):
    values=sorted(v/1e6 for v in values)
    if not values:return dict(observed=0)
    return dict(observed=len(values),mean_ms=statistics.mean(values),p50_ms=statistics.median(values),
                p95_ms=values[math.ceil(.95*len(values))-1],p99_ms=values[math.ceil(.99*len(values))-1],max_ms=max(values))


def analyze(rows,delay_ns=8_000_000,contiguous=True):
    counts=collections.Counter();values=collections.defaultdict(list)
    previous=0
    for index,raw in enumerate(rows,1):
        r={k:int(v) for k,v in raw.items()}
        if r['id']<=previous or (contiguous and r['id']!=index):raise ValueError('Nonconsecutive diagnostic request identity')
        previous=r['id']
        counts['requests']+=1
        requested,scheduled,worker,providers,posted,entered,finished,cancel=(r[k+'_ns'] for k in
            ('requested','scheduled','worker','providers','posted','entered','finished','cancel'))
        if not 0<requested<=scheduled:raise ValueError('Invalid schedule timestamps')
        if cancel and cancel<requested:raise ValueError('Cancellation precedes request')
        terminal=r['terminal']
        if terminal not in range(5):raise ValueError('Unknown terminal state')
        if not worker:
            if any((providers,posted,entered,finished,terminal)):raise ValueError('Work completed without starting')
            counts['cancelled_before_worker' if cancel else 'not_started_unresolved']+=1
            continue
        counts['worker_started']+=1
        if worker<scheduled+delay_ns:raise ValueError('Worker starts before requested deadline')
        values['scheduled_wait'].append(worker-scheduled)
        values['schedule_deadline_overshoot'].append(worker-scheduled-delay_ns)
        if terminal==4:
            if finished<worker:raise ValueError('Failure precedes worker start')
            counts['failed']+=1
            continue
        if not providers:
            counts['worker_unfinished']+=1
            if any((posted,entered,finished,terminal)):raise ValueError('Delivery before providers finished')
            continue
        if providers<worker:raise ValueError('Invalid provider chronology')
        values['provider_work'].append(providers-worker)
        if not posted:
            if terminal!=2 or entered or finished:raise ValueError('Invalid cancelled pipeline state')
            counts['cancelled_pipeline']+=1
            continue
        if posted<providers:raise ValueError('Post precedes provider completion')
        counts['posted']+=1;values['before_post'].append(posted-providers)
        if not entered:
            if finished or terminal:raise ValueError('Callback finished without entry')
            counts['posted_unresolved']+=1
            continue
        if entered<posted:raise ValueError('Callback precedes post')
        counts['entered']+=1;values['main_queue'].append(entered-posted)
        if finished<entered or terminal not in (1,3):raise ValueError('Invalid callback completion')
        counts['accepted' if terminal==1 else 'stale_delivery']+=1
        values['callback'].append(finished-entered)
        values['request_to_main'].append(entered-requested)
        values['request_to_finish'].append(finished-requested)
        if terminal==1:
            for key,value in [('accepted_scheduled_wait',worker-scheduled),('accepted_provider_work',providers-worker),
                              ('accepted_main_queue',entered-posted),('accepted_callback',finished-entered)]:
                values[key].append(value)
    return dict(counts=dict(counts),stages={k:distribution(v) for k,v in values.items()})


def correlate(touches,requests):
    groups=collections.defaultdict(list);coverage=collections.defaultdict(collections.Counter);used=set()
    for index,touch in enumerate(touches):
        if touch['action']!='key':continue
        start=int(touch['up_ns']);end=int(touches[index+1]['up_ns']) if index+1<len(touches) else float('inf')
        keys=[touch['mode']+'/'+touch['interval_ms'],touch['mode']+'/'+touch['interval_ms']+'/'+touch['source']+'/'+touch['genre']+'/'+touch['condition']]
        matching=[r for r in requests if start<=int(r['requested_ns'])<end]
        state='unique_request' if len(matching)==1 else 'unmatched' if not matching else 'ambiguous'
        for key in keys:
            coverage[key]['letters']+=1;coverage[key][state]+=1
            if state=='unique_request':groups[key].append(matching[0])
        if state=='unique_request':
            identity=int(matching[0]['id'])
            if identity in used:raise ValueError('Request attributed to multiple letter windows')
            used.add(identity)
    return dict(requests_not_uniquely_attributed=len(requests)-len(used),
                groups={key:dict(attribution=dict(coverage[key]),**analyze(groups[key],contiguous=False)) for key in coverage})


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source',type=Path);parser.add_argument('output',type=Path)
    parser.add_argument('--touch',type=Path);parser.add_argument('--mode');parser.add_argument('--shard',type=int,default=0)
    args=parser.parse_args()
    with args.source.open(encoding='utf-8') as stream:rows=list(csv.DictReader(stream,delimiter='\t'))
    result=dict(scope='Instrumented synthetic requests only. Schedule overshoot includes worker availability and timer dispatch. Main-queue includes Handler enqueue overhead. Do not add percentiles.',
                source_sha256=hashlib.sha256(args.source.read_bytes()).hexdigest(),schedule_ms=8,**analyze(rows))
    if args.touch:
        if not args.mode:parser.error('--mode is required with --touch')
        with args.touch.open(encoding='utf-8') as stream:touches=list(csv.DictReader(stream,delimiter='\t'))
        corpus=Path(__file__).resolve().parents[1]/'docs/release-hardening/language-timing/inputs.tsv'
        with corpus.open(encoding='utf-8') as stream:inventory=list(csv.DictReader(stream,delimiter='\t'))
        result['workload']=validate_workload(touches,inventory,args.mode,args.shard)
        result['touch_sha256']=hashlib.sha256(args.touch.read_bytes()).hexdigest()
        result['corpus_sha256']=hashlib.sha256(corpus.read_bytes()).hexdigest()
        result['correlation']=correlate(touches,rows)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
