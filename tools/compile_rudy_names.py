"""Systematic Rudy/OSM POI import. No entity list, manual spellings or test inputs.

Dataset categories determine scope. Prefer upstream pronunciation tags; otherwise
join unambiguous licensed McBopomofo units. Keep this ODbL-derived pack separate
from CC-BY-SA language packs. No per-name article links are maintained.
"""
from pathlib import Path
from sources import require_sources
require_sources('rudy','mcbopomofo')
import collections,gzip,hashlib,io,json,re,shutil,sqlite3,unicodedata,zipfile,sys
ROOT=Path(__file__).resolve().parent.parent
SOURCE=ROOT/'third_party/rudy'
ASSETS=ROOT/'app/src/main/assets'
REPORT=ROOT/'docs/addons-learning'

def han(value):
    return 2<=len(value)<=32 and all('CJK UNIFIED' in unicodedata.name(c,'') or 'CJK COMPATIBILITY IDEOGRAPH' in unicodedata.name(c,'') for c in value)

def fold_pinyin(value):
    value=value.lower().replace('ü','v').replace('u:','v')
    # Marked u-diaeresis may also carry a tone. Preserve that vowel as v.
    value=unicodedata.normalize('NFD',value).replace('u\u0308','v')
    value=''.join(c for c in value if not unicodedata.combining(c))
    value=re.sub('[1-5]','',value).replace('’',"'")
    return value if re.fullmatch("[a-zv '\-]+",value) else None

readings=collections.defaultdict(set);frequency={}
for line in (ASSETS/'zh_tw.tsv').read_text(encoding='utf-8').splitlines():
    p=line.split('\t');readings[p[2]].add((p[0],p[4]));frequency[p[2]]=max(frequency.get(p[2],0),float(p[3]))
def derive(word):
    if word in readings:return sorted(readings[word])
    result=[];at=0
    while at<len(word):
        for end in range(len(word),at,-1):
            candidates=readings.get(word[at:end],set())
            if len(candidates)==1:result.append(next(iter(candidates)));at=end;break
        else:return []
    return [("'".join(r[0] for r in result),''.join(r[1] for r in result))]

def extract():
    metadata=json.loads((SOURCE/'source.json').read_text(encoding='utf-8'))
    assert hashlib.sha256((SOURCE/'rudy-poi.zip').read_bytes()).hexdigest()==metadata['sha256']
    db=ROOT/'artifacts/rudy-poi.sqlite'
    with zipfile.ZipFile(SOURCE/'rudy-poi.zip') as archive:
        member=archive.getinfo('MOI_OSM_Taiwan_TOPO_Rudy.poi')
        with archive.open(member) as src,db.open('wb') as target:shutil.copyfileobj(src,target)
    connection=sqlite3.connect('file:'+db.as_posix()+'?mode=ro',uri=True)
    categories={r[0]:(r[1],r[2]) for r in connection.execute('select * from poi_categories')}
    category_scope={'登山相關','自然','地點','水路','歷史遺跡'}
    roots={cid for cid,(name,parent) in categories.items() if re.sub(r'^\d+\s*','',name) in category_scope}
    assert len(roots)==len(category_scope),'Upstream category schema changed; review the importer'
    selected=set(roots)
    while True:
        added={cid for cid,(name,parent) in categories.items() if parent in selected}-selected
        if not added:break
        selected|=added
    ids={r[0] for r in connection.execute('select id from poi_category_map where category in ('+','.join('?' for _ in selected)+')',sorted(selected))}
    names=collections.defaultdict(lambda:dict(pinyin=set(),bpmf=set()))
    rejected=collections.Counter();origin=collections.Counter();category_counts={}
    for cid in sorted(selected):category_counts[categories[cid][0]]=connection.execute('select count(*) from poi_category_map where category=?',(cid,)).fetchone()[0]
    fields=('name:zh-hant-tw','name:zh-hant','name:zh','name')
    aliases=('alt_name:zh-hant','alt_name:zh','alt_name','old_name:zh-hant','old_name:zh','old_name','official_name:zh','official_name','short_name:zh','short_name')
    for identifier,data in connection.execute('select id,data from poi_data'):
        if identifier not in ids:continue
        tags=dict(part.split('=',1) for part in data.split('\r') if '=' in part)
        osm=tags.get('osm_id','')
        if not re.fullmatch('[PWR]/[0-9]+',osm):rejected['not_an_osm_record']+=1;continue
        origin[osm.split('/')[0]]+=1
        primary=next((tags[key] for key in fields if tags.get(key)),None)
        values=set()
        if primary:values.update(primary.split(';'))
        for field in aliases:
            if field in tags:values.update(tags[field].split(';'))
        if not values:rejected['unnamed_record']+=1;continue
        for value in values:
            value=unicodedata.normalize('NFC',value).strip()
            if not han(value):rejected['non_Han_or_outside_length_limit']+=1;continue
            record=names[value]
            # Do not apply the current-name pronunciation to an old/alternative name.
            if value==primary:
                for key in ('name:zh-latn-pinyin','name:cmn-latn-pinyin','name:cmn-latn-tw-pinyin'):
                    if tags.get(key):
                        folded=fold_pinyin(tags[key])
                        if folded:record['pinyin'].add(folded)
                if tags.get('name:zh-bopo'):
                    bpmf=re.sub(r'\s+','',tags['name:zh-bopo'])
                    if re.fullmatch('[\u3105-\u3129ˉˊˇˋ˙]+',bpmf):record['bpmf'].add(bpmf)

    summary=dict(source=metadata,database_metadata=dict(connection.execute('select * from metadata')),source_records=connection.execute('select count(*) from poi_data').fetchone()[0],selected_records=len(ids),selected_categories=category_counts,osm_origins=dict(origin),name_fields=fields,alias_fields=aliases,rejected=dict(rejected))
    plain={word:{key:sorted(value) for key,value in record.items()} for word,record in sorted(names.items())}
    connection.close()
    return dict(extractor_version=1,summary=summary,names=plain)

snapshot=SOURCE/'extracted-names.json.gz'
if '--extract' in sys.argv or not snapshot.exists():
    extracted=extract();buffer=io.BytesIO()
    with gzip.GzipFile(fileobj=buffer,mode='wb',mtime=0) as archive:archive.write((json.dumps(extracted,ensure_ascii=False,separators=(',',':'))+'\n').encode('utf-8'))
    snapshot.write_bytes(buffer.getvalue())
extracted=json.loads(gzip.decompress(snapshot.read_bytes()).decode('utf-8'))
assert extracted['extractor_version']==1
names=extracted['names'];summary=extracted['summary']
metadata=json.loads((SOURCE/'source.json').read_text(encoding='utf-8'))
assert summary['source']['sha256']==metadata['sha256']
rows=set();skipped=[];reading_methods=collections.Counter()
version=summary['database_metadata']['comment'].split('/')[0].strip()
def add(key,word,method):
    if key and len(key)<=96:rows.add(('geography',key,word,'rudy-'+version+':'+method,'geography_history'))
syllables={line.split('\t')[0] for line in (ASSETS/'syllables.tsv').read_text(encoding='utf-8').splitlines()}
for word,data in sorted(names.items()):
    if data['pinyin'] or data['bpmf']:
        reading_methods['upstream_tags']+=1
        for key in data['pinyin']:
            add(key,word,'upstream');parts=re.split("[ '\-]+",key)
            if all(p in syllables for p in parts) and len(parts)>1:add(''.join(p[0] for p in parts),word,'upstream')
        for key in data['bpmf']:add(key,word,'upstream')
    else:
        found=derive(word)
        if not found:skipped.append(word);continue
        reading_methods['mcbopomofo_exact' if word in readings else 'mcbopomofo_unambiguous_units']+=1
        for key,bpmf in found:
            add(key,word,'mcbopomofo-derived');add(bpmf,word,'mcbopomofo-derived')
            if "'" in key:add(''.join(p[0] for p in key.split("'")),word,'mcbopomofo-derived')
# Source unigram frequency is a general tie-break, never POI segment counts or a
# manually preferred mountain. Unknown labels retain deterministic lexical order.
ordered=sorted(rows,key=lambda r:(r[0],r[1],-frequency.get(r[2],0),r[2],r[3]))
payload='# pack\treading\toutput\tdataset\tcategory\n'+''.join('\t'.join(r)+'\n' for r in ordered)
(ASSETS/'geography.tsv').write_bytes(payload.encode('utf-8'))
report=dict(summary,eligible_Han_names=len(names),reading_methods=dict(reading_methods),included_names=len({r[2] for r in rows}),pinyin_names=len({r[2] for r in rows if re.fullmatch("[a-zv '\-]+",r[1])}),rows=len(rows),unavailable_readings=len(skipped),asset_sha256=hashlib.sha256(payload.encode()).hexdigest(),extracted_snapshot_sha256=hashlib.sha256(snapshot.read_bytes()).hexdigest(),reading_source_sha256=hashlib.sha256((ASSETS/'zh_tw.tsv').read_bytes()).hexdigest(),rules='All descendants of named thematic categories; NFC/trim; semicolon aliases; 2-32 Han glyphs; upstream Pinyin/Zhuyin or exact/unambiguous McBopomofo units; no entity allowlist or per-name links')
(REPORT/'rudy-manifest.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
buffer=io.BytesIO()
with gzip.GzipFile(fileobj=buffer,mode='wb',mtime=0) as archive:archive.write(('\n'.join(skipped)+'\n').encode('utf-8'))
(REPORT/'rudy-unavailable-readings.txt.gz').write_bytes(buffer.getvalue())
print(json.dumps({k:v for k,v in report.items() if k in ('source_records','selected_records','eligible_Han_names','reading_methods','included_names','rows','unavailable_readings','rejected')},ensure_ascii=False,indent=2))
