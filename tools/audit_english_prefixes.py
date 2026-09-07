"""Check the 500-entry scan cap against the same dictionary's frequency ordering.
This measures truncation, not independent language-model accuracy.
"""
from pathlib import Path
from collections import defaultdict
import json
root=Path(__file__).resolve().parent.parent
words=[(p[0],int(p[1])) for p in (line.split('\t') for line in (root/'app/src/main/assets/en_us.tsv').read_text(encoding='utf-8').splitlines())]
groups=defaultdict(list)
for word,frequency in sorted(words):
 if len(word)>2:groups[word[:2]].append((word,frequency))
results=[]
for prefix,group in sorted(groups.items()):
 if len(group)<=500:continue
 top=lambda items:[word for word,_ in sorted(items,key=lambda v:-v[1])[:3]]
 full=top(group);bounded=top(group[:500])
 if full!=bounded:results.append(dict(prefix=prefix,matches=len(group),current_top3=bounded,uncapped_frequency_top3=full))
(root/'docs/parity/english-prefix-cap.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')
print(json.dumps(results))
