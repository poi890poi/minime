"""Summarize paired gap checks without equating replay completion with parity."""
import json
from pathlib import Path
root=Path(__file__).resolve().parent.parent/'docs/gap-implementation'
before={r['id']:r for r in json.loads((root.parent/'parity/comparison.json').read_text(encoding='utf-8'))}
records_by_id={}
for plan in sorted((root/'plans').glob('*.json')):
    evidence=root/plan.stem/'observations.json'
    if not evidence.exists():continue
    records={(r['id'],r['provider']):r for r in json.loads(evidence.read_text(encoding='utf-8-sig'))}
    for case in json.loads(plan.read_text(encoding='utf-8')):
        g=records[case['id'],'google'];m=records[case['id'],'minime']
        gs=g['steps'][-1];ms=m['steps'][-1]
        prior=records_by_id.get(case['id'])
        row=dict(id=case['id'],group=case['group'],target=case.get('target'),
            before=before[case['id']]['minime_text'],google=gs['text'],minime=ms['text'],
            google_status=g['status'],minime_status=m['status'],
            google_action=gs['editorAction'],minime_action=ms['editorAction'],
            evidence=str(evidence.relative_to(root)))
        if prior:row['prior_evidence']=prior['evidence']
        records_by_id[case['id']]=row
rows=list(records_by_id.values())
targets=[r for r in rows if r['target']]
summary=dict(pairs=len(rows),observed=sum(r['google_status']==r['minime_status']=='observed' for r in rows),
    phrase_cases=len(targets),before=sum(r['before']==r['target'] for r in targets),
    minime=sum(r['minime']==r['target'] for r in targets),google=sum(r['google']==r['target'] for r in targets))
(root/'comparison.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(root/'summary.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
print(json.dumps(summary))
