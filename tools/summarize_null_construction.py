"""Paired reference reconstruction; never labels alternative phrases nonsense."""
from pathlib import Path
from collections import defaultdict
import json,gzip,statistics,random,hashlib
R=Path(__file__).resolve().parent.parent;O=R/'docs/null-construction'
def read(p):return [json.loads(s) for s in gzip.decompress(p.read_bytes()).decode().splitlines()]
labels=json.loads(gzip.decompress((R/'docs/construction-confidence/inputs.json.gz').read_bytes()))
report={'scope':'Consumed prose reference reconstruction; no semantic precision, Mandarin conversation, actual correction cost or human typing-time claim.','groups':{},'english':{}}
for role in ['development','reserved']:
    refs=[r for r in labels if r['role']==role]
    data={v:read(O/(role+'-'+v+'.jsonl.gz')) for v in ['null','native','java','both']}
    assert all(len(rows)==len(refs)*2 for rows in data.values())
    for condition in ['full','initials','mixed','partial']:
        for optional in [False,True]:
            group=role+'/'+condition+'/addons='+str(optional).lower();stats={};selected={}
            for variant,rows in data.items():
                part=[r for r in rows if r['condition']==condition and r['addons']==optional];selected[variant]=part;n=len(part)
                stats[variant]={'inputs':n,'rank1':sum(r['rank']==1 for r in part),'recall8':sum(0<r['rank']<=8 for r in part),'recall_all':sum(r['rank']>0 for r in part),'mrr':sum(1/r['rank'] if r['rank'] else 0 for r in part)/n,'space_hits':sum(r['hit'] for r in part),'construction_exposure':sum(r['constructed']>0 for r in part),'construction_top8':sum(any(c['constructed'] for c in r['candidates'][:8]) for r in part),'no_han':sum(r['han']==0 for r in part),'core_flush_p95_ms':sorted(r['core_ns']/1e6 for r in part)[int(n*.95)],'one_selection_top8_reference':sum(0<r['rank']<=8 for r in part)}
            for variant in ['native','java','both']:
                deltas=defaultdict(lambda:[0,0,0]);gained=lost=0
                for b,c in zip(selected['null'],selected[variant]):
                    assert (b['index'],b['addons'])==(c['index'],c['addons'])
                    bv=0<b['rank']<=8;cv=0<c['rank']<=8;gained+=cv and not bv;lost+=bv and not cv
                    d=deltas[refs[b['index']]['document']];d[0]+=(c['rank']==1)-(b['rank']==1);d[1]+=cv-bv;d[2]+=1
                rng=random.Random(20260912);values=list(deltas.values());intervals=[]
                for metric in [0,1]:
                    samples=[]
                    for _ in range(2000):
                        draw=rng.choices(values,k=len(values));samples.append(sum(x[metric] for x in draw)/sum(x[2] for x in draw))
                    samples.sort();intervals.append([samples[49],samples[1949]])
                stats[variant].update(gained8=gained,lost8=lost,paired_document_bootstrap95_rank1_delta=intervals[0],paired_document_bootstrap95_recall8_delta=intervals[1])
            report['groups'][group]=stats
for variant in ['null','native','java','both']:
    rows=[json.loads(s) for s in (O/('english-'+variant+'.jsonl')).read_text(encoding='utf-8').splitlines()]
    for group in ['en-gum-conversation','en-gum-essay']:
        for mode in ['fresh','primed','english']:
            selected=[r for r in rows if r['group']==group and r['mode']==mode];tokens=[t for r in selected for t in r['tokens']]
            key=group+'/'+mode;report['english'].setdefault(key,{})[variant]={'sentences':len(selected),'tokens':len(tokens),'literal_word_hits':sum(t['output'].lower()==t['raw'].lower() for t in tokens),'han_conversions':sum(any('\u3400'<=c<='\u9fff' for c in t['output']) for t in tokens)}
report['effort']={}
effort={v:[json.loads(s) for s in (O/('effort-'+v+'.jsonl')).read_text(encoding='utf-8').splitlines()] for v in ['null','native','java','both']}
for role in ['development','reserved']:
    for condition in ['full','initials','mixed','partial']:
        key=role+'/'+condition;report['effort'][key]={};base=[r for r in effort['null'] if r['role']==role and r['condition']==condition]
        for variant,rows in effort.items():
            part=[r for r in rows if r['role']==role and r['condition']==condition];ok=[r for r in part if r['success']]
            paired=[(b,r) for b,r in zip(base,part) if b['success'] and r['success']]
            report['effort'][key][variant]={'inputs':len(part),'completed':len(ok),'mean_selections_on_success':statistics.mean(r['selections'] for r in ok) if ok else None,'common_successes':len(paired),'selection_delta_on_common_successes':statistics.mean(r['selections']-b['selections'] for b,r in paired) if paired else None,'gained_completion':sum(r['success'] and not b['success'] for b,r in zip(base,part)),'lost_completion':sum(b['success'] and not r['success'] for b,r in zip(base,part))}
report['native_control_parity']={}
for role in ['development','reserved']:
    current=read(O/'native-on'/(role+'-native.jsonl.gz'));prior=read(R/'docs/construction-confidence'/(role+'-native.jsonl.gz'))
    def signature(r):return [(c['end'],c['kind'],c['text']) for c in r['choices']]
    assert [r['raw'] for r in current]==[r['raw'] for r in prior]
    report['native_control_parity'][role]={'queries':len(current),'different_text_order_consumption_origin':sum(signature(a)!=signature(b) for a,b in zip(current,prior))}
report['balanced_core_timing']={'scope':'Three separate JVM passes, 128 identical hash-selected queries per variant, add-ons off/on. Replay variants executed sequentially. Core flush only; dictionary loading and native decoding excluded. Not touch latency or an idle-device certification.','variants':{}}
for variant in ['null','native','java','both']:
    passes=[]
    for pass_id in range(3):
        rows=[json.loads(s) for s in (O/('timing-'+str(pass_id)+'-'+variant+'.jsonl')).read_text(encoding='utf-8').splitlines()]
        times=sorted(r['core_ns']/1e6 for r in rows)
        passes.append({'n':len(times),'p50_ms':statistics.median(times),'p95_ms':times[int(len(times)*.95)],'max_ms':max(times)})
    report['balanced_core_timing']['variants'][variant]=passes
(O/'summary.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for k,v in report['groups'].items():
    if k.endswith('addons=true'):print(k,{a:{m:b[m] for m in ['rank1','recall8','space_hits','construction_top8']} for a,b in v.items()})
print('English',report['english'])
