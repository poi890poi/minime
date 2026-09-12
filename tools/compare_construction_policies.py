"""Counterfactual ranking over frozen native outputs; never a production importer."""
import json,gzip
from pathlib import Path
from collections import defaultdict
import argparse
ROOT=Path(__file__).resolve().parent.parent;OUT=ROOT/'docs/construction-confidence'
parser=argparse.ArgumentParser();parser.add_argument('--role',required=True,choices=['development','reserved']);args=parser.parse_args()
rows=[json.loads(l) for l in gzip.decompress((OUT/(args.role+'-native.jsonl.gz')).read_bytes()).decode().splitlines()]
def bounded(choices,n):
    full=[c for c in choices if c['end']==n][:24];prefix=[c for c in choices if c['end']<n][:12]
    return full[:3]+prefix[:3]+full[3:]+prefix[3:]
def rank(choices,policy):
    generated=[c for c in choices if c['kind']=='sentence'];attested=[c for c in choices if c['kind']!='sentence']
    if policy=='baseline':return choices
    if policy=='after-all-attested':return attested+generated
    if policy=='after-early-attested':return attested[:3]+generated+attested[3:]
    raise ValueError(policy)
result={}
for condition in dict.fromkeys(r['condition'] for r in rows):
    group=[r for r in rows if r['condition']==condition];variants={}
    for policy in ['baseline','after-all-attested','after-early-attested']:
        totals=defaultdict(int)
        for r in group:
            choices=rank(bounded(r['choices'],len(r['raw'])),policy);totals['inputs']+=1
            for k in [1,3,8]:totals['top'+str(k)+'_reference_hits']+=any(c['text']==r['target'] for c in choices[:k])
            full=[c for c in choices if c['end']==len(r['raw']) and (policy=='baseline' or c['kind']!='sentence')]
            chosen=full[0]['text'] if full else r['raw']
            totals['automatic_reference_hits']+=chosen==r['target']
            totals['automatic_nonreference_conversion']+=chosen!=r['target'] and chosen!=r['raw']
            totals['literal_recovery']+=chosen==r['raw']
        variants[policy]=dict(totals)
    result[condition]=variants
(OUT/(args.role+'-policy-comparison.json')).write_text(json.dumps({'scope':'native-only counterfactual, bounded like Android; no Java/addon merge, no human semantic judgement','results':result},indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,indent=2))
