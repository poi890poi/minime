"""Report source retrieval deltas, retaining omissions and regressions."""
import csv, collections, gzip, json
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
report={}
for condition,filename in [('full','retrieval-bounded.tsv'),('half','partial-retrieval.tsv')]:
    rows=list(csv.DictReader((ROOT/'artifacts/poj-tones'/filename).open(encoding='utf-8'),delimiter='\t'))
    groups={}
    for category in sorted({r['category'] for r in rows}):
        group=[r for r in rows if r['category']==category]
        groups[category]=dict(n=len(group),before_top8=sum(0<int(r['before_rank'])<=8 for r in group),
            after_top8=sum(0<int(r['after_rank'])<=8 for r in group),before_missing=sum(int(r['before_rank'])==0 for r in group),
            after_missing=sum(int(r['after_rank'])==0 for r in group),lost_available=sum(int(r['before_rank'])>0 and int(r['after_rank'])==0 for r in group))
    report[condition]=groups
    with gzip.open(ROOT/'docs/poj-tones'/(condition+'-results.tsv.gz'),'wb') as out:out.write((ROOT/'artifacts/poj-tones'/filename).read_bytes())
(ROOT/'docs/poj-tones/retrieval.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
