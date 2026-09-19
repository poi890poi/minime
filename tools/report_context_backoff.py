"""Paired conditional-ranking report; labels never enter runtime scoring."""
import argparse,collections,csv,gzip,hashlib,itertools,json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('before',type=Path);p.add_argument('after',type=Path);p.add_argument('output',type=Path);a=p.parse_args();a.output.mkdir(exist_ok=True,parents=True)
groups=collections.defaultdict(collections.Counter);changes=[];counts=collections.Counter();times=[{},{}]
with a.before.open(encoding='utf-8') as before,a.after.open(encoding='utf-8') as after:
    for old,new in itertools.zip_longest(csv.DictReader(before,delimiter='\t'),csv.DictReader(after,delimiter='\t')):
        assert old is not None and new is not None
        assert all(old[k]==new[k] for k in ('genre','id','condition','context','raw','target'))
        left,right=old['outputs'].split('|'),new['outputs'].split('|')
        counts['episodes']+=1;counts['changed_order']+=left!=right
        counts['candidate_or_span_changes']+=collections.Counter(left)!=collections.Counter(right)
        ranks=[int(old['rank']),int(new['rank'])]
        for group in [old['genre'],old['genre']+'/'+old['condition']]:
            c=groups[group];c['episodes']+=1
            for n in [1,5,8]:
                x,y=[0<r<=n for r in ranks]
                c[f'before_top{n}']+=x;c[f'after_top{n}']+=y;c[f'top{n}_gained']+=not x and y;c[f'top{n}_lost']+=x and not y
            c['target_absent']+=ranks[0]==0;c['before_rank_sum_when_present']+=ranks[0];c['after_rank_sum_when_present']+=ranks[1]
        for i,row in enumerate([old,new]):times[i][(row['context'],row['raw'])]=int(row['lookup_ns'])
        if ranks[0]!=ranks[1]:changes.append({k:old[k] for k in ['genre','id','condition','context','raw','target']}|{'before_rank':ranks[0],'after_rank':ranks[1],'before':left[:8],'after':right[:8]})
def latency(values):
    v=sorted(values.values());n=len(v)
    return {'unique_queries':n,'mean_ms':sum(v)/n/1e6,'p50_ms':v[n//2]/1e6,'p95_ms':v[n*95//100]/1e6,'max_ms':v[-1]/1e6}
summary={'scope':'Conditional source-covered dictionary ranking, reused data; off-target is not a nonsense label. No raw recovery row or UI promotions.',
         'counts':counts,'groups':groups,'lookup_timing_before':latency(times[0]),'lookup_timing_after':latency(times[1]),
         'hashes':{str(path):hashlib.sha256(path.read_bytes()).hexdigest() for path in [a.before,a.after]}}
(a.output/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(a.output/'changes.jsonl.gz').write_bytes(gzip.compress(('\n'.join(json.dumps(r,ensure_ascii=False) for r in changes)+'\n').encode(),mtime=0))
print(json.dumps({'counts':counts,'genres':{g:c for g,c in groups.items() if '/' not in g},'timing':[summary['lookup_timing_before'],summary['lookup_timing_after']]}))
assert counts['candidate_or_span_changes']==0
