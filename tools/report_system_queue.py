"""Fail closed on incomplete local Perfetto observations, then account queue time."""
import argparse,bisect,collections,csv,hashlib,json,math,statistics
from pathlib import Path
from system_queue_intervals import partition,union_duration

def distribution(values):
    values=sorted(values)
    if not values:return dict(n=0)
    return dict(n=len(values),mean_ms=sum(values)/len(values)/1e6,
        p50_ms=statistics.median(values)/1e6,p95_ms=values[math.ceil(.95*len(values))-1]/1e6,
        p99_ms=values[math.ceil(.99*len(values))-1]/1e6,max_ms=values[-1]/1e6)

def analyze(rows,requests):
    groups=collections.defaultdict(list)
    for row in rows:groups[row['kind']].append(row)
    main=groups['main'];health=groups['health']
    queued={int(r['id']) for r in requests if int(r['posted_ns'])}
    entered={int(r['id']) for r in requests if int(r['entered_ns'])}
    failures=[];markers={}
    for kind,expected in [('queue',queued),('callback',entered)]:
        selected=groups[kind]
        ids=[int(r['name'].rsplit('.',1)[1]) for r in selected]
        if len(ids)!=len(set(ids)):failures.append(kind+' duplicate IDs')
        if set(ids)!=expected:failures.append(kind+' missing/extra IDs')
        if any(int(r['dur'])<0 for r in selected):failures.append(kind+' incomplete spans')
        markers[kind]={identity:r for identity,r in zip(ids,selected)}
    if len(main)!=1:failures.append('main thread not uniquely identified')
    if health:failures.append('trace loss/error/overrun')
    for identity in markers['queue'].keys() & markers['callback'].keys():
        queue=markers['queue'][identity];callback=markers['callback'][identity]
        if int(callback['ts'])<int(queue['ts'])+int(queue['dur']):failures.append('callback precedes queue completion')
    summary=dict(status='incomplete' if failures else 'complete',failures=failures,
        expected_queues=len(queued),observed_queues=len(groups['queue']),
        expected_callbacks=len(entered),observed_callbacks=len(groups['callback']),health=health)
    if failures:return summary
    states=sorted((int(r['ts']),int(r['ts'])+int(r['dur']),r['name']) for r in groups['state'] if int(r['dur'])>=0)
    for before,after in zip(states,states[1:]):
        if before[1]>after[0]:raise ValueError('Overlapping global thread states')
    starts=[s[0] for s in states]
    frames=[(int(r['ts']),int(r['ts'])+int(r['dur'])) for r in groups['frame'] if int(r['dur'])>=0]
    callbacks=[(int(r['ts']),int(r['ts'])+int(r['dur'])) for r in groups['callback']]
    values=collections.defaultdict(list);totals=collections.Counter();samples=[]
    for identity,row in sorted(markers['queue'].items()):
        start=int(row['ts']);end=start+int(row['dur'])
        current=states[max(0,bisect.bisect_left(starts,start)-1):bisect.bisect_left(starts,end)]
        portions=partition(start,end,current)
        totals.update(portions)
        for key,value in portions.items():values[key].append(value)
        values['queue'].append(end-start)
        frame=union_duration(start,end,frames);callback=union_duration(start,end,callbacks)
        values['frame_wall_overlap'].append(frame);values['callback_wall_overlap'].append(callback)
        samples.append(dict(id=identity,queue_ns=end-start,**portions,frame_wall_ns=frame,callback_wall_ns=callback))
    total=sum(totals.values())
    summary.update(stages={key:distribution(value) for key,value in values.items()},
        duration_weighted_state_shares={key:value/total if total else 0 for key,value in totals.items()},
        accounting='States partition each queued-request interval. Shares use summed request-wait duration; frame/callback wall overlap is separate and may include blocked time.',
        scope='Instrumented synthetic Chinese shard only; sleeping does not prove a barrier. No release latency or cross-app claim.',
        frame_slice_count=len(frames),unknown_request_count=sum(s['unknown']>0 for s in samples))
    return summary

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('trace_csv',type=Path);p.add_argument('queues',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
    with a.trace_csv.open(encoding='utf-8-sig') as f:rows=list(csv.DictReader(f))
    with a.queues.open(encoding='utf-8-sig') as f:requests=list(csv.DictReader(f,delimiter='\t'))
    result=analyze(rows,requests)
    result['inputs']={str(path):hashlib.sha256(path.read_bytes()).hexdigest() for path in (a.trace_csv,a.queues)}
    a.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,indent=2))
    if result['status']!='complete':raise SystemExit(2)

if __name__=='__main__':main()
