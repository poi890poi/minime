"""Score frozen conversation references after lookup; never feed labels to the IME."""
from pathlib import Path
import csv,gzip,json,collections,hashlib,unicodedata
OUT=Path(__file__).resolve().parent/'conversations'
manifest=json.loads((OUT/'manifest.json').read_text(encoding='utf8'))
inputs=list(csv.reader((OUT/'inputs.tsv').open(encoding='utf8'),delimiter='\t'))
with gzip.open(OUT/'references.jsonl.gz','rt',encoding='utf8') as f:refs=[json.loads(s) for s in f]
assert hashlib.sha256((OUT/'inputs.tsv').read_bytes()).hexdigest()==manifest['input_sha256']
assert len(inputs)==len(refs)==manifest['rows']
assert all(r['row']==i for i,r in enumerate(refs))
full={tuple(p[:3]):p[4] for p in inputs if p[3]=='full'}
parts=collections.defaultdict(lambda:collections.defaultdict(set));audit=collections.defaultdict(collections.Counter)
for p,r in zip(inputs,refs):
    assert len(p)==7 and p[4] and p[5] and len(p[4])<=96
    parts[r['source']][r['role']].add(r['document'])
    a=audit[r['source']];a['conditions']+=1
    if p[3] in ['transpose','omission','neighbor']:
        a['error_controls']+=1;a['unchanged_error_controls']+=p[4]==full[tuple(p[:3])]
    if p[3]=='full':
        a['references']+=1;a['addon_target_overlap']+=r['source_target_in_production'];a[r['reference']]+=1
        if r['source'] in ['suisiann-thousand','taiwanese-basic']:
            a['has_explicit_tone_mark']+=any(unicodedata.combining(c) and c!='\u0358' for c in unicodedata.normalize('NFD',p[5]))
for source,roles in parts.items():
    assert not (roles['development'] & roles['holdout']),source

def dimensions(p,r):
    root='/'.join([r['source'],r['role'],r['unit'],p[3]])
    overlap='addon-overlap' if r['source_target_in_production'] else 'no-addon-overlap'
    tone='not-applicable' if r['source'] not in ['suisiann-thousand','taiwanese-basic'] else ('marked' if any(unicodedata.combining(c) and c!='\u0358' for c in unicodedata.normalize('NFD',p[5])) else 'unmarked')
    effective='changed' if p[4]!=full[tuple(p[:3])] else 'unchanged'
    return [root,root+'/reference:'+r['reference'],root+'/overlap:'+overlap,root+'/tone:'+tone,root+'/input:'+effective]

def flags(row):
    rank=int(row['suggestion_rank']);fold=int(row['casefold_rank'])
    return {'default':row['default_hit']=='true','fold_default':row['casefold_default_hit']=='true',
      'top3':0<rank<=3,'top8':0<rank<=8,'any':rank>0,
      'fold_top3':0<fold<=3,'fold_top8':0<fold<=8,'fold_any':fold>0,'no_alternatives':int(row['count'])<=1}

result={'input_sha256':manifest['input_sha256'],'audit':{s:dict(c) for s,c in audit.items()},
 'documents':{s:{role:len(ids) for role,ids in roles.items()} for s,roles in parts.items()},
 'metric_notes':['Top3/8 are ordinal suggestion positions excluding raw slot; no claim about physical row width.',
 'Default includes a matching literal raw slot; suggestion metrics exclude it. Complete-consumption matches only.',
 'Overlap is exact text in addons.tsv, not all generative kana outputs or all shared upstream lineage.',
 'Error controls that leave input unchanged are explicitly split; error frequencies are not measured human frequencies.',
 'This run consumes the initial holdout; it is no longer a fresh holdout for later tuning.'],
 'coverage':{},'comparisons':{}}
variants=json.loads((OUT.parent/'manifest.json').read_text(encoding='utf8'))['variants'];baseline=None;changed=[]
for variant in variants:
    with gzip.open(OUT/(variant+'.tsv.gz'),'rt',encoding='utf8') as f:rows=list(csv.DictReader(f,delimiter='\t'))
    assert len(rows)==len(inputs),(variant,len(rows))
    if baseline is None:baseline=rows
    groups=collections.defaultdict(collections.Counter);deltas=collections.defaultdict(collections.Counter)
    for i,(row,b,p,r) in enumerate(zip(rows,baseline,inputs,refs)):
        assert int(row['row'])==i
        f=flags(row);bf=flags(b)
        delta={'n':1,'order_changed':row['signature']!=b['signature'],'default_changed':row['default_text']!=b['default_text']}
        for name in f:
            if name=='no_alternatives':continue
            delta[name+'_gain']=f[name] and not bf[name];delta[name+'_loss']=bf[name] and not f[name]
        for key in dimensions(p,r):
            groups[key].update({'n':1,**{k:int(v) for k,v in f.items()}});deltas[key].update({k:int(v) for k,v in delta.items()})
        if variant!='baseline' and (delta['order_changed'] or delta['default_changed']):
            changed.append([variant,i,r['source'],r['role'],r['unit'],p[3],p[4],p[5],b['default_text'],row['default_text'],b['suggestion_rank'],row['suggestion_rank']])
    result['coverage'][variant]={k:dict(v) for k,v in groups.items()}
    result['comparisons'][variant]={k:dict(v) for k,v in deltas.items()}
    total=collections.Counter()
    for k,v in deltas.items():
        if len(k.split('/'))==4:total.update(v)
    print(variant,dict(total),flush=True)
(OUT/'results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
with gzip.open(OUT/'changed-outputs.tsv.gz','wt',encoding='utf8',newline='') as f:
    w=csv.writer(f,delimiter='\t');w.writerow(['variant','row','source','role','unit','condition','raw','target','baseline_default','variant_default','baseline_rank','variant_rank']);w.writerows(changed)
print('Input hashes, row alignment, nonempty references and source-level split disjointness verified.')
