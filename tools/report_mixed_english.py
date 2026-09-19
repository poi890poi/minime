"""Paired word/prefix retention audit; ranks include explicit raw recovery."""
import argparse, collections, csv, gzip, hashlib, itertools, json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
base = root / 'artifacts/candidate-usefulness'
parser=argparse.ArgumentParser()
parser.add_argument('--before',type=Path,default=base/'english-before.tsv')
parser.add_argument('--after',type=Path,default=base/'english-after.tsv')
parser.add_argument('--output',type=Path,default=root/'docs/candidate-usefulness')
args=parser.parse_args()
groups = collections.defaultdict(collections.Counter)
changes = []
lineage = collections.defaultdict(set)
for name in ('inputs.tsv','gum-test.tsv'):
    for line in (root/'docs/conversation-ranking/corpus'/name).read_text(encoding='utf-8').splitlines():
        cells=line.split('\t')
        if len(cells)>=4 and cells[0].startswith('en-'):
            for word in cells[3].split(' '):lineage[(name,word)].add(cells[0])
with args.before.open(encoding='utf-8') as a, args.after.open(encoding='utf-8') as b:
    for old, new in itertools.zip_longest(csv.DictReader(a, delimiter='\t'), csv.DictReader(b, delimiter='\t')):
        assert old is not None and new is not None
        assert all(old[k] == new[k] for k in ('source','word','condition','raw','mode'))
        keys=[old['mode'], old['mode']+'/'+old['condition'], old['source']+'/'+old['mode']+'/'+old['condition']]
        keys += ['source-group/'+g+'/'+old['mode']+'/'+old['condition'] for g in sorted(lineage[(old['source'],old['word'])])]
        for key in keys:
            c = groups[key]; c['episodes'] += 1
            for label, row in (('before', old), ('after', new)):
                rank = int(row['rank'])
                for n in (1, 5, 8):
                    c[f'{label}_first{n}'] += 0 < rank <= n
                c[f'{label}_anywhere'] += rank > 0
            c['space_changed'] += old['space'] != new['space']
            c['inventory_order_changed'] += old['inventory_sha256'] != new['inventory_sha256']
            for n in (1,5,8):
                was=0<int(old['rank'])<=n;now=0<int(new['rank'])<=n
                c[f'lost_first{n}']+=was and not now
                c[f'gained_first{n}']+=now and not was
        if old['rank'] != new['rank'] or old['space'] != new['space']:
            changes.append(dict(before=old, after=new))
report = dict(scope='512 hash-selected unique words from each pinned EWT/GUM source set; whole, missing-last and half-prefix; isolated words, no natural-context or conversational accuracy claim; reused evidence, not fresh holdout. Ranks include raw recovery.',
              source_group_scope='Separate source-role/genre word probes. A word can occur in several source groups, so these groups overlap and must not be summed. No training data or fit; previously exposed dev/test are both labeled reused evaluation.',
              groups={k:dict(v) for k,v in groups.items()}, changed_rank_or_space=len(changes),
              hashes={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in
                      [args.before.resolve(), args.after.resolve(), root/'docs/conversation-ranking/corpus/inputs.tsv', root/'docs/conversation-ranking/corpus/gum-test.tsv']})
out=args.output;out.mkdir(parents=True,exist_ok=True)
(out/'english-priority-summary.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n', encoding='utf-8')
(out/'english-priority-changes.jsonl.gz').write_bytes(gzip.compress(('\n'.join(json.dumps(row,ensure_ascii=False) for row in changes)+'\n').encode(),mtime=0))
print(json.dumps({k:v for k,v in report['groups'].items() if k.count('/')==1},ensure_ascii=False))
