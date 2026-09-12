"""Measure true native Sentence reconstruction; labels never enter the provider."""
from pathlib import Path
from collections import defaultdict,Counter
import argparse,json,gzip,subprocess,os,hashlib,statistics,math
ROOT=Path(__file__).resolve().parent.parent;OUT=ROOT/'docs/construction-confidence'
parser=argparse.ArgumentParser();parser.add_argument('--role',choices=['development','reserved'],required=True)
parser.add_argument('--output-dir',type=Path,default=OUT)
parser.add_argument('--probe',type=Path,default=ROOT/'artifacts/native-metadata/probe.exe')
parser.add_argument('--require-no-assembly',action='store_true');args=parser.parse_args()
inputs=[r for r in json.loads(gzip.decompress((OUT/'inputs.json.gz').read_bytes())) if r['role']==args.role]
corpus_hash=hashlib.sha256((OUT/'inputs.json.gz').read_bytes()).hexdigest()
OUT=args.output_dir;OUT.mkdir(parents=True,exist_ok=True)
env=os.environ.copy();env['PATH']=str(ROOT/'.tools/rime-evaluation/msvc/dist/lib')+os.pathsep+env['PATH']
user=ROOT/('artifacts/native-metadata/user-'+args.role);user.mkdir(exist_ok=True)
p=subprocess.Popen([str(args.probe.resolve()),str(ROOT/'app/src/main/rimeAssets/rime'),str(user)],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,encoding='utf-8',env=env)
assert p.stdout.readline().strip()=='READY 1.16.1'
results=[]
try:
    for i,row in enumerate(inputs):
        p.stdin.write(row['raw']+'\n');p.stdin.flush();choices=[]
        while True:
            line=p.stdout.readline()
            if not line:raise RuntimeError('Native evaluator ended; C++/C API consistency may have failed')
            if line.startswith('END\t'):micros=int(line.split('\t')[1]);break
            end,kind,quality,weight,parts,text,components=line.rstrip('\n').split('\t')
            choices.append(dict(end=int(end),kind=kind,quality=float(quality),weight=float(weight),parts=int(parts),text=text,components=components.rstrip('|').split('|') if components else []))
        sentence=next((c for c in choices if c['kind']=='sentence'),None)
        if args.require_no_assembly and sentence:raise AssertionError('Native assembly still enabled: '+row['raw'])
        if sentence:
            sentence['mean_entry_log_weight']=sentence['weight']/sentence['parts']+13.815510557964274
            sentence['hit']=sentence['text']==row['target']
            sentence['target_prefix']=row['target'].startswith(sentence['text'])
        results.append(dict(**row,query_us=micros,choices=choices,sentence=sentence))
        if (i+1)%1000==0:print('Evaluated',i+1,'/',len(inputs),flush=True)
finally:
    p.stdin.close();assert p.wait(timeout=15)==0
with (OUT/(args.role+'-native.jsonl.gz')).open('wb') as f:
    with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as z:
        for row in results:z.write((json.dumps(row,ensure_ascii=False,separators=(',',':'))+'\n').encode())
groups={}
for condition in dict.fromkeys(r['condition'] for r in results):
    rows=[r for r in results if r['condition']==condition];generated=[r['sentence'] for r in rows if r['sentence']]
    bins=[]
    for low,high in [(-100,-14),(-14,-12),(-12,-10),(-10,-8),(-8,-6),(-6,0)]:
        chunk=[c for c in generated if low<=c['mean_entry_log_weight']<high]
        bins.append(dict(low=low,high=high,n=len(chunk),hits=sum(c['hit'] for c in chunk)))
    latency=sorted(r['query_us'] for r in rows)
    groups[condition]=dict(inputs=len(rows),constructed=len(generated),construction_hits=sum(c['hit'] for c in generated),construction_target_prefix=sum(c['target_prefix'] for c in generated),
        top1_reference_hits=sum(bool(r['choices']) and r['choices'][0]['text']==r['target'] for r in rows),
        top8_reference_hits=sum(any(c['text']==r['target'] for c in r['choices'][:8]) for r in rows),
        quality_values=dict(Counter(str(c['quality']) for c in generated)),mean_entry_weight_bins=bins,
        query_us=dict(p50=statistics.median(latency),p95=latency[math.ceil(len(latency)*.95)-1],maximum=max(latency)))
report={'role':args.role,'groups':groups,'corpus_sha256':corpus_hash,
        'model':json.loads((ROOT/'third_party/rime/model.json').read_text()),
        'evaluator_sha256':hashlib.sha256(args.probe.read_bytes()).hexdigest(),
        'limitations':['exact source reconstruction, not semantic judgement','prose only; no natural Mandarin conversation coverage','not Android touch latency','reading coverage excludes ambiguous/unmapped source spans']}
(OUT/(args.role+'-summary.json')).write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
