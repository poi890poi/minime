"""Compare frozen composition outputs without using target labels in decoding."""
import argparse,collections,csv,gzip,hashlib,itertools,json
from pathlib import Path
from audit_candidate_usefulness import inventory,evaluate

def main():
    parser=argparse.ArgumentParser();parser.add_argument('before',type=Path);parser.add_argument('after',type=Path);parser.add_argument('output',type=Path);parser.add_argument('--baseline',default='fd728d5');parser.add_argument('--aggregate-only',action='store_true',help='Retain original TSV telemetry; omit the duplicate raw-change archive');a=parser.parse_args()
    groups=collections.defaultdict(collections.Counter);changed=[]
    with a.before.open(encoding='utf-8') as left,a.after.open(encoding='utf-8') as right:
        for old,new in itertools.zip_longest(csv.DictReader(left,delimiter='\t'),csv.DictReader(right,delimiter='\t')):
            assert old is not None and new is not None
            assert all(old[k]==new[k] for k in ('genre','id','condition','raw','target'))
            x,y=inventory(old),inventory(new)
            for key in ('all',old['genre'],old['genre']+'/'+old['condition']):
                c=groups[key];c['cases']+=1;c['order_changed']+=x!=y;c['inventory_span_changed']+=sorted(x)!=sorted(y);c['space_changed']+=old['space']!=new['space']
                if old['target']:
                    c['labeled']+=1
                    for limit in (1,5,8):
                        before=evaluate(x,old['raw'],old['target'],limit);after=evaluate(y,old['raw'],old['target'],limit)
                        c[f'before_whole_{limit}']+=before['whole_available'];c[f'after_whole_{limit}']+=after['whole_available']
                        c[f'gains_{limit}']+=after['whole_available'] and not before['whole_available'];c[f'losses_{limit}']+=before['whole_available'] and not after['whole_available']
                        c[f'before_useful_slots_{limit}']+=before['useful_slots'];c[f'after_useful_slots_{limit}']+=after['useful_slots']
            if not a.aggregate_only and (x!=y or old['space']!=new['space']):changed.append(dict(before=old,after=new))
    report=dict(baseline=a.baseline,groups={k:dict(v) for k,v in sorted(groups.items())},hashes={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (a.before,a.after)})
    a.output.mkdir(parents=True,exist_ok=True)
    (a.output/'trial-summary.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode())
    if not a.aggregate_only:
        (a.output/'trial-changes.jsonl.gz').write_bytes(gzip.compress(('\n'.join(json.dumps(r,ensure_ascii=False) for r in changed)+'\n').encode(),mtime=0))
    print(json.dumps({k:v for k,v in report['groups'].items() if '/' not in k},ensure_ascii=False,indent=2))
if __name__=='__main__':main()
