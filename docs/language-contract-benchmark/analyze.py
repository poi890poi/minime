"""Post-run scoring only. No targets or aggregate outputs feed the variants."""
from pathlib import Path
import csv,gzip,json,collections,math,hashlib,platform
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'docs/language-contract-benchmark'
manifest=json.loads((OUT/'manifest.json').read_text(encoding='utf8'))
inputs=list(csv.reader((OUT/'inputs.tsv').open(encoding='utf8'),delimiter='\t'))
def read(path):
    op=gzip.open if str(path).endswith('.gz') else open
    with op(path,'rt',encoding='utf8',newline='') as f:return list(csv.DictReader(f,delimiter='\t'))
def stats(vals,scale=1):
    a=sorted(v/scale for v in vals);n=len(a)
    if not a:return {'n':0}
    return {'n':n,'mean':sum(a)/n,'p50':a[math.ceil(n*.5)-1],'p95':a[math.ceil(n*.95)-1],
      'p99':a[math.ceil(n*.99)-1],'max':a[-1]}
def flags(r):
    rank=int(r['suggestion_rank']);return {'default':r['default_hit']=='true','top3':0<rank<=3,'top8':0<rank<=8,'suggested':rank>0,'recoverable':int(r['rank_including_raw'])>=0}
report={'baseline':manifest['revision'],'conditions':len(inputs),'coverage':{},'comparisons':{},'mechanics':{},'continuation':{},'latency_ms':{},'working_set':{}}
baseline=None
diffrows=[]
for v in manifest['variants']:
    file=OUT/(v+'-coverage.tsv.gz')
    if not file.exists():continue
    try:rows=read(file)
    except EOFError:continue
    current={(r['row'],r['mode']):r for r in rows}
    if v=='baseline':baseline=current
    groups=collections.defaultdict(collections.Counter);deltas=collections.defaultdict(collections.Counter)
    for key,r in current.items():
        p=inputs[int(r['row'])];group='/'.join([p[0],r['mode'],p[3]]);g=groups[group];g['n']+=1;f=flags(r);g.update({k:int(value) for k,value in f.items()});g['no_alternatives']+=int(r['count'])<=1
        if baseline is not None:
            b=baseline[key];bf=flags(b);d=deltas[group];d['n']+=1;d['order_changed']+=r['signature']!=b['signature'];d['default_changed']+=r['default_text']!=b['default_text']
            for name in f:
                d[name+'_gain']+=f[name] and not bf[name];d[name+'_loss']+=bf[name] and not f[name]
            if v!='baseline' and (r['signature']!=b['signature'] or r['default_text']!=b['default_text']):
                diffrows.append([v,*key,p[0],p[3],p[4],p[5],b['default_text'],r['default_text'],b['suggestion_rank'],r['suggestion_rank']])
    report['coverage'][v]={k:dict(g) for k,g in groups.items()};report['comparisons'][v]={k:dict(g) for k,g in deltas.items()}
    f=OUT/(v+'-acceptance.tsv.gz')
    if f.exists():
        groups=collections.defaultdict(collections.Counter)
        for r in read(f):
            g=groups['/'.join([r['condition'],r['action'],'private' if r['private']=='true' else 'normal'])]
            g['n']+=1;g['available']+=r['available']=='true';g['pass']+=r['suffix_ok']=='true';g['focus_learning']+=int(r['focus_learning']);g['other_learning']+=int(r['other_learning'])
        report['mechanics'][v]={k:dict(g) for k,g in groups.items()}
    f=OUT/(v+'-continuation.tsv.gz')
    if f.exists():
        groups=collections.defaultdict(collections.Counter)
        for r in read(f):
            p=inputs[int(r['row'])];g=groups['/'.join([p[0],r['mode'],'private' if r['private']=='true' else 'normal'])]
            g['n']+=1;g['provider_nonempty']+=int(r['provider_count'])>0;g['idle_nonempty']+=int(r['idle_count'])>0;g['target_top8']+=0<=int(r['target_rank'])<8;g['learning_calls']+=int(r['learning_calls'])
        report['continuation'][v]={k:dict(g) for k,g in groups.items()}
    report['latency_ms'][v]={}
    for f in sorted(OUT.glob(v+'-latency-*.tsv.gz')):
        groups=collections.defaultdict(list)
        for r in read(f):groups[r['mode']+'/'+r['phase']].append(r)
        report['latency_ms'][v][f.name]={k:{field:stats([int(r[field]) for r in group],1e6) for field in ['total_ns','core_ns','native_ipc_ns','addon_ns','apply_and_raw_ns']} for k,group in groups.items()}
for f in sorted(OUT.glob('working-set-*.tsv')):
    rows=read(f);groups=collections.defaultdict(list)
    for r in rows:groups[r['phase']].append(r)
    report['working_set'][f.stem]={k:{'steps':len(rs),'misses':sum(r['hit']=='false' for r in rs),'load_ms':stats([int(r['load_ns']) for r in rs],1e6),'peak_addon_mib':max(int(r['retained_addon_heap_bytes']) for r in rs)/2**20,'last_addon_mib':int(rs[-1]['retained_addon_heap_bytes'])/2**20} for k,rs in groups.items()}
report['load']={f.stem:json.loads(f.read_text(encoding='utf8')) for f in OUT.glob('*-load-*.json')}
(OUT/'results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
with gzip.open(OUT/'changed-outputs.tsv.gz','wt',encoding='utf8',newline='') as f:
    w=csv.writer(f,delimiter='\t');w.writerow(['variant','row','mode','group','condition','raw','target','baseline_default','variant_default','baseline_rank','variant_rank']);w.writerows(diffrows)
print('scored',list(report['coverage']),'changed outputs',len(diffrows))
for v,groups in report['comparisons'].items():
    c=collections.Counter()
    for g in groups.values():c.update(g)
    print(v,dict(c))
