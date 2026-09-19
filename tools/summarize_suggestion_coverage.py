"""Report recall by condition; an assertion count is not language accuracy."""
import collections,csv,gzip,hashlib,json,sys
from pathlib import Path
root=Path(__file__).resolve().parents[1];art=root/'artifacts/suggestion-coverage';out=root/'docs/suggestion-coverage'
def load(name):
    with (art/name).open(encoding='utf-8') as f:return list(csv.DictReader(f,delimiter='\t'))
def times(rows):
    q={r['raw']:int(r['lookup_ns'])/1e6 for r in rows};v=sorted(q.values())
    return dict(queries=len(v),p50=v[len(v)//2],p95=v[int(len(v)*.95)],p99=v[int(len(v)*.99)],maximum=v[-1])
def compare(a,b):
    assert len(a)==len(b), 'Missing retrieval rows'
    groups=collections.defaultdict(lambda:dict(attempts=0,before_available=0,after_available=0,before_first8=0,after_first8=0))
    lost=[];changed=set()
    for x,y in zip(a,b):
        assert all(x[k]==y[k] for k in ['role','reading','target','condition','raw'])
        old,new=int(x['target_rank']),int(y['target_rank'])
        for key in ['all',x['kind']+'/'+x['condition']]:
            g=groups[key];g['attempts']+=1;g['before_available']+=old>0;g['after_available']+=new>0;g['before_first8']+=0<old<=8;g['after_first8']+=0<new<=8
        if old and not new:lost.append({k:x[k] for k in ['reading','target','raw','condition']})
        if x['top8']!=y['top8']:changed.add(x['raw'])
    return dict(groups=dict(groups),lost_targets=lost,changed_first8_queries=len(changed),before_lookup_ms=times(a),after_lookup_ms=times(b))
reports={}
prefix_mode='--prefix' in sys.argv
for role in ['development','reserved']:
    before=load(role+('-capacity-128.tsv' if prefix_mode else '-before.tsv'))
    for capacity in (['prefix'] if prefix_mode else ([64,128] if role=='development' else [128])):
        after=load(f'{role}-prefix.tsv' if prefix_mode else f'{role}-capacity-{capacity}.tsv');reports[f'{role}-{capacity}']=compare(before,after)
oracle=load('development-before.tsv.oracle.tsv');reports['oracle']=dict(queries=242,eligible_targets=len(oracle),missing=sum(r['returned']=='false' for r in oracle),scope='Independent exhaustive unit matching; eligible means top 24 overall or top six of a glyph length.')
(out/('prefix-results.json' if prefix_mode else 'capacity-results.json')).write_bytes((json.dumps(reports,ensure_ascii=False,indent=2)+'\n').encode())
evidence=out/'evidence';evidence.mkdir(exist_ok=True)
for name in (['development-prefix.tsv','reserved-prefix.tsv'] if prefix_mode else ['development-before.tsv','development-capacity-64.tsv','development-capacity-128.tsv','reserved-before.tsv','reserved-capacity-128.tsv','development-before.tsv.oracle.tsv']):
    raw=(art/name).read_bytes();(evidence/(name+'.gz')).write_bytes(gzip.compress(raw,mtime=0))
for key,v in reports.items():
    if key!='oracle':print(key,'lost',len(v['lost_targets']),'changed first8',v['changed_first8_queries'],'initials',v['groups']['phrase/initials'])
