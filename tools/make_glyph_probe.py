"""Freeze independent frequency references; never feed their ranks to decoding."""
from pathlib import Path
from collections import Counter,defaultdict
import gzip,hashlib,json,re,zipfile
root=Path(__file__).resolve().parent.parent;out=root/'docs/glyph-ranking';out.mkdir(exist_ok=True)
shine=root.parent/'shine_aac/packages/aac-core/src/data/zh-tw-moe-glyph-frequency.generated.js'
data=shine.read_bytes();assert hashlib.sha256(data).hexdigest()=='8f9afba41c7b653eb76b824dde638ed291d7b7ecf987ea6c0e2862885cefdf0d'
moe={}
for m in re.finditer(r'Object.freeze\((\[".*?"\s*,\d+,\d+\])\)',data.decode('utf-8')):
    glyph,count,rank=json.loads(m.group(1));moe[glyph]={'count':count,'rank':rank}
assert len(moe)==4343
ud=Counter();sources={str(shine):hashlib.sha256(data).hexdigest()}
for split in ('dev','test'):
    path=root/'third_party/ud/UD_Chinese-GSD'/('zh_gsd-ud-'+split+'.conllu.gz')
    sources[str(path.relative_to(root)).replace('\\','/')]=hashlib.sha256(path.read_bytes()).hexdigest()
    for line in gzip.decompress(path.read_bytes()).decode('utf-8').splitlines():
        p=line.split('\t')
        if len(p)>1 and p[0].isdigit():ud.update(c for c in p[1] if '\u3400'<=c<='\u9fff')
readings=defaultdict(set);priors=defaultdict(dict)
path=root/'app/src/main/assets/zh_tw.tsv';sources[str(path.relative_to(root)).replace('\\','/')]=hashlib.sha256(path.read_bytes()).hexdigest()
for line in path.read_text(encoding='utf-8').splitlines():
    p=line.split('\t')
    if len(p[2])==1 and re.fullmatch('[a-zv]+',p[0]):readings[p[2]].add(p[0]);priors[p[2]][p[0]]=float(p[3])
path=root/'third_party/cedict/cedict.txt.gz';sources[str(path.relative_to(root)).replace('\\','/')]=hashlib.sha256(path.read_bytes()).hexdigest()
for line in gzip.open(path,'rt',encoding='utf-8'):
    m=re.match(r'(\S) \S+ \[([a-zA-Z:]+)[1-5]\]',line)
    if m:readings[m[1]].add(m[2].lower().replace('u:','v'))
syllables=sorted({line.split('\t')[0] for line in (root/'app/src/main/assets/syllables.tsv').read_text(encoding='utf-8').splitlines()})
queries=sorted(set(syllables)|{s[:i] for s in syllables for i in range(1,len(s))})
(out/'inputs.txt').write_bytes(('\n'.join(queries)+'\n').encode())
rime_weights=defaultdict(dict);essay={}
for archive,suffix in [('rime-luna-pinyin.zip','/luna_pinyin.dict.yaml'),('rime-essay.zip','/essay.txt')]:
    path=root/'third_party/rime/data-sources'/archive;sources[str(path.relative_to(root)).replace('\\','/')]=hashlib.sha256(path.read_bytes()).hexdigest()
    with zipfile.ZipFile(path) as z:text=z.read(next(n for n in z.namelist() if n.endswith(suffix))).decode('utf-8')
    for line in text.splitlines():
        p=line.split('\t')
        if len(p)<2 or len(p[0])!=1:continue
        if suffix.endswith('essay.txt'):
            if p[1].isdigit():essay[p[0]]=int(p[1])
        elif re.fullmatch('[a-zv]+',p[1]):
            readings[p[0]].add(p[1]);rime_weights[p[0]][p[1]]=p[2] if len(p)>2 else 'preset/default'
reference={'moe':moe,'ud_gsd_dev_test':dict(ud),'readings':{k:sorted(v) for k,v in readings.items()},'core_source_frequency':priors,'rime_reading_weights':rime_weights,'rime_essay_frequency':essay,'syllables':syllables}
(out/'reference.json').write_bytes((json.dumps(reference,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
manifest={'inputs':len(queries),'complete_syllables':len(syllables),'moe_reference_glyphs':len(moe),'ud_reference_glyphs':len(ud),'sources':sources,
    'moe_source':'https://language.moe.gov.tw/001/Upload/files/SITE_CONTENT/M0001/PRIMARY/shrest2-18.htm',
    'contract':'All supported syllables and every proper prefix. Fresh native sessions, no user learning. References only score outputs afterward. Compare core, native, merge, composition without packs, and composition with all packs. School-corpus frequency is not universal frequency; missing reference glyphs are unranked, not proven rare.'}
for name in ('inputs.txt','reference.json'):manifest[name+'_sha256']=hashlib.sha256((out/name).read_bytes()).hexdigest()
(out/'manifest.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
print(json.dumps({k:v for k,v in manifest.items() if k!='sources'}))
