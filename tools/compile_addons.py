"""Deterministic, offline source selection; no base-model or ranking changes.

The build report distinguishes source readings from conservative dictionary-derived
title readings. Source archive hashes pin all inputs. Runtime bundles vocabulary only.
"""
from pathlib import Path
import csv, gzip, hashlib, json, re, tarfile, unicodedata, subprocess
from collections import Counter, defaultdict
from everyday_addons import append_everyday
from taiwan_entities import append_entities
from sources import require_sources
require_sources('cedict','wikidata','taiwan-encyclopedia','opencc-python','itaigi','taihoa','taiwanese-basic','jmdict','jmnedict','wanakana')

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / 'app/src/main/assets'
OUT = ROOT / 'docs/addons-learning'
rows, provenance, skipped = set(), [], []
readings = defaultdict(set)
syllables = dict(line.split('\t') for line in (ASSETS/'syllables.tsv').read_text(encoding='utf-8').splitlines())

def add(pack, key, output, source, category):
    if not key or not output or len(key)>96 or len(output)>96: return
    if any(c in key+output+source+category for c in '\t\r\n'): raise ValueError('Invalid record')
    rows.add((pack,key,output,source,category))

def han(text):
    return bool(text) and all('CJK UNIFIED' in unicodedata.name(c,'') or 'CJK COMPATIBILITY IDEOGRAPH' in unicodedata.name(c,'') for c in text)

def pinyin(value):
    value=value.lower().replace('u:','v').replace('ü','v')
    parts=value.split()
    if not parts or not all(re.fullmatch('[a-zv]+[1-5]',p) for p in parts): return None
    if not all(p[:-1] in syllables for p in parts): return None
    return parts

def chinese(word, parts, source, category):
    plain=[p[:-1] for p in parts]
    add('taiwan',"'".join(plain),word,source,category)
    if len(parts)>1:add('taiwan',''.join(p[0] for p in plain),word,source,category)
    add('taiwan',''.join(syllables[p[:-1]]+'ˉˊˇˋ˙'[int(p[-1])-1] for p in parts),word,source,category)

def derived(word):
    # Longest exact dictionary unit, but never guess a polyphonic unit.
    result=[];at=0
    while at<len(word):
        for end in range(len(word),at,-1):
            choices=readings.get(word[at:end],set())
            if len(choices)==1:
                result.extend(next(iter(choices)));at=end;break
        else:return None
    return result

cedict=gzip.open(ROOT/'third_party/cedict/cedict.txt.gz','rt',encoding='utf-8').read().splitlines()
for num,line in enumerate(cedict,1):
    m=re.match(r'(\S+) \S+ \[([^]]+)\] /(.*)/$',line)
    if not m:continue
    word,reading,definition=m.groups()
    tw=re.search(r'Taiwan pr\. \[([^]]+)\]',definition)
    parts=pinyin(tw.group(1) if tw else reading)
    if parts and han(word):readings[word].add(tuple(parts))
    if not re.search(r'\bTaiwan(?:ese)?\b|\bFormosa\b|\(Tw\)',definition,re.I):continue
    if not parts or not han(word):skipped.append(['cedict',word,'unsupported reading or non-Han headword']);continue
    # A definition mentioning a president or plant does not make its headword
    # a person or plant. Keep usage vocabulary separate from entity taxonomy.
    category='taiwan_usage'
    chinese(word,parts,'cedict:line:'+str(num),category)

# Wikidata supplies title identity; CC-CEDICT supplies only unambiguous reading units.
for kind in ('films','books','songs'):
    path=ROOT/('third_party/wikidata/taiwan-'+kind+'.json')
    if not path.exists():skipped.append(['wikidata',kind,'source unavailable']);continue
    for item in json.loads(path.read_text(encoding='utf-8'))['results']['bindings']:
        word=item['itemLabel']['value'];entity=item['item']['value'].rsplit('/',1)[-1]
        parts=derived(word) if han(word) else None
        if parts:chinese(word,parts,'wikidata:'+entity+'+cedict:derived',kind)
        else:skipped.append(['wikidata:'+entity,word,'reading needs review'])

# POJ selection is entirely data-driven: short source expressions with Mandarin
# labels, without using written Mandarin frequency to exclude conversation phrases.
# Keep all eligible source variants; no phrase/name/ID allowlist.
frequency={}
for line in (ROOT/'third_party/mcbopomofo/phrase.occ').read_text(encoding='utf-8').splitlines():
    parts=line.split()
    if len(parts)==2:frequency[parts[0]]=float(parts[1])
for item in csv.DictReader((ROOT/'third_party/itaigi/itaigi.csv').open(encoding='utf-8-sig')):
    meaning=item['HoaBun'];key=item['PojInput'].lower()
    if not re.fullmatch('[a-z0-9 -]+',key) or len(key)>96:continue
    output=unicodedata.normalize('NFC',item['PojUnicode']);source='itaigi:'+item['DictWordID']
    for alias in (key,re.sub('[1-9]','',key)):add('poj',alias,output,source,'short_vocabulary')
    provenance.append({'source':source,'meaning':meaning,'input':key,'output':output,'orthography':'POJ','contributor':item['DataProvidedBy'],'Mandarin_source_frequency':frequency.get(meaning,0)})

# Existing MIT-licensed WanaKana provides build-time Romanization.
with tarfile.open(ROOT/'third_party/jmnedict/jmnedict.json.tgz') as archive:
    data=json.load(archive.extractfile(next(m for m in archive.getmembers() if m.name.endswith('.json'))))
selected_japanese=[item for item in data['words'] if any('work' in tr['type'] or any(re.search(r'\b(Taiwan|Taipei|Tainan|Kaohsiung|singer|musician|actor|actress|animator|filmmaker|novelist|manga artist)\b',v['text'],re.I) for v in tr['translation']) for tr in item['translation'])]
kana_values=sorted({kana['text'] for item in selected_japanese for kana in item['kana']})
converted=json.loads(subprocess.check_output(['node',str(ROOT/'tools/romanize_kana.cjs')],input=json.dumps(kana_values,ensure_ascii=False).encode('utf-8')).decode('utf-8'))
for item in selected_japanese:
    for kana in item['kana']:
        roman=converted[kana['text']]['romaji'].replace(' ', '').replace('・','')
        key=converted[kana['text']]['reading'];source='jmnedict:'+item['id']
        if not re.fullmatch("[a-z']+",key):skipped.append([source,kana['text'],'unsupported kana alias']);continue
        add('japanese',key,kana['text'],source,'taiwan_and_culture')
        add('japanese',key,roman,source,'taiwan_and_culture')
        for name in item['kanji']:
            if '*' in kana['appliesToKanji'] or name['text'] in kana['appliesToKanji']:
                add('japanese',key,name['text'],source,'taiwan_and_culture')

# Full authored Taiwanese dictionary. Preserve aligned source variants; never
# use Mandarin gloss form or guessed example fragments as eligibility criteria.
taihoa_records = 0
for item in csv.DictReader((ROOT/'third_party/taihoa/taihoa.csv').open(encoding='utf-8-sig')):
    taihoa_records += 1
    source='taihoa:'+item['DictWordID']
    for suffix in ('','Others'):
        keys=item['PojInput'+suffix].split('/');outputs=item['PojUnicode'+suffix].split('/')
        if len(keys)!=len(outputs):skipped.append([source,suffix,'unaligned source variants']);continue
        for key,output in zip(keys,outputs):
            key=key.strip().lower();output=unicodedata.normalize('NFC',output.strip())
            if not key:continue
            if not re.fullmatch('[a-z0-9 -]+',key) or not output or len(key)>96 or len(output)>96:
                skipped.append([source,output,'unsupported or exceeds composition limit']);continue
            for alias in (key,re.sub('[1-9]','',key)):add('poj',alias,output,source,'extended_vocabulary')
everyday = append_everyday(add, skipped)
everyday['taihoa_source_records']=taihoa_records
encyclopedia = append_entities(add, readings, syllables)
serialized='# pack\treading\toutput\tsource\tcategory\n'+''.join('\t'.join(r)+'\n' for r in sorted(rows))
(ASSETS/'addons.tsv').write_bytes(serialized.encode('utf-8'))
sources=[]
for folder,filename,url,license_name in [
 ('cedict','cedict.txt.gz','https://www.mdbg.net/chinese/export/cedict/cedict_1_0_ts_utf-8_mdbg.txt.gz','CC-BY-SA-4.0'),
 ('jmnedict','jmnedict.json.tgz',(ROOT/'third_party/jmnedict/source-url.txt').read_text().strip(),'CC-BY-SA-4.0'),
 ('itaigi','itaigi.csv',json.loads((ROOT/'third_party/itaigi/itaigi.csv.source.json').read_text())['url'],'CC0-1.0')]:
    path=ROOT/'third_party'/folder/filename
    sources.append(dict(file=str(path.relative_to(ROOT)).replace('\\','/'),url=url,license=license_name,sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
for path in sorted((ROOT/'third_party/wikidata').glob('*.json')):
    if path.name.endswith('.source.json'):continue
    source=json.loads(path.with_name(path.name+'.source.json').read_text());source.update(file=str(path.relative_to(ROOT)).replace('\\','/'),license='CC0-1.0');sources.append(source)
for folder, filename in [('jmdict','jmdict-eng-common.json.tgz'), ('taiwanese_basic','vocabulary.csv')]:
    path=ROOT/'third_party'/folder/filename
    source=json.loads(path.with_name(path.name+'.source.json').read_text())
    assert hashlib.sha256(path.read_bytes()).hexdigest()==source['sha256']
    source['file']=str(path.relative_to(ROOT)).replace('\\','/');sources.append(source)
source=json.loads((ROOT/'third_party/taihoa/source.json').read_text(encoding='utf-8'));source['file']='third_party/taihoa/taihoa.csv';sources.append(source)
report=dict(format=1,orthography={'poj':'Pe̍h-ōe-jī; original PojUnicode/PojInput, not Tâi-lô'},
 sources=sources,asset_sha256=hashlib.sha256(serialized.encode()).hexdigest(),rows=len(rows),
 outputs_by_pack={pack:len({r[2] for r in rows if r[0]==pack}) for pack in ('taiwan','poj','japanese')},
 taiwan_category_outputs={category:len({r[2] for r in rows if r[0]=='taiwan' and r[4]==category}) for category in sorted({r[4] for r in rows if r[0]=='taiwan'})},
 poj_entries=provenance,skipped=skipped)
report['romanizer']=json.loads((ROOT/'third_party/wanakana/source.json').read_text(encoding='utf-8'))
report['everyday']=everyday
report['taiwan_encyclopedia']=encyclopedia
report['sources'].append(dict(file='third_party/taiwan_encyclopedia/snapshot.json.gz',url='https://zh.wikipedia.org/',license='CC-BY-SA-4.0',sha256=encyclopedia['snapshot_sha256']))
report['sources'].append(dict(file='third_party/taiwan_encyclopedia/entity-types.json.gz',url='https://www.wikidata.org/',license='CC0-1.0',sha256=encyclopedia['entity_types_sha256']))
report['sources'].append(dict(file='third_party/taiwan_encyclopedia/traditional-labels.json.gz',url='https://www.wikidata.org/',license='CC0-1.0',sha256=encyclopedia['traditional_labels_sha256']))
report['selection_rules']={'poj':'All supported iTaigi and Taihoa headwords/variants and beginner headwords within the 96-character limit; complete beginner examples <=6 syllables; no Mandarin gloss or frequency eligibility gate','japanese':'All JMdict source-common readings and compatible common spellings across parts of speech, plus JMnedict works, creative professions and Taiwan metadata; no entity allowlist'}
(OUT/'source-manifest.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
print(json.dumps({k:v for k,v in report.items() if k in ('rows','outputs_by_pack','taiwan_category_outputs')},ensure_ascii=False,indent=2))
print('Skipped for review:',len(skipped))
