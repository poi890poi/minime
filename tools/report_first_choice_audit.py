"""Trace first-choice glyph alerts; external frequencies never enter the runtime."""
from pathlib import Path
from collections import Counter,defaultdict
import gzip,hashlib,json,sys,unicodedata
r=Path(__file__).resolve().parent.parent;o=r/'docs/taiwan-quality'
ref=json.loads((r/'docs/glyph-ranking/reference.json').read_text(encoding='utf-8'));moe=ref['moe'];ud=ref['ud_gsd_dev_test']
def rows(path):
    with gzip.open(path,'rt',encoding='utf-8') as f:
        for line in f:yield json.loads(line)
def han(s):return len(s)==1 and ('CJK UNIFIED' in unicodedata.name(s,'') or 'CJK COMPATIBILITY IDEOGRAPH' in unicodedata.name(s,''))
def info(g):return dict(glyph=g,moe=moe.get(g),ud_count=ud.get(g,0),core_frequencies=ref['core_source_frequency'].get(g,{}),rime_essay=ref['rime_essay_frequency'].get(g),native_readings=ref['rime_reading_weights'].get(g,{}))
groups={p[0]:p[1].split(',') for p in (line.split('\t') for line in (o/'first-choice-inputs.tsv').read_text().splitlines())}
def summarize(path):
    stats=defaultdict(Counter);alerts=[];origins=Counter();batch={};first_chinese=[];first8=[]
    def inspect(batch):
        native=batch['native']['candidates'];core=batch['core']['candidates']
        for mode,row in batch.items():
            s=stats[mode];s['inputs']+=1;candidates=row['candidates']
            if not candidates:s['empty']+=1;continue
            for pos,c in enumerate(candidates[:8],1):
                g=c[0]
                if han(g) and moe.get(g,{}).get('rank',99999)>3000 and ud.get(g,0)<=1:
                    s['first8_reference_alert_occurrences']+=1
                    if c[4]:s['first8_static_addon_alert_occurrences']+=1
                    if mode=='all-packs':first8.append(dict(input=row['input'],position=pos,candidate=c,reference=info(g)))
            if mode not in ('core','native','merged'):
                first=next(((i,c) for i,c in enumerate(candidates) if c[0] and all(han(g) for g in c[0])),None)
                if first and first[0]>0 and han(first[1][0]):
                    i,c=first;g=c[0]
                    if moe.get(g,{}).get('rank',99999)>3000 and ud.get(g,0)<=1:
                        first_chinese.append(dict(input=row['input'],mode=mode,position=i+1,candidate=c,reference=info(g),preferred=row['preferred'],space=row['space'],native_first=native[:3],core_first=core[:3]))
            top=candidates[0];g=top[0]
            if not han(g):continue
            s['first_is_han_glyph']+=1
            # This is an alert, never a universal rarity assertion. Corpus absence
            # alone flags unknowns separately from low observed frequencies.
            flag='unranked' if g not in moe else 'low_reference_frequency' if moe[g]['rank']>3000 else None
            if flag is None or ud.get(g,0)>1:continue
            s['flagged_'+flag]+=1
            if mode in ('core','native','merged'):continue
            if top[4]:cause='static_addon_glyph_promotion' if any(han(c[0]) and c[2]==0 for c in native+core) else 'optional_only_no_base_competitor'
            elif native and native[0][0]==g:
                cause='native_exact_rare_only_syllable' if top[2]==0 and len(native)==1 else 'native_order'
                if top[2]>0:cause='native_prefix_consumption'
                elif row['input'] not in ref['rime_reading_weights'].get(g,{}):cause='native_correction_or_spelling_expansion'
            elif core and any(c[0]==g for c in core):cause='fallback_or_composition_order'
            else:cause='other_merge_or_presentation'
            origins[mode+'/'+cause]+=1
            alternatives=[info(c[0]) for c in candidates[1:] if han(c[0]) and c[0] in moe and moe[c[0]]['rank']<=1000][:3]
            alerts.append(dict(input=row['input'],groups=groups[row['input']],mode=mode,flag=flag,cause=cause,top=top,reference=info(g),preferred=row['preferred'],space=row['space'],native_first=native[:3],core_first=core[:3],common_reference_alternatives=alternatives))
    for row in rows(path):
        if batch and row['input']!=next(iter(batch.values()))['input']:inspect(batch);batch={}
        batch[row['mode']]=row
    if batch:inspect(batch)
    return dict(raw_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),stats={k:dict(v) for k,v in stats.items()},causes=dict(origins),alerts=alerts,first_chinese_after_other_languages=first_chinese,all_packs_first8_alerts=first8)
baseline=summarize(o/'first-choice-baseline.jsonl.gz')
report=dict(baseline=baseline,limitations='MOE school and UD counts are review signals, not universal usage or pronunciation frequencies. Unranked does not mean rare. Query groups overlap; not a sentence accuracy metric. Fresh sessions do not reproduce personal-history effects.')
if (o/'first-choice-glyph-fixed.jsonl.gz').exists():
    fixed=summarize(o/'first-choice-glyph-fixed.jsonl.gz');report['glyph_fixed']=fixed
    changes=[]
    for a,b in zip(rows(o/'first-choice-baseline.jsonl.gz'),rows(o/'first-choice-glyph-fixed.jsonl.gz')):
        assert (a['input'],a['mode'])==(b['input'],b['mode'])
        if a['candidates']!=b['candidates'] or a['preferred']!=b['preferred'] or a['space']!=b['space']:
            changes.append(dict(input=a['input'],mode=a['mode'],before_top=a['candidates'][:3],after_top=b['candidates'][:3],before_preferred=a['preferred'],after_preferred=b['preferred'],before_space=a['space'],after_space=b['space']))
    report['changes']=changes
(o/'first-choice-analysis.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
for k in ('baseline','glyph_fixed'):
    if k in report:print(k,json.dumps(dict(stats=report[k]['stats'],causes=report[k]['causes'])))
if 'changes' in report:print('changed',len(report['changes']))
