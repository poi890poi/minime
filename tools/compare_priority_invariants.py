"""Verify full candidate/span preservation and Space, not just aggregate recall."""
import argparse, collections, csv, hashlib, itertools, json
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('before',type=Path);parser.add_argument('after',type=Path);parser.add_argument('output',type=Path)
args=parser.parse_args();counts=collections.Counter()
with args.before.open(encoding='utf-8') as a,args.after.open(encoding='utf-8') as b:
    for old,new in itertools.zip_longest(csv.DictReader(a,delimiter='\t'),csv.DictReader(b,delimiter='\t')):
        assert old is not None and new is not None
        assert all(old[k]==new[k] for k in ('genre','id','condition','raw','target'))
        left=old['outputs'].split('|');right=new['outputs'].split('|')
        counts['episodes']+=1
        counts['space_changes']+=old['space']!=new['space']
        counts['candidate_or_span_changes']+=collections.Counter(left)!=collections.Counter(right)
        counts['list_order_changes']+=left!=right
        for n in (5,8):counts[f'first{n}_membership_changes']+=collections.Counter(left[:n])!=collections.Counter(right[:n])
        assert new['select_ok']=='true'
report=dict(counts=counts,hashes={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (args.before,args.after)})
args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(counts))
assert counts['space_changes']==counts['candidate_or_span_changes']==0
