"""Validate each completed frozen replay before permitting the next session."""
import csv,hashlib,json,re,sys
from pathlib import Path
root=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(root/'tools'))
from report_touch_latency import report,validate_workload
tag=sys.argv[1]
if not re.fullmatch(r'(chinese|taiwanese|japanese|english)-(old|new)',tag):raise ValueError('Unknown session tag')
here=root/'artifacts/single-window-language';log=here/(tag+'.log');body=log.read_text(encoding='utf-8-sig')
matches=re.findall(r'Session evidence: .*?device-tests[\\/]([0-9a-f-]{36})',body)
if len(matches)!=1 or 'OK (1 test)' not in body or 'Final display OFF verified' not in body:raise ValueError('Incomplete replay or cleanup')
session=matches[0];source=root/'artifacts/device-tests'/session/'touch-latency.tsv'
def rows(path):
    with path.open(encoding='utf-8-sig') as stream:return list(csv.DictReader(stream,delimiter='\t'))
data=rows(source);language=tag.split('-')[0]
mode={'taiwanese':'taiwanese_english','japanese':'japanese_english'}.get(language,language)
corpus=root/'docs/release-hardening/language-timing/inputs.tsv'
result=report(data,tag);result['workload_validation']=validate_workload(data,rows(corpus),mode,0)
result['session']=session
result['inputs']={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [source,corpus,log]}
outliers=[]
for r in data:
    for metric,end in [('raw_or_space','editor_submit_ns'),('candidate','candidate_submit_ns')]:
        if int(r[end]) and (int(r[end])-int(r['up_ns']))>100_000_000:
            outliers.append(dict(query_id=r['query_id'],mode=r['mode'],source=r['source'],genre=r['genre'],condition=r['condition'],
                interval_ms=int(r['interval_ms']),action=r['action'],typed_length=len(r['expected']),metric=metric,
                delay_ms=(int(r[end])-int(r['up_ns']))/1e6))
result['over_100_ms']=outliers
result['raw_space_missing']=sum(g[k]['unobserved'] for g in result['groups'].values() for k in ('editor_submission','space_submission'))
reports=here/'reports';reports.mkdir(exist_ok=True)
(reports/(tag+'.json')).write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(tag,'actions',len(data),'missing raw/Space',result['raw_space_missing'],'over 100 ms',len(outliers))
for key,g in result['groups'].items():print(key,'candidate p95',round(g['candidate_submission']['p95_ms'],2),'observed',g['candidate_submission']['observed'],'of',g['candidate_submission']['n'])
if result['raw_space_missing']:raise SystemExit('Missing raw/Space frames block further replay expansion')
