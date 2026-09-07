"""Freeze large evaluation-only corpora; never modify production data.

UD dev/test are independent of the train-only context asset. Chinese readings are
derived from dictionary annotations, so coverage/reading ambiguity is reported,
not hidden. Expected Chinese text always comes from UD, not decoder output.
"""
import collections, gzip, hashlib, json, re
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'docs/conversation-ranking/corpus';OUT.mkdir(exist_ok=True)
manifest=[];rows=[];coverage=[]
sources=json.loads((ROOT/'third_party/ud/sources.json').read_text(encoding='utf-8'))
readings={}
for line in (ROOT/'app/src/main/assets/zh_tw.tsv').read_text(encoding='utf-8').splitlines():
    p=line.split('\t');entry=(float(p[3]),p[0])
    if p[2] not in readings or readings[p[2]][0]<entry[0]:readings[p[2]]=entry
for lang,repo,prefix in [('en','UD_English-EWT','en_ewt'),('zh','UD_Chinese-GSD','zh_gsd')]:
    for split in ['dev','test']:
        relative=repo+'/'+prefix+'-ud-'+split+'.conllu.gz'
        data=gzip.decompress((ROOT/'third_party/ud'/relative).read_bytes())
        source=next(s for s in sources if s['path'].replace('\\','/')==relative)
        assert hashlib.sha256(data).hexdigest()==source['sha256']
        manifest.append(dict(group=lang+'-'+split,url=source['url'],sha256=source['sha256']))
        text=data.decode('utf-8');sentences=0;words=collections.Counter()
        for block in text.strip().split('\n\n'):
            tokens=[]
            if lang=='en':
                natural=next((line[9:] for line in block.splitlines() if line.startswith('# text = ')),None)
                assert natural is not None, 'Missing original sentence'
                tokens=re.findall("[a-z]+(?:'[a-z]+)*",natural.lower().replace('’',"'"))
            for line in block.splitlines():
                p=line.split('\t')
                if len(p)<2 or not p[0].isdigit():continue
                token=p[1]
                if lang=='en':
                    continue
                elif re.fullmatch('[\u3400-\u9fff]{1,8}',token):words[token]+=1
            if tokens:
                sentences+=1;raw=' '.join(tokens)
                rows.append(('en-'+split,str(sentences),raw,raw))
        if lang=='zh':
            eligible=[w for w in words if w in readings]
            # Input-independent deterministic sampling, never rank by model success.
            selected=sorted(eligible,key=lambda w:hashlib.sha256(('minime-corpus-v1:'+w).encode()).hexdigest())[:1000]
            coverage.append(dict(group='zh-'+split,source_unique=len(words),annotated_readings=len(eligible),sampled=len(selected),missing_readings=len(words)-len(eligible)))
            for word in selected:
                syllables=readings[word][1].split("'")
                spellings={'full':''.join(syllables),'initial':''.join(s[0] for s in syllables),
                           'mixed-left':''.join(s if i%2==0 else s[0] for i,s in enumerate(syllables)),
                           'mixed-right':''.join(s[0] if i%2==0 else s for i,s in enumerate(syllables))}
                for style,raw in spellings.items():rows.append(('zh-'+split+'-'+style,word,raw,word))
        else:coverage.append(dict(group='en-'+split,sentences=sentences,normalization='Original # text sentences, lowercase letter/apostrophe words; punctuation and numeric spans excluded. Natural contractions remain intact.'))
# Broad independent glyph-frequency reference from the SHINE audit, not training.
shine=ROOT.parent/'shine_aac/packages/aac-core/src/data/zh-tw-moe-glyph-frequency.generated.js'
data=shine.read_bytes();glyphs=[];all_glyphs=[]
assert hashlib.sha256(data).hexdigest()=='8f9afba41c7b653eb76b824dde638ed291d7b7ecf987ea6c0e2862885cefdf0d', 'SHINE MOE source changed; review provenance before refreezing'
for match in re.finditer(r'Object.freeze\((\[".*?"\s*,\d+,\d+\])\)',data.decode('utf-8')):
    glyph,count,rank=json.loads(match.group(1))
    all_glyphs.append(glyph)
    if rank<=1000:glyphs.append((glyph,count,rank))
assert len(all_glyphs)==4343
for glyph,count,rank in glyphs:
    if glyph in readings:rows.append(('moe-glyph',str(rank),readings[glyph][1].replace("'",''),glyph))
manifest.append(dict(group='moe-glyph',url='https://language.moe.gov.tw/001/Upload/files/SITE_CONTENT/M0001/PRIMARY/shrest2-18.htm',
    extraction='SHINE AAC b5b2de7b2eab8dc5e5b56ff97d599b5deb692f8a generated MOE entries',sha256=hashlib.sha256(data).hexdigest(),eligible=sum(g in readings for g,_,_ in glyphs),rank_limit=1000,total=len(glyphs)))
payload=''.join('\t'.join(row)+'\n' for row in rows).encode('utf-8')
(OUT/'inputs.tsv').write_bytes(payload)
(OUT/'manifest.json').write_text(json.dumps(dict(version=2,inputs_sha256=hashlib.sha256(payload).hexdigest(),rows=len(rows),sources=manifest,coverage=coverage),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(rows=len(rows),sha256=hashlib.sha256(payload).hexdigest(),coverage=coverage),ensure_ascii=False))
