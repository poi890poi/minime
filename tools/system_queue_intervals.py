"""Strict interval accounting for diagnostic traces (nanoseconds throughout)."""
from collections import Counter
from collections import defaultdict
import heapq

def union_duration(start,end,intervals):
    if end<start:raise ValueError('Reversed interval')
    covered=0;edge=start
    for left,right in sorted(intervals):
        if right<left:raise ValueError('Reversed source interval')
        left=max(left,start,edge);right=min(right,end)
        if right>left:covered+=right-left;edge=right
    return covered

def partition(start,end,states):
    """Clip disjoint thread-state spans; absent coverage is explicitly unknown."""
    if end<start:raise ValueError('Reversed request interval')
    counts=Counter(running=0,runnable=0,sleeping=0,other=0,unknown=0)
    edge=start
    for left,right,state in sorted(states):
        if right<left:raise ValueError('Reversed state span')
        left=max(start,left);right=min(end,right)
        if right<=left:continue
        if left<edge:raise ValueError('Overlapping thread-state spans')
        counts['unknown']+=left-edge
        kind='running' if state=='Running' else 'runnable' if state in ('R','R+') else 'sleeping' if state=='S' else 'unknown' if state in (None,'','[NULL]') else 'other'
        counts[kind]+=right-left;edge=right
    counts['unknown']+=end-edge
    if sum(counts.values())!=end-start:raise ValueError('Interval accounting mismatch')
    return dict(counts)

def exclusive_work(queues,slices,states):
    """Attribute each request-wait instant to the deepest traced main-thread slice."""
    events=[];names={}
    for identity,depth,start,end,name in slices:
        if end<start:raise ValueError('Reversed slice')
        if end==start:continue
        if identity in names:raise ValueError('Duplicate slice identity')
        names[identity]=name;events.extend([(start,3,identity,depth),(end,0,identity,depth)])
    for start,end,state in states:
        if end<start:raise ValueError('Reversed state')
        if end>start:events.extend([(start,4,state,0),(end,1,'',0)])
    for start,end in queues:
        if end<start:raise ValueError('Reversed queue')
        if end>start:events.extend([(start,5,0,0),(end,2,0,0)])
    events.sort();active=set();depths=set();heap=[];pending=0;state='unknown';state_active=False
    totals=Counter();by_state=defaultdict(Counter)
    if not events:return totals,by_state
    last=events[0][0]
    for at,kind,identity,depth in events:
        while heap and heap[0][1] not in active:heapq.heappop(heap)
        if at>last and pending:
            name=names[heap[0][1]] if heap else '[untraced]'
            duration=(at-last)*pending;totals[name]+=duration;by_state[name][state]+=duration
        if kind==0:active.remove(identity);depths.remove(depth)
        elif kind==1:state='unknown';state_active=False
        elif kind==2:pending-=1
        elif kind==3:
            if depth in depths:raise ValueError('Overlapping slices at the same depth')
            active.add(identity);depths.add(depth);heapq.heappush(heap,(-depth,identity))
        elif kind==4:
            if state_active:raise ValueError('Overlapping states')
            state=identity;state_active=True
        else:pending+=1
        if pending<0:raise ValueError('Unbalanced queue boundaries')
        last=at
    if pending or sum(totals.values())!=sum(end-start for start,end in queues):raise ValueError('Exclusive accounting mismatch')
    return totals,by_state
