"""Task-conditioned usefulness; off-target alternatives are not labeled nonsense."""
import argparse, collections, csv, gzip, hashlib, json
from pathlib import Path

def relevance(text, consumed, raw, target):
    if text == target and (consumed == 0 or consumed == len(raw)):
        return 'whole'
    if 0 < consumed < len(raw) and len(text) < len(target) and target.startswith(text):
        return 'glyph' if len(text) == 1 else 'phrase'
    return 'off_target'

def evaluate(values, raw, target, limit):
    shown=values if limit is None else values[:limit]
    labels=[relevance(text,end,raw,target) for text,end in shown]
    useful=[i for i,label in enumerate(labels) if label!='off_target']
    phrase=[i for i,label in enumerate(labels) if label in ('whole','phrase')]
    return dict(slots=len(shown),useful_slots=len(useful),whole_slots=labels.count('whole'),
        phrase_slots=labels.count('phrase'),glyph_slots=labels.count('glyph'),off_target_slots=labels.count('off_target'),
        first_useful_rank=useful[0]+1 if useful else 0,first_phrase_rank=phrase[0]+1 if phrase else 0,
        best_glyphs=max((len(shown[i][0]) for i in useful),default=0),
        whole_top1=bool(labels and labels[0]=='whole'),whole_available='whole' in labels)

def inventory(row):
    return [(text,int(end)) for value in row['outputs'].split('|') if value for text,end in [value.rsplit(':',1)]]

def aggregate(rows):
    result=dict(episodes=len(rows))
    for key in ('slots','useful_slots','whole_slots','phrase_slots','glyph_slots','off_target_slots','whole_top1','whole_available'):
        result[key]=sum(r[key] for r in rows)
    result['target_compatible_precision']=result['useful_slots']/result['slots'] if result['slots'] else None
    result['with_useful_choice']=sum(r['first_useful_rank']>0 for r in rows)
    result['with_phrase_choice']=sum(r['first_phrase_rank']>0 for r in rows)
    result['without_useful_choice']=len(rows)-result['with_useful_choice']
    result['mean_best_glyphs']=sum(r['best_glyphs'] for r in rows)/len(rows) if rows else None
    result['off_target_slots_before_first_useful_when_available']=sum(r['first_useful_rank']-1 for r in rows if r['first_useful_rank'])
    result['search_cost_denominator']=result['with_useful_choice']
    return result

def main():
    parser=argparse.ArgumentParser();parser.add_argument('before',type=Path);parser.add_argument('after',type=Path);parser.add_argument('output',type=Path);args=parser.parse_args()
    args.output.mkdir(parents=True,exist_ok=True)
    groups=collections.defaultdict(lambda:collections.defaultdict(lambda:[[],[]]));deltas=[];added=collections.Counter();changed=0
    with args.before.open(encoding='utf-8') as a,args.after.open(encoding='utf-8') as b:
        ra,rb=csv.DictReader(a,delimiter='\t'),csv.DictReader(b,delimiter='\t')
        from itertools import zip_longest
        for old,new in zip_longest(ra,rb):
            assert old is not None and new is not None
            assert all(old[k]==new[k] for k in ('genre','id','condition','raw','target'))
            if not old['target']:continue
            left,right=inventory(old),inventory(new);raw,target=old['raw'],old['target']
            for candidate in set(right)-set(left):
                added[relevance(*candidate,raw,target)]+=1
            for group in ('all',old['genre'],old['genre']+'/'+old['condition']):
                for limit in (5,8,None):
                    pair=groups[group][str(limit)];pair[0].append(evaluate(left,raw,target,limit));pair[1].append(evaluate(right,raw,target,limit))
            if left[:8]!=right[:8]:
                x,y=evaluate(left,raw,target,8),evaluate(right,raw,target,8)
                deltas.append(dict(genre=old['genre'],id=old['id'],condition=old['condition'],raw=raw,target=target,
                    before=left[:8],after=right[:8],before_metrics=x,after_metrics=y))
                changed+=1
    report=dict(scope='Known intended text; off-target means not useful for that target, not a linguistic nonsense label. Unlabeled rows excluded.',
        groups={g:{limit:dict(before=aggregate(pair[0]),after=aggregate(pair[1])) for limit,pair in limits.items()} for g,limits in groups.items()},
        added_candidate_occurrences=dict(added),changed_first8_episodes=changed,
        hashes={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (args.before,args.after)})
    (args.output/'core-summary.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode())
    (args.output/'first8-changes.jsonl.gz').write_bytes(gzip.compress(('\n'.join(json.dumps(r,ensure_ascii=False) for r in deltas)+'\n').encode(),mtime=0))
    print(json.dumps(dict(first8=report['groups']['all']['8'],added=dict(added),changed=changed),ensure_ascii=False))

if __name__=='__main__':main()
