"""Compare consumed broad regression corpora before/after whole-input relevance."""
from pathlib import Path
from collections import defaultdict
import csv,json,gzip,argparse,math
root=Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser();p.add_argument('--role',choices=['development','reserved'],required=True)
p.add_argument('--after-tag',default='quality-after');p.add_argument('--output-dir',type=Path,default=root/'docs/whole-input-quality');args=p.parse_args()
out=args.output_dir;out.mkdir(parents=True,exist_ok=True)
tables={}
for variant in ('before','after'):
    tag='quality-before' if variant=='before' else args.after_tag
    path=root/'artifacts/native-metadata'/f'{args.role}-core-{tag}.tsv'
    rows=list(csv.reader(path.open(encoding='utf-8'),delimiter='\t'))
    assert len(rows)==(10200 if args.role=='development' else 12240)
    tables[variant]={tuple(r[:3]):r for r in rows}
    (out/(path.name+'.gz')).write_bytes(gzip.compress(path.read_bytes(),mtime=0))
assert tables['before'].keys()==tables['after'].keys()
labels=[r for r in json.loads(gzip.decompress((root/'docs/construction-confidence/inputs.json.gz').read_bytes())) if r['role']==args.role]
groups=defaultdict(lambda:defaultdict(dict));lost=[];gained=[]
for variant,rows in tables.items():
    for group in sorted({(k[0],k[2]) for k in rows}):
        subset=[r for k,r in rows.items() if (k[0],k[2])==group];times=sorted(int(r[7])/1e6 for r in subset)
        groups[group[0]+';addons='+group[1]][variant]=dict(inputs=len(subset),
            **{f'top{k}':sum(0<int(r[3])<=k for r in subset) for k in (1,3,8)},
            any_rank=sum(int(r[3])>0 for r in subset),automatic_hits=sum(r[6]=='true' for r in subset),
            nonreference_conversion=sum(r[6]=='false' and r[4]=='false' for r in subset),
            flush_p95_ms=times[math.ceil(len(times)*.95)-1])
for key,before in tables['before'].items():
    after=tables['after'][key];b=0<int(before[3])<=8;a=0<int(after[3])<=8
    if b!=a:
        label=labels[int(key[1])]
        (gained if a else lost).append(dict(condition=key[0],addons=key[2],raw=label['raw'],target=label['target'],document=label['document'],before_rank=int(before[3]),after_rank=int(after[3]),before_choice=before[8],after_choice=after[8]))
report=dict(role=args.role,scope='Consumed prose regression, not fresh holdout or semantic judgement; real shared-core replay, no native/UI timing claim',groups=groups,lost_first8=lost,gained_first8=gained)
(out/(args.role+'.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(groups=groups,lost_first8=len(lost),gained_first8=len(gained)),indent=2))
