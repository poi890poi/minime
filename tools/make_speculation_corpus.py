"""All eligible CC-CEDICT entries; held-out natural spans; no success-based sampling."""
import collections,gzip,hashlib,io,json,re,unicodedata
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent;OUT=ROOT/'docs/speculation';OUT.mkdir(exist_ok=True)
def packed(path,text):
    buffer=io.BytesIO()
    with gzip.GzipFile(fileobj=buffer,mode='wb',mtime=0) as z:z.write(text.encode('utf-8'))
    path.write_bytes(buffer.getvalue())
def han(word):return bool(word) and all('CJK UNIFIED' in unicodedata.name(c,'') or 'CJK COMPATIBILITY IDEOGRAPH' in unicodedata.name(c,'') for c in word)
syllables={line.split('\t')[0] for line in (ROOT/'app/src/main/assets/syllables.tsv').read_text(encoding='utf-8').splitlines()}
production=set();readings={};vocabulary=set();sources=[]
for line in (ROOT/'app/src/main/assets/zh_tw.tsv').read_text(encoding='utf-8').splitlines():
    p=line.split('\t');production.add(p[2]);vocabulary.add(p[2]);choice=(float(p[3]),p[0].split("'"))
    if p[2] not in readings or choice[0]>readings[p[2]][0]:readings[p[2]]=choice
cedict=ROOT/'third_party/cedict/cedict.txt.gz';entries=set();rejected=collections.Counter();source_entries=0
for line in gzip.decompress(cedict.read_bytes()).decode('utf-8').splitlines():
    m=re.match(r'(\S+) (\S+) \[([^]]+)\] /(.*)/$',line)
    if not m:continue
    source_entries+=1;word,simplified,reading,definition=m.groups();vocabulary.update([word,simplified])
    if not han(word) or not 2<=len(word)<=8:rejected['non_Han_or_outside_2_to_8_characters']+=1;continue
    parts=reading.lower().replace('u:','v').replace('ü','v').split()
    if not parts or not all(re.fullmatch('[a-zv]+[1-5]',p) and p[:-1] in syllables for p in parts):rejected['unsupported_reading']+=1;continue
    parts=tuple(p[:-1] for p in parts)
    if len(parts)!=len(word):rejected['reading_glyph_count_mismatch']+=1;continue
    entries.add((word,parts))
    if word not in readings:readings[word]=(0,list(parts))
rows=[]
def add(group,identity,word,parts):
    full=''.join(parts)
    variants={'full':full,'initial':''.join(p[0] for p in parts),
              'mixed-left':''.join(p if i%2==0 else p[0] for i,p in enumerate(parts)),
              'mixed-right':''.join(p[0] if i%2==0 else p for i,p in enumerate(parts)),
              'prefix':full[:-1]}
    for style,raw in variants.items():
        if raw:rows.append((group+'-'+style,identity,raw,word))
for index,(word,parts) in enumerate(sorted(entries)):
    add('dict-seen' if word in production else 'dict-unseen',str(index),word,parts)
natural={};natural_coverage={}
for split in ['dev','test']:
    source=ROOT/('third_party/ud/UD_Chinese-GSD/zh_gsd-ud-'+split+'.conllu.gz');sources.append(source)
    spans=set();missing=0
    for block in gzip.decompress(source.read_bytes()).decode('utf-8').split('\n\n'):
        tokens=[p[1] for line in block.splitlines() for p in [line.split('\t')] if len(p)==10 and p[0].isdigit()]
        for n in [2,3]:
            for at in range(len(tokens)-n+1):
                sequence=tokens[at:at+n];word=''.join(sequence)
                if not 2<=len(word)<=12 or not all(han(t) for t in sequence):continue
                if not all(t in readings for t in sequence):missing+=1;continue
                parts=tuple(p for t in sequence for p in readings[t][1]);spans.add((word,parts))
    # Every eligible distinct span, not selected according to decoder success.
    for index,(word,parts) in enumerate(sorted(spans)):add('natural-'+split,str(index),word,parts)
    natural_coverage[split]=dict(eligible_spans=len(spans),missing_token_readings=missing)
packed(OUT/'inputs.tsv.gz',''.join('\t'.join(row)+'\n' for row in rows))
packed(OUT/'vocabulary.txt.gz',''.join(word+'\n' for word in sorted(vocabulary)))
manifest=dict(baseline='146bf1e',source_entries=source_entries,eligible_dictionary_reading_pairs=len(entries),
              unique_dictionary_targets=len({w for w,p in entries}),production_seen_pairs=sum(w in production for w,p in entries),
              vocabulary_entries=len(vocabulary),rows=len(rows),unique_queries=len({r[2] for r in rows}),groups=dict(collections.Counter(r[0] for r in rows)),
              rejected=dict(rejected),natural=natural_coverage,
              sources={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in [cedict,ROOT/'app/src/main/assets/zh_tw.tsv',ROOT/'app/src/main/assets/syllables.tsv']+sources},
              artifacts={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [OUT/'inputs.tsv.gz',OUT/'vocabulary.txt.gz']},
              limits='Dictionary membership is attestation, not semantic accuracy. Chinese variant spelling and polyphonic reading ambiguity are not normalized away. Natural readings use annotated units, never decoder outputs.')
(OUT/'corpus-manifest.json').write_bytes((json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
print(json.dumps({k:v for k,v in manifest.items() if k not in ['sources','artifacts','groups']},ensure_ascii=False,indent=2))
