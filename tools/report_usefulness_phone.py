"""Audit actual visible rows; never treat runner OK as all observations passing."""
import argparse,collections,gzip,hashlib,json
from pathlib import Path
from audit_candidate_usefulness import aggregate
from audit_mixed_english import latin_kind

def extract(provider,stage,visible):
    if provider=='minime':
        return [s[10:] for s in visible if s.startswith('Candidate ') and s!='Candidate list']
    marker='其他候選鍵' if stage=='typed' else '隱藏其他候選鍵'
    if marker not in visible:raise ValueError('Google candidate boundary unavailable')
    result=[s for s in visible[:visible.index(marker)] if s and s!='…']
    if stage=='expanded':
        # Google exposes its spelling sidebar and script toggle between row/grid.
        if '輸入簡體中文' not in visible:raise ValueError('Google expanded grid boundary unavailable')
        at=visible.index('輸入簡體中文')+1
        if at<len(visible) and visible[at]=='簡':at+=1
        for s in visible[at:]:
            if s in ('Page Down 鍵','刪除','Enter 鍵'):break
            if s and s!='…':result.append(s)
    return result

def score(texts,target):
    labels=['whole' if text==target else ('glyph' if len(text)==1 else 'phrase')
        if len(text)<len(target) and target.startswith(text) else 'off_target' for text in texts]
    useful=[i for i,label in enumerate(labels) if label!='off_target'];phrase=[i for i,label in enumerate(labels) if label in ('whole','phrase')]
    return dict(slots=len(texts),useful_slots=len(useful),whole_slots=labels.count('whole'),phrase_slots=labels.count('phrase'),
        glyph_slots=labels.count('glyph'),off_target_slots=labels.count('off_target'),first_useful_rank=useful[0]+1 if useful else 0,
        first_phrase_rank=phrase[0]+1 if phrase else 0,best_glyphs=max((len(texts[i]) for i in useful),default=0),
        whole_top1=bool(labels and labels[0]=='whole'),whole_available='whole' in labels)

def main():
    p=argparse.ArgumentParser();p.add_argument('plans',type=Path);p.add_argument('artifacts',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
    plans={case['id']:case for file in sorted(a.plans.glob('batch-*.json')) for case in json.loads(file.read_text(encoding='utf-8'))}
    groups=collections.defaultdict(list);rows=[];failures=[];accepts=collections.defaultdict(list);seen=set();hashes={}
    english=collections.defaultdict(collections.Counter)
    for file in sorted(a.artifacts.glob('phone-*/observations.json')):
        hashes[str(file)]=hashlib.sha256(file.read_bytes()).hexdigest()
        for record in json.loads(file.read_text(encoding='utf-8')):
            case=plans[record['id']];source=case['source'];provider=record['provider'];identity=(provider,record['id']);assert identity not in seen;seen.add(identity)
            if record['status']!='observed':failures.append(dict(provider=provider,id=record['id'],error=record.get('error')))
            row=dict(source,source_id=source['id'],provider=provider,status=record['status'],stages={});row['id']=record['id']
            for stage in ('typed','expanded'):
                snapshot=next((s for s in record['steps'] if s['stage']==stage),None)
                if snapshot is None:row['stages'][stage]=dict(unavailable='missing snapshot');continue
                try:texts=extract(provider,stage,snapshot['visible'])
                except ValueError as error:row['stages'][stage]=dict(unavailable=str(error));continue
                raw_choices=[text for text in texts if text==source['raw']]
                texts=[text for text in texts if text!=source['raw']]
                kinds=[latin_kind(text,source['raw']) for text in texts]
                exposure=english[provider+'/'+stage]
                exposure['episodes']+=1
                exposure['with_latin']+=any(kinds)
                exposure['latin_top1']+=bool(kinds and kinds[0])
                exposure['latin_slots']+=sum(bool(k) for k in kinds)
                for kind in kinds:
                    if kind:exposure[kind]+=1
                metrics=score(texts,source['target']);row['stages'][stage]=dict(candidates=texts,metrics=metrics,excluded_exact_raw=len(raw_choices))
                for genre in ('all',source['genre']):groups[(provider,stage,genre)].append(metrics)
            accepted=next((s for s in record['steps'] if s['stage']=='accepted'),None)
            if accepted:
                row['accepted']=dict(text=accepted['text'],composing_start=accepted['composingStart'],composing_end=accepted['composingEnd'])
                exact=accepted['text'].rstrip(' ')==source['target'] and accepted['composingStart']==-1
                row['accepted']['whole_target_committed']=exact
                accepts[provider].append(exact)
            rows.append(row)
    report=dict(scope='Visible text compatibility for a specified task. Prefix spans not proven by screenshots. Alternative homophones are not labeled nonsense. Google learning persists; MinIME fresh per case.',
        planned_cases=len(plans),observed_records=len(rows),groups={'/'.join(k):aggregate(v) for k,v in groups.items()},
        acceptance={p:dict(observed=len(accepts[p]),whole_target_committed=sum(accepts[p]),missing=len(plans)-len(accepts[p])) for p in ('google','minime')},
        action_failures=failures,rows=rows,hashes=hashes,english_exposure={k:dict(v) for k,v in english.items()})
    a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode())
    print(json.dumps(dict(groups={k:v for k,v in report['groups'].items() if k.endswith('/all')},acceptance=report['acceptance'],failures=len(failures)),ensure_ascii=False))

if __name__=='__main__':main()
