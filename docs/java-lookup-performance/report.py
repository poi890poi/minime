import csv, gzip, hashlib, json, statistics
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'docs/java-lookup-performance'
RAW=ROOT/'artifacts/java-lookup'

def metrics(values):
    a=sorted(values)
    return {'n':len(a),'mean':statistics.mean(a),'p50':a[len(a)//2],
            'p95':a[min(len(a)-1,int(len(a)*.95))],
            'p99':a[min(len(a)-1,int(len(a)*.99))],'max':a[-1]}

def load(name):
    with (RAW/(name+'.tsv')).open(encoding='utf-8') as f:
        return list(csv.DictReader(f,delimiter='\t'))

baseline=load('baseline-1')
reference={(r['pass'],r['row']):(r['count'],r['digest']) for r in baseline}
assert len(reference)==3*json.loads((OUT/'manifest.json').read_text())['rows']
result={'units':{'latency':'milliseconds','allocation':'bytes per lookup'},
        'profiling_run':'baseline-1 (timing excluded from speedup comparison)',
        'runs':{},'output_comparisons':{}}
for path in sorted(RAW.glob('*-[123].tsv')):
    name=path.stem;rows=load(name)
    assert len(rows)==len(reference),(name,len(rows))
    differences=sum(reference[(r['pass'],r['row'])]!=(r['count'],r['digest']) for r in rows)
    result['output_comparisons'][name]={'records':len(rows),'differences':differences}
    assert differences==0,(name,differences)
    groups={}
    for r in rows:
        for category in ['pack','group','condition']:
            key=f"{category}/{r[category]}/{'first' if r['pass']=='0' else 'warm'}"
            groups.setdefault(key,[]).append(r)
    result['runs'][name]={k:{'latency':metrics([int(r['ns'])/1e6 for r in v]),
        'allocation':metrics([int(r['allocated_bytes']) for r in v]),
        'empty_outputs':sum(r['count']=='0' for r in v)} for k,v in sorted(groups.items())}
    with (OUT/(name+'.tsv.gz')).open('wb') as f:
        f.write(gzip.compress(path.read_bytes(),mtime=0))
(OUT/'results.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
for name,run in result['runs'].items():
    print(name, {k: {'p95_ms':round(v['latency']['p95'],3),'mean_bytes':round(v['allocation']['mean'])} for k,v in run.items() if k.startswith('pack/') and k.endswith('/warm')})
print(result['output_comparisons'])
