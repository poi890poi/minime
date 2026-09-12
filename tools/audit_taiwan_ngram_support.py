"""Audit independent source support, not an IME accuracy or semantic score."""
from pathlib import Path
from collections import Counter
import json,gzip,hashlib
R=Path(__file__).resolve().parent.parent;OUT=R/'docs/whole-input-quality/ngram-source'
attested=set();inventory={};tokens=set()
for n in range(1,5):
 data=gzip.decompress((OUT/f'token_{n}gram.tsv.gz').read_bytes()).decode('utf-8')
 rows=0;minimum=10**9;maximum=0
 for line in data.splitlines():
  count,text=line.split('\t');count=int(count);parts=text.split(' ')
  assert len(parts)==n and count>=40
  rows+=1;minimum=min(minimum,count);maximum=max(maximum,count)
  attested.add(''.join(parts));tokens.update(parts)
 inventory[str(n)]=dict(rows=rows,min_host_support=minimum,max_host_support=maximum)
base={line.split('\t')[2] for line in (R/'app/src/main/assets/zh_tw.tsv').read_text(encoding='utf-8').splitlines()}
report=dict(inventory=inventory,distinct_concatenated_forms=len(attested),distinct_tokens=len(tokens),tokens_in_base=len(tokens&base),groups={})
for role in ['development','reserved']:
 rows=[json.loads(s) for s in gzip.decompress((R/'docs/construction-confidence'/(role+'-native.jsonl.gz')).read_bytes()).decode().splitlines()]
 for condition in ['full','initials','mixed','partial']:
  values=[v for v in rows if v['condition']==condition];counts=Counter()
  for v in values:
   counts['inputs']+=1;counts['reference_in_base']+=v['target'] in base;counts['reference_in_ngram_forms']+=v['target'] in attested
   s=v['sentence']
   if not s:continue
   hit=s['text']==v['target'];counts['assembled']+=1;counts['assembled_reference_hits']+=hit
   full=s['text'] in attested
   pairs=len(s['components'])>1 and all(a+b in attested for a,b in zip(s['components'],s['components'][1:]))
   for label,has in [('whole_form_support',full),('every_boundary_support',pairs)]:
    if has:counts[label]+=1;counts[label+'_reference_hits']+=hit
  report['groups'][role+'/'+condition]=dict(counts)
report['limitations']=['No token sequence becomes a production entry or score.','Reference mismatch is not synonymous with meaningless output.','Attestation is web support, not semantic judgment or conversational frequency.','Existing source documents may overlap web training; this cannot establish held-out generalization.','Boundary support audits the existing chosen path, not alternative-path decoding.']
(OUT/'audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
