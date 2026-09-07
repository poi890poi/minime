"""Compare observation evidence, never treating a completed replay as parity."""
import json,re,csv,sys
sys.stdout.reconfigure(encoding='utf-8')
from pathlib import Path
root=Path(__file__).resolve().parent.parent/'docs/parity'
matrix=json.loads((root/'matrix.json').read_text(encoding='utf-8'))
records={}
for path in sorted((root/'runs').glob('batch-*/observations.json')):
 for row in json.loads(path.read_text(encoding='utf-8-sig')):
  row['evidence']=str(path.relative_to(root));records[row['id'],row['provider']]=row
# Explicit settling/setup rechecks retain provenance. A failed recheck never
# replaces a completed observation; all raw attempts remain on disk.
for path in sorted((root/'runs').glob('recheck-*/observations.json')):
 for row in json.loads(path.read_text(encoding='utf-8-sig')):
  key=(row['id'],row['provider']);prior=records.get(key)
  if row['status']=='observed':
   row['evidence']=str(path.relative_to(root))
   if prior:row['prior_evidence']=prior['evidence']
   records[key]=row
def candidates(record,case):
 if not record:return []
 step=next((s for s in record['steps'] if s['stage']=='typed'),None)
 if not step:return []
 values=step['visible']
 if record['provider']=='minime':return list(dict.fromkeys(v[10:] for v in values if v.startswith('Candidate ')))
 # Special layouts can place digit keys before q or omit q entirely. Their
 # raw nodes remain available; do not label those keys as candidate words.
 if case.get('field') in ('password','number'):return []
 stop=next((i for i,v in enumerate(values) if v in ('q','Q')),len(values))
 return [v for v in dict.fromkeys(values[:stop]) if v not in ('其他候選鍵','選取中文鍵盤','中文鍵盤','英文鍵盤','語音輸入','…')]
rows=[]
for case in matrix:
 g=records.get((case['id'],'google'));m=records.get((case['id'],'minime'))
 if not g or not m:continue
 gs=g['steps'][-1];ms=m['steps'][-1]
 valid=g['status']==m['status']=='observed'
 target=case.get('target');gc=candidates(g,case);mc=candidates(m,case)
 row=dict(id=case['id'],group=case['group'],mode=case['mode'],google_status=g['status'],minime_status=m['status'],
  paired_valid=valid,google_text=gs['text'],minime_text=ms['text'],same_final_text=valid and gs['text']==ms['text'],
  google_action=gs['editorAction'],minime_action=ms['editorAction'],google_candidates=gc,minime_candidates=mc,
  google_composing=[gs['composingStart'],gs['composingEnd']],minime_composing=[ms['composingStart'],ms['composingEnd']],
  google_selection=[gs['selectionStart'],gs['selectionEnd']],minime_selection=[ms['selectionStart'],ms['selectionEnd']],
  google_orientation=gs.get('orientation'),minime_orientation=ms.get('orientation'),
  google_error=g.get('error'),minime_error=m.get('error'),evidence=g['evidence'],
  minime_evidence=m['evidence'],google_prior_evidence=g.get('prior_evidence'),minime_prior_evidence=m.get('prior_evidence'))
 if target:row.update(target=target,google_target_rank=gc.index(target)+1 if target in gc else None,minime_target_rank=mc.index(target)+1 if target in mc else None)
 rows.append(row)
(root/'comparison.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
with (root/'comparison.tsv').open('w',encoding='utf-8',newline='') as f:
 w=csv.writer(f,delimiter='\t');w.writerow(['id','group','google_status','minime_status','google_text','minime_text','same_final_text','google_candidates','minime_candidates'])
 for r in rows:w.writerow([r[k] if k not in ('google_candidates','minime_candidates') else ' | '.join(r[k]) for k in ['id','group','google_status','minime_status','google_text','minime_text','same_final_text','google_candidates','minime_candidates']])
summary=dict(planned_pairs=len(matrix),recorded_pairs=len(rows),valid_pairs=sum(r['paired_valid'] for r in rows),same_final_text=sum(r['same_final_text'] for r in rows))
targets=[r for r in rows if r.get('target') and r['paired_valid']]
summary['pinyin_targets']=dict(cases=len(targets),google_space_target=sum(r['google_text']==r['target'] for r in targets),minime_space_target=sum(r['minime_text']==r['target'] for r in targets))
summary['groups']={group:dict(cases=sum(r['group']==group for r in rows),valid=sum(r['group']==group and r['paired_valid'] for r in rows)) for group in dict.fromkeys(r['group'] for r in rows)}
(root/'summary.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
print(json.dumps(summary))
for r in rows:
 if not r['paired_valid']:print('UNAVAILABLE',r['id'],r['google_error'],r['minime_error'])
 elif not r['same_final_text']:print('DIFF',r['id'],repr(r['google_text']),'->',repr(r['minime_text']))
