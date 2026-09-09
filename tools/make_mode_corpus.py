"""Freeze document-separated genre inputs; labels never enter the decoder."""
from pathlib import Path
from collections import defaultdict,Counter
import gzip,hashlib,json,re
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'docs/input-modes';OUT.mkdir(exist_ok=True)
SEED='mode-coverage-20260909-v1'
def order(value):return hashlib.sha256((SEED+':'+value).encode()).digest()
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
rows=[];inventory={};sources={};excluded=Counter()
for name in ('app/src/main/assets/syllables.tsv','app/src/main/assets/addons.tsv'):
    sources[name]=digest(ROOT/name)
def add(group,doc,identity,raw,target,context='',units=None):
    forms=[('full',raw),('half',raw[:max(1,len(raw)//2)].rstrip("'"))]
    if units:forms.extend([('initials',''.join(s[0] for s in units)),('mixed',''.join(s if i%2 else s[0] for i,s in enumerate(units)))])
    if len(raw)>2:
        index=int.from_bytes(order(identity)[:4],'big')%(len(raw)-1)
        forms.append(('transpose',raw[:index]+raw[index+1]+raw[index]+raw[index+2:]))
    for condition,value in forms:
        if value:rows.append((group,doc,identity,condition,value,target,context))
# GUM train/dev have not trained MinIME. Freeze whole source documents first.
documents=defaultdict(list)
for split in ('train','dev'):
    path=ROOT/('artifacts/mode-corpus/en_gum-ud-'+split+'.conllu');sources[str(path.relative_to(ROOT))]=digest(path)
    for block in path.read_text(encoding='utf8').split('\n\n'):
        identity=re.search(r'^# sent_id = (.+)$',block,re.M);text=re.search(r'^# text = (.+)$',block,re.M)
        if not identity or not text:continue
        identity=identity.group(1);doc=identity.rsplit('-',1)[0]
        genre=doc.split('_')[1]
        if genre not in ('conversation','essay'):continue
        words=re.findall("[a-z]+(?:'[a-z]+)*",text.group(1).lower().replace('’',"'"))
        for i,word in enumerate(words):
            if 2<=len(word)<=24:documents[(genre,doc)].append((identity+':'+str(i),word,' '.join(words[max(0,i-2):i])))
for genre in ('conversation','essay'):
    docs=sorted([d for g,d in documents if g==genre],key=order)[:8]
    inventory['en-'+genre]={'available_documents':len([1 for g,d in documents if g==genre]),'selected_documents':docs,'eligible_occurrences':sum(len(documents[(genre,d)]) for d in docs)}
    for doc in docs:
        for identity,word,context in sorted(documents[(genre,doc)],key=lambda r:order(r[0]))[:32]:
            add('en-'+genre,doc,identity,word.replace("'",''),word,context)
# Independent reference readings from original McBopomofo source, no decoder or scores.
syllables={p[1]:p[0] for line in (ROOT/'app/src/main/assets/syllables.tsv').read_text(encoding='utf8').splitlines() for p in [line.split('\t')]}
readings=defaultdict(set)
for name in ('BPMFBase.txt','BPMFMappings.txt'):
    path=ROOT/'third_party/mcbopomofo'/name;sources[str(path.relative_to(ROOT))]=digest(path)
    for line in path.read_text(encoding='utf8').splitlines():
        p=line.split()
        if len(p)<2 or not re.fullmatch('[\u3400-\u9fff]+',p[0]):continue
        bpmf=p[1:2] if name=='BPMFBase.txt' else p[1:]
        units=[syllables.get(re.sub('[ˊˇˋ˙ˉ]','',b)) for b in bpmf]
        if all(units):readings[p[0]].add(tuple(units))
def reading(text):
    if len(readings[text])==1:return next(iter(readings[text]))
    if all(len(readings[c])==1 for c in text):return tuple(s for c in text for s in next(iter(readings[c])))
    return None
path=ROOT/'docs/sources/audit-2026-09-08/sample-records.json.gz';sources[str(path.relative_to(ROOT))]=digest(path)
articles=[r for r in json.loads(gzip.decompress(path.read_bytes())) if r['source']=='taiwan-md' and r['path'].startswith('knowledge/') and r['path'].split('/')[1] not in ('About','resources')]
categories=defaultdict(list)
for article in articles:categories[article['path'].split('/')[1]].append(article)
inventory['zh-essay']={}
for category,values in sorted(categories.items()):
    chosen=sorted(values,key=lambda r:order(r['path']))[:2]
    inventory['zh-essay'][category]=[r['path'] for r in chosen]
    for article in chosen:
        text=re.sub(r'^---.*?---\s*','',article['text'],flags=re.S)
        # Prose lines only: omit headings, lists, tables, code, image/link destinations.
        text='\n'.join(line for line in text.splitlines() if line.strip() and not re.match(r'^\s*(?:[#>|*`-]|\d+\.)',line))
        text=re.sub(r'\[([^\]]+)\]\([^)]*\)',r'\1',text)
        candidates=[]
        for i,match in enumerate(re.finditer(r'[\u3400-\u9fff]+',text)):
            target=match.group()
            if not 2<=len(target)<=12:excluded['zh-essay/outside-2-12-glyph-window']+=1;continue
            units=reading(target)
            if not units:excluded['zh-essay/ambiguous-or-missing-reference-reading']+=1;continue
            excluded['zh-essay/eligible-reference-spans-before-sampling']+=1
            candidates.append((article['path']+':'+str(i),target,units))
        for identity,target,units in sorted(candidates,key=lambda r:order(r[0]))[:16]:
            add('zh-essay',article['path'],identity,''.join(units),target,units=units)
# Specialist packs: seen-source retrieval only, never relabel as conversation/essay.
packrows=defaultdict(list)
for line in (ROOT/'app/src/main/assets/addons.tsv').read_text(encoding='utf8').splitlines():
    p=line.split('\t')
    if len(p)==5 and p[0] in ('poj','japanese') and p[4].startswith('everyday_'):
        key=re.sub("[' -]",'',p[1])
        if re.fullmatch('[a-z]{2,24}',key):packrows[p[0]].append((p[3]+':'+p[1],key,p[2]))
for pack,values in sorted(packrows.items()):
    seen=set()
    for identity,raw,target in sorted(values,key=lambda r:order(r[0])):
        if (raw,target) in seen:continue
        seen.add((raw,target));add(pack+'-retrieval',pack,identity,raw,target)
        if len(seen)==128:break
payload=''.join('\t'.join(row)+'\n' for row in rows).encode()
(OUT/'coverage-inputs.tsv').write_bytes(payload)
manifest=dict(seed=SEED,rows=len(rows),groups=dict(Counter(r[0] for r in rows)),documents=inventory,sources=sources,exclusions=dict(excluded),sha256=hashlib.sha256(payload).hexdigest(),scope='GUM train/dev documents are newly acquired local evaluation holdouts, never MinIME training. Taiwan.md essays were previously audited. Chinese reading eligibility uses unambiguous original McBopomofo annotations (shared lexical lineage, no decoder-generated gold). Dictionary probes are seen-source retrieval. No production or ranking changes were selected from outputs.',missing='No licensed accessible Taiwan Mandarin conversation corpus acquired: NCCU public endpoint returns site suspended; Sinica TMC requires an approved account. No independent POJ/Japanese conversation or essay corpus is established. Do not infer these missing genre/language results.')
manifest['gum_upstream']='https://github.com/UniversalDependencies/UD_English-GUM/tree/34d01cb603867d0c085896e8286a0fe01229fa37'
(OUT/'coverage-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'rows':len(rows),'groups':manifest['groups'],'exclusions':dict(excluded)},indent=2))
