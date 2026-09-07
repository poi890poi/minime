"""Score frozen desktop runs, preserving all rows separately from production data."""
import collections, hashlib, json, sys
from pathlib import Path
def summarize(path):
    groups=collections.defaultdict(collections.Counter);failures=[];rows=0
    for line in Path(path).read_text(encoding='utf-8').splitlines():
        r=json.loads(line);rows+=1
        if r['group'].startswith('en-'):
            g=groups[r['group']+'/'+r['mode']];g['sentences']+=1
            for t in r['tokens']:
                g['tokens']+=1;known=t['known'];changed=t['output']!=t['raw'];noninitial=t['position']>0
                g['known_tokens']+=known;g['changed_tokens']+=changed
                g['known_noninitial_tokens']+=known and noninitial
                g['known_noninitial_changed']+=known and noninitial and changed
                if changed and len(failures)<100:failures.append(dict(group=r['group'],mode=r['mode'],id=r['id'],**t))
        else:
            g=groups[r['group']];g['probes']+=1;rank=r['rank'];g['top1']+=rank==1;g['top5']+=0<rank<=5;g['reachable']+=rank>0;g['space_exact']+=r['output']==r['expected']
    return dict(file=Path(path).name,sha256=hashlib.sha256(Path(path).read_bytes()).hexdigest(),rows=rows,groups=dict(groups),first_changed_examples=failures)
report={Path(p).stem:summarize(p) for p in sys.argv[1:]}
out=Path('docs/conversation-ranking/desktop-summary.json');out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for name,r in report.items():print(name,json.dumps(r['groups'],ensure_ascii=False))
