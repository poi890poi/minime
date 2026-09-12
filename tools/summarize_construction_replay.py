"""Report actual core replay separately from native-only counterfactuals."""
from pathlib import Path
import csv,json,gzip,hashlib,math
from collections import defaultdict
root=Path(__file__).resolve().parent.parent
out=root/'docs/construction-confidence'
report={}
for role in ('development','reserved'):
    report[role]={}
    for variant in ('baseline','policy'):
        source=root/'artifacts/native-metadata'/f'{role}-core-{variant}.tsv'
        if not source.exists():continue
        groups=defaultdict(list)
        for row in csv.reader(source.open(encoding='utf-8'),delimiter='\t'):
            if len(row)!=9:raise ValueError('Incomplete replay record')
            groups[row[0]+';addons='+row[2]].append(row)
        summary={}
        for group,rows in groups.items():
            expected=1275 if role=='development' else 1530
            assert len(rows)==expected,(role,variant,group,len(rows))
            times=sorted(int(r[7])/1e6 for r in rows)
            summary[group]=dict(inputs=len(rows),**{f'top{k}_hits':sum(0<int(r[3])<=k for r in rows) for k in (1,3,8)},
                automatic_hits=sum(r[6]=='true' for r in rows),automatic_nonreference_conversion=sum(r[6]=='false' and r[4]=='false' for r in rows),
                literal_default=sum(r[4]=='true' for r in rows),constructed_default=None if variant=='baseline' else sum(r[5]=='true' for r in rows),
                replay_flush_ms=dict(p50=times[len(times)//2],p95=times[math.ceil(len(times)*.95)-1],maximum=max(times)))
        report[role][variant]=summary
        target=out/(source.name+'.gz');target.write_bytes(gzip.compress(source.read_bytes(),mtime=0))
report['scope']='Real core merge, Java conversion, English competition and optional Taiwan/geography add-ons; frozen native replay. Flush timing excludes native decoding, typing, UI and physical touch. Parallel runs are not a speedup claim.'
(out/'core-comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
