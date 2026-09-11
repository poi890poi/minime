"""Score recorded outputs after runs; never called by a provider."""
import collections, json, statistics
from pathlib import Path
from run import HERE, rows
from make_corpus import dump

def stats(values):
    a=sorted(values)
    if not a:return {}
    return {'n':len(a),'mean':statistics.mean(a),'p50':a[len(a)//2],
            'p95':a[min(len(a)-1,int(len(a)*.95))],'p99':a[min(len(a)-1,int(len(a)*.99))],'max':a[-1]}

def group(values,fields):
    groups=collections.defaultdict(list)
    for r in values:groups['/'.join(str(r.get(f,'')) for f in fields)].append(r)
    return groups

def main():
    result={}
    for engine in ['minime','kazuma']:
        result[engine]={'completion':{},'probes':{},'performance':{},'ajimee':{}}
        for role in ['development','holdout']:
            path=HERE/f'{engine}-completion-{role}.jsonl.gz'
            if path.exists():
                for key,a in group(rows(path.name),['source','role','policy']).items():
                    totals=collections.Counter()
                    for x in a:totals.update(x['counts'])
                    successes=[x for x in a if x['success']]
                    fresh=[x for x in a if not x['previously_seen_text'] and not x['cross_role_repeat']]
                    events=[ev for x in a for ev in x['events'] if '/' not in ev['id']]
                    result[engine]['completion'][key]={'turns':len(a),'complete_turns':len(successes),'turn_rate':len(successes)/len(a),
                        'non_repeated_turns':len(fresh),'non_repeated_success':sum(x['success'] for x in fresh),
                        'clause_attempts':len(events),'clause_hits':sum(x['rank']>=0 for x in events),
                        'actions_all_attempts':stats([x['counts']['actions'] for x in a]),
                        'actions_successes':stats([x['counts']['actions'] for x in successes]),
                        'actions_per_reference_char_all':totals['actions']/totals['output_chars'],
                        'totals':dict(totals)}
            path=HERE/f'{engine}-probes-{role}.jsonl.gz'
            if path.exists():
                for key,a in group(rows(path.name),['source','role','reference','condition']).items():
                    changed=[x for x in a if x['changed']]
                    result[engine]['probes'][key]={'n':len(a),'initial_hits':sum(x['initial_hit'] for x in a),
                        'after_retry_hits':sum(x['recovered'] for x in a),'changed_n':len(changed),'changed_hits':sum(x['initial_hit'] for x in changed),
                        'mean_typed':statistics.mean(x['counts'].get('typed',0) for x in a),
                        'mean_deletes':statistics.mean(x['counts'].get('deletes',0) for x in a)}
        path=HERE/f'{engine}-ajimee-development.jsonl.gz'
        if path.exists():
            for key,a in group(rows(path.name),['reference_has_context']).items():
                result[engine]['ajimee'][key]={'n':len(a),'top8':sum(x['rank']>=0 for x in a),
                    'default':sum(x['default_hit'] for x in a),'mean_min_cer':statistics.mean(x['min_cer'] for x in a),
                    'raw_over_96':sum(x['raw_over_96'] for x in a)}
            bounded=[x for x in rows(path.name) if not x['raw_over_96']]
            result[engine]['ajimee']['within_96_keys']={'n':len(bounded),'top8':sum(x['rank']>=0 for x in bounded),
                'default':sum(x['default_hit'] for x in bounded),'mean_min_cer':statistics.mean(x['min_cer'] for x in bounded)}
        for passno in [1,2,3]:
            path=HERE/f'{engine}-perf-{passno}.jsonl.gz'
            if path.exists():
                result[engine]['performance'][str(passno)]={key:{'engine_ms':stats([x['engine_ns']/1e6 for x in a]),
                    'transport_inclusive_ms':stats([x['wall_ns']/1e6 for x in a])} for key,a in group(rows(path.name),['cycle']).items()}
    dump(HERE/'summary.json',result)
    for engine,r in result.items():
        print(engine)
        for key,a in r['completion'].items():
            if '/holdout/' in key:print(key,a['complete_turns'],a['turns'],a['clause_hits'],a['clause_attempts'],round(a['actions_per_reference_char_all'],2))
        print('AJIMEE',r['ajimee'])
        for key,a in r['performance'].items():print('perf',key,a['repeat']['engine_ms'])

if __name__=='__main__':main()
