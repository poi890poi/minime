"""Compare frozen per-episode prefix evidence with explicit denominators."""
import argparse,collections,csv,gzip,hashlib,json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('before',type=Path);p.add_argument('after',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
def load(path):
    with path.open(encoding='utf-8') as f:return list(csv.DictReader(f,delimiter='\t'))
before,after=load(a.before),load(a.after)
assert len(before)==len(after)
def stats(rows):
    return dict(episodes=len(rows),available=sum(int(r['composition_rank'])>0 for r in rows),
                first8=sum(0<int(r['composition_rank'])<=8 for r in rows),
                leading=sum(int(r['composition_rank'])==1 for r in rows))
groups=collections.defaultdict(lambda:([],[]));lost=[];space=[];whole=[];glyphs=[]
for x,y in zip(before,after):
    assert all(x[k]==y[k] for k in ('role','reading','target','condition','raw','consumed','suffix'))
    for group in ('all',x['condition'],'frequency-band/'+x['frequency_band']):groups[group][0].append(x);groups[group][1].append(y)
    key={k:x[k] for k in ('reading','target','condition','raw')}
    if int(x['composition_rank'])>0 and int(y['composition_rank'])==0:lost.append(key)
    if x['space']!=y['space']:space.append(key)
    if x['whole_choices']!=y['whole_choices']:whole.append(key)
    if x['first_glyphs']!=y['first_glyphs']:glyphs.append(key)
def timing(rows):
    v=sorted(int(r['lookup_ns'])/1e6 for r in rows)
    return dict(n=len(v),mean=sum(v)/len(v),p50=v[len(v)//2],p95=v[int(len(v)*.95)],p99=v[int(len(v)*.99)],max=v[-1])
report=dict(groups={g:dict(before=stats(x),after=stats(y)) for g,(x,y) in sorted(groups.items())},lost_targets=lost,
            changed_space=space,changed_whole_choices=whole,changed_first_two_glyphs=glyphs,
            before_lookup_ms=timing(before),after_lookup_ms=timing(after),
            hashes={str(path):hashlib.sha256(path.read_bytes()).hexdigest() for path in (a.before,a.after)})
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode())
print(json.dumps(dict(all=report['groups']['all'],lost=len(lost),space=len(space),whole=len(whole),glyphs=len(glyphs),timing=report['after_lookup_ms'])))
