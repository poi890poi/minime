"""Offline measurements of candidate data; retrieval coverage is not linguistic quality."""
from pathlib import Path
from collections import Counter,defaultdict
import csv,gzip,hashlib,io,json,re,unicodedata,zipfile

ROOT=Path(__file__).resolve().parent.parent
INPUT=ROOT/'artifacts/source-audit';OUT=ROOT/'docs/sources/audit-2026-09-08'

def source_bytes(name):
    if (INPUT/name).exists():return (INPUT/name).read_bytes()
    if (OUT/(name+'.gz')).exists():return gzip.decompress((OUT/(name+'.gz')).read_bytes())
    return (OUT/name).read_bytes()
def load(name):return json.loads(source_bytes(name).decode('utf8'))
def han(s):return bool(s) and all('CJK UNIFIED' in unicodedata.name(c,'') or 'CJK COMPATIBILITY IDEOGRAPH' in unicodedata.name(c,'') for c in s)
def compress(raw):
    out=io.BytesIO()
    with gzip.GzipFile(fileobj=out,mode='wb',mtime=0) as z:z.write(raw)
    return out.getvalue()

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    known=set();units=defaultdict(set)
    for p in ('zh_tw.tsv','addons.tsv','geography.tsv'):
        for line in (ROOT/'app/src/main/assets'/p).read_text(encoding='utf8').splitlines():
            if not line or line.startswith('#'):continue
            row=line.split('\t');known.add(row[2])
            if p=='zh_tw.tsv':units[row[2]].add(row[0])
    def resolvable(name):
        at=0
        while at<len(name):
            for end in range(len(name),at,-1):
                if len(units.get(name[at:end],()))==1:at=end;break
            else:return False
        return True
    def inventory(names):
        names=list(names);distinct=set(names)-{''};eligible={n for n in distinct if 2<=len(n)<=24 and han(n)}
        return dict(records=len(names),nonempty_records=sum(bool(x) for x in names),distinct_labels=len(distinct),duplicate_nonempty_records=sum(bool(x) for x in names)-len(distinct),literal_han_2_to_24=len(eligible),literal_labels_already_in_minime=len(distinct&known),eligible_labels_with_unambiguous_mcbopomofo_units=sum(resolvable(x) for x in eligible),limitations='Exact strings and unambiguous unit concatenation only; not entity identity, correct name pronunciation, ranking or common-usage accuracy.')
    report={'baseline':'MinIME 0.6.4 / 3a566a1','baseline_asset_sha256':{p:hashlib.sha256((ROOT/'app/src/main/assets'/p).read_bytes()).hexdigest() for p in ('zh_tw.tsv','addons.tsv','geography.tsv')},'sources':{}}
    samples=load('sample-records.json');planned=load('taiwan-md-sample-plan.json');inv=load('taiwan-md-inventory.json')
    paths=[e['path'] for e in inv['tree'] if e['type']=='blob' and len(e['path'].split('/'))==3 and e['path'].startswith('knowledge/') and e['path'].endswith('.md') and not Path(e['path']).name.startswith('_')]
    topical=[p for p in paths if p.split('/')[1] not in ('About','resources')]
    evidence=[];domains=Counter()
    for r in samples:
        if r['source']!='taiwan-md' or r['path'].split('/')[1] in ('About','resources'):continue
        text=r['text'];urls=re.findall(r'https?://[^\s\]\)>"\']+',text)
        from urllib.parse import urlsplit
        hosts=Counter(urlsplit(u).netloc.lower() for u in urls);domains.update(hosts)
        front=re.match(r'^---\s*\n(.*?)\n---',text,re.S)
        cited=set(re.findall(r'(?m)^\[\^([^\]]+)\]:',text));used=set(re.findall(r'\[\^([^\]]+)\](?!:)',text))
        metadata={m.group(1):m.group(2).strip().strip("\"'") for m in re.finditer(r'(?m)^([A-Za-z]+):\s*(.*)$',front.group(1) if front else '')}
        evidence.append(dict(path=r['path'],category=r['path'].split('/')[1],frontmatter=bool(front),metadata=metadata,urls=len(urls),domains=dict(hosts),undefined_footnotes=sorted(used-cited),characters=len(text),sha256=hashlib.sha256(text.encode()).hexdigest()))
    report['sources']['taiwan-md']=dict(commit=inv['commit'],commit_date=inv['date'],inventory=inventory(Path(p).stem for p in topical),category_counts=planned['category_counts'],sample_n=len(evidence),sample_seed=planned['seed'],samples_without_urls=sum(e['urls']==0 for e in evidence),samples_with_undefined_footnotes=sum(bool(e['undefined_footnotes']) for e in evidence),sample_domains=dict(domains.most_common()),sample_evidence=evidence,scope='Complete non-hub direct knowledge-folder filename inventory; eight hash-selected files per topical folder. Filenames are not canonical entity labels. Links and frontmatter are mechanical signals, not verified facts or human authorship.')
    records=[r for block in load('cip.json') for r in block['result']['records']]
    report['sources']['cip-tribes']=dict(inventory=inventory(r['部落名稱'].strip() for r in records),counties=dict(Counter(r['縣市'] for r in records)),peoples=dict(Counter(r['族別'] for r in records)),record_dates=dict(Counter(r['DateListed'] for r in records)),missing_romanization=sum(not r['部落傳統名制_羅馬拼音'].strip() for r in records),names_with_parentheses=sum(bool(re.search('[()（）]',r['部落名稱'])) for r in records),names_ending_bu_only=sum(r['部落名稱'].endswith('部') for r in records),romanization_with_non_ascii=sum(not r['部落傳統名制_羅馬拼音'].isascii() for r in records))
    with zipfile.ZipFile(io.BytesIO(source_bytes('taicol.zip'))) as z:
        rows=list(csv.reader(io.StringIO(z.read('taxon.txt').decode('utf8')),delimiter='\t'));header=rows.pop(0)
        # Column identities are pinned by meta.xml, not assumed from language labels.
        import xml.etree.ElementTree as ET
        xml=ET.fromstring(z.read('meta.xml'));ns='{http://rs.tdwg.org/dwc/text/}';fields={f.attrib['term'].rsplit('/',1)[-1]:int(f.attrib['index']) for f in xml.find(ns+'core').findall(ns+'field')}
        vern=fields['vernacularName'];status=fields['taxonomicStatus'];kingdom=fields['kingdom']
        report['sources']['taicol']=dict(inventory=inventory(r[vern].strip() for r in rows),taxonomic_status=dict(Counter(r[status] for r in rows)),kingdom=dict(Counter(r[kingdom] for r in rows)),compound_vernacular_fields=sum(bool(re.search('[,;；、]',r[vern])) for r in rows),header=header,scope='Complete v1.13 archive; vernacularName measured as exact fields, without inventing alias boundaries. Accepted taxa and synonyms are separate.')
    kem=next(x for x in samples if x['path']=='dicts/kisaragi/kisaragi-dict.org');text=kem['text'];active=False;entries=[];group='';entry=None
    for line in text.splitlines():
        if line=='* Words':active=True;continue
        if not active:continue
        if line.startswith('* '):active=False;continue
        if line.startswith('** '):group=line[3:]
        elif line.startswith('*** '):entry=dict(name=line[4:].strip(),group=group,readings=[]);entries.append(entry)
        elif line.startswith('**** ') and entry is not None:entry['readings'].append(line[5:].strip())
    ki=load('kemdict-inventory.json')
    report['sources']['kemdict-kisaragi']=dict(commit=ki['commit'],commit_date=ki['date'],inventory=inventory(e['name'] for e in entries),groups=dict(Counter(e['group'] for e in entries)),entries_with_zhuyin=sum(any(re.search('[ㄅ-ㄩ]',p) for p in e['readings']) for e in entries),entries_without_readings=sum(not e['readings'] for e in entries),scope='All completed Words headings; Todo excluded. An authored supplementary dictionary, not a representative frequency corpus.')
    music=load('music-records.json');music_rows=[];schemas=Counter();malformed=0
    for file in music:
        reader=csv.DictReader(io.StringIO(file['text'],newline=None));schemas[' | '.join(reader.fieldnames)]+=1
        for row in reader:
            if None in row or any(v is None for v in row.values()):malformed+=1
            music_rows.append(row)
    works=[(r.get('入圍作品') or r.get('得獎作品') or '').strip() for r in music_rows]
    people=[(r.get('入圍單位/者') or r.get('得獎單位/者') or '').strip() for r in music_rows]
    report['sources']['music-awards']=dict(files=len(music),rows=len(music_rows),schemas=dict(schemas),malformed_rows=malformed,years=dict(sorted(Counter(r['年度'] for r in music_rows).items())),editions=dict(sorted(Counter(r['屆別'] for r in music_rows).items())),works=inventory(works),credited_parties=inventory(people),compound_work_fields=sum(bool(re.search('[《》/／（）()]',s)) for s in works),scope='Every CSV linked in the official catalog; winners and nominations overlap. Work fields can combine songs, albums and edition labels. Credited parties include companies, not only people. No field is split into invented names.')
    (OUT/'measurements.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode())
    # Reproducible audit artifacts; these remain outside production source folders.
    fingerprints={}
    for name in ('cip.json','taiwan-md-inventory.json','taiwan-md-sample-plan.json','kemdict-inventory.json','sample-records.json','fetch-errors.json','music-catalog.json','music-records.json'):
        raw=source_bytes(name);dest=OUT/(name+'.gz');dest.write_bytes(compress(raw));fingerprints[dest.name]=dict(uncompressed_sha256=hashlib.sha256(raw).hexdigest(),sha256=hashlib.sha256(dest.read_bytes()).hexdigest())
    (OUT/'taicol.zip').write_bytes(source_bytes('taicol.zip'));fingerprints['taicol.zip']=dict(sha256=hashlib.sha256((OUT/'taicol.zip').read_bytes()).hexdigest())
    (OUT/'artifacts.json').write_bytes((json.dumps(fingerprints,indent=2)+'\n').encode())
    print(json.dumps({k:{key:v for key,v in value.items() if key in ('inventory','sample_n','entries_with_zhuyin','names_ending_bu_only','missing_romanization','samples_without_urls','samples_with_undefined_footnotes')} for k,value in report['sources'].items()},ensure_ascii=False,indent=2))

if __name__=='__main__':main()
