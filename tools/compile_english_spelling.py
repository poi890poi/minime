"""Apostrophe safety metadata from complete AOSP vocabulary and UD TRAIN tags.

No evaluation text or handpicked words participate. This does not add rare words
to completion lists or alter the existing language-model frequencies.
"""
import gzip, hashlib, json, re
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
ASSETS=ROOT/'app/src/main/assets'
aosp=ROOT/'third_party/aosp/en_US_wordlist.combined.gz'
ud=ROOT/'third_party/ud/UD_English-EWT/en_ewt-ud-train.conllu.gz'
words=set()
for line in gzip.decompress(aosp.read_bytes()).decode('utf-8').splitlines():
    match=re.match(r' word=([^,]+),f=(\d+)',line)
    if match and re.fullmatch('[A-Za-z]+',match[1]) and 'not_a_word=true' not in line:
        words.add(match[1].lower())
apostrophes={line.split('\t')[0].lower() for line in (ASSETS/'en_us.tsv').read_text(encoding='utf-8').splitlines() if "'" in line.split('\t')[0]}
for line in (ASSETS/'context.tsv').read_text(encoding='utf-8').splitlines():
    language,context,word,count=line.split('\t')
    if language=='en' and not context and re.fullmatch("[a-z]+'[a-z]+",word) and int(count)>=2:apostrophes.add(word)
valid=words & {word.replace("'",'') for word in apostrophes}
contractions=set()
for sentence in gzip.decompress(ud.read_bytes()).decode('utf-8').split('\n\n'):
    tokens={};spans=[]
    for line in sentence.splitlines():
        p=line.split('\t')
        if len(p)!=10:continue
        if p[0].isdigit():tokens[int(p[0])]=p
        elif re.fullmatch(r'\d+-\d+',p[0]):spans.append(p)
    for span in spans:
        word=span[1].lower().replace('’',"'")
        if word not in apostrophes:continue
        start,end=map(int,span[0].split('-'))
        # AUX expansion distinguishes grammatical contractions from possessives
        # and letter plurals, which are especially ambiguous with Pinyin initials.
        if any(tokens[i][3]=='AUX' for i in range(start,end+1) if i in tokens):contractions.add(word)
data=''.join('valid\t'+word+'\n' for word in sorted(valid))+''.join('contraction\t'+word+'\n' for word in sorted(contractions))
target=ASSETS/'en_spelling.tsv';target.write_bytes(data.encode('utf-8'))
report=dict(valid_bare_spellings=len(valid),grammatical_contractions=len(contractions),bytes=len(data.encode('utf-8')),
            rule='Complete AOSP ASCII vocabulary intersected with apostrophe-stripped model words; UD EWT TRAIN multiword tokens containing an AUX expansion intersected with model words.',
            sources={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in [aosp,ud,ASSETS/'en_us.tsv',ASSETS/'context.tsv']},
            asset_sha256=hashlib.sha256(target.read_bytes()).hexdigest())
(ROOT/'docs/dictionary-impact/spelling-sources.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
print(json.dumps(report,indent=2))
