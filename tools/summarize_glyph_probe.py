from pathlib import Path
import collections,json
root=Path(__file__).resolve().parent.parent;out=root/'docs/glyph-ranking'
ref=json.loads((out/'reference.json').read_text(encoding='utf-8'));moe=ref['moe'];ud=ref['ud_gsd_dev_test']
rows=[json.loads(line) for line in (out/'candidates.jsonl').read_text(encoding='utf-8').splitlines()]
stats=collections.defaultdict(collections.Counter);pairs=[];unranked=[]
def primary(glyph,raw):
    weights=ref['rime_reading_weights'].get(glyph,{})
    def value(s):
        if s=='preset/default':return 1.0
        try:return float(s[:-1])/100 if s.endswith('%') else float(s)
        except ValueError:return 0.0
    return raw in weights and value(weights[raw])==max(map(value,weights.values()))
for row in rows:
    raw,mode=row['input'],row['mode']
    if raw not in ref['syllables']:continue
    eligible=[(i+1,c[0]) for i,c in enumerate(row['candidates']) if len(c[0])==1 and c[2]==0 and raw in ref['readings'].get(c[0],[])]
    s=stats[mode];s['syllables']+=1;s['exact_glyph_candidates']+=len(eligible)
    inversions=0
    for at,(pos,a) in enumerate(eligible):
        for later,b in eligible[at+1:]:
            if a not in moe and b in moe and moe[b]['rank']<=1000 and pos<=8:
                unranked.append({'mode':mode,'input':raw,'earlier':a,'position':pos,'later':b,'later_position':later,'later_moe_count':moe[b]['count'],'earlier_ud_count':ud.get(a,0),'later_ud_count':ud.get(b,0)})
                s['unranked_before_top1000_in_first8']+=1
            if a not in moe or b not in moe:continue
            s['ranked_pairs']+=1
            if moe[a]['count']>=moe[b]['count']:continue
            inversions+=1;s['moe_inversions']+=1
            ratio=moe[b]['count']/moe[a]['count']
            if ratio>=10:s['tenfold_inversions']+=1
            if ratio>=100:s['hundredfold_inversions']+=1
            if pos<=8 and ratio>=10:
                s['tenfold_in_first8']+=1
                if ud.get(a,0)<ud.get(b,0):
                    s['tenfold_in_first8_agree_ud']+=1
                    principal=primary(a,raw) and primary(b,raw)
                    if principal:s['tenfold_first8_both_references_primary_readings']+=1
                    pairs.append({'mode':mode,'input':raw,'earlier':a,'position':pos,'later':b,'later_position':later,'moe_ratio':ratio,
                        'both_primary_readings':principal,
                        'moe_counts':[moe[a]['count'],moe[b]['count']],'ud_counts':[ud.get(a,0),ud.get(b,0)],
                        'core_frequency':[ref['core_source_frequency'].get(g,{}).get(raw) for g in (a,b)],
                        'rime_essay_frequency':[ref['rime_essay_frequency'].get(g) for g in (a,b)],
                        'rime_reading_weights':[ref['rime_reading_weights'].get(g,{}).get(raw) for g in (a,b)]})
        s['syllables_with_inversions']+=int(inversions>0) if at==len(eligible)-1 else 0
report={'modes':dict(stats),'confirmed_pairs':sorted(pairs,key=lambda p:(p['mode'],-p['moe_ratio'],p['input'],p['position'])),'unranked_pairs':unranked,
    'limitations':'MOE school text and held-out UD frequencies are independent reference samples, not universal usage. Missing entries are unranked rather than proven rare. Pair counts are correlated and not error probabilities. Polyphonic glyph frequencies are not pronunciation-specific. Candidate collection is bounded by the shipped adapter.'}
report['core_zero_frequency_moe']=[{'glyph':g,'moe':m,'ud_count':ud.get(g,0),'rime_essay_frequency':ref['rime_essay_frequency'].get(g),'readings':ref['core_source_frequency'][g]} for g,m in moe.items() if g in ref['core_source_frequency'] and max(ref['core_source_frequency'][g].values())==0]
by_key={(r['input'],r['mode']):r for r in rows};reordered=[]
for raw in ref['syllables']:
    base=by_key[(raw,'no-packs')]['candidates'];extra=by_key[(raw,'all-packs')]['candidates']
    a=[c[0] for c in base if len(c[0])==1 and c[2]==0];b=[c[0] for c in extra if len(c[0])==1 and c[2]==0]
    for i,g in enumerate(a):
        if g not in b:continue
        for h in a[i+1:]:
            if h in b and b.index(g)>b.index(h):reordered.append({'input':raw,'base_before':g,'promoted_over_it':h})
report['addon_base_glyph_order_reversals']=reordered
(out/'summary.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
print(json.dumps(report['modes'],indent=2))
for mode in ('core','native','merged','no-packs','all-packs'):
    print(mode)
    for p in [p for p in report['confirmed_pairs'] if p['mode']==mode and p['both_primary_readings']][:6]:print(json.dumps(p,ensure_ascii=False))
print('Unranked native first-eight examples:')
for p in sorted([p for p in unranked if p['mode']=='native'],key=lambda p:(-p['later_moe_count'],p['input'],p['position']))[:8]:print(json.dumps(p,ensure_ascii=False))
