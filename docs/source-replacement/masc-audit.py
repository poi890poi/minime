"""Structural corpus audit only; no language model, splits, or production writes."""
from pathlib import Path, PurePosixPath
from collections import Counter, defaultdict
import hashlib, json, re, zipfile

ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'artifacts/source-audit/masc'
EXPECTED='678a5141cf3381bedb1839c58a330507337be07c7c71603279c0ef5337032304'
def sha(data): return hashlib.sha256(data).hexdigest()
def read_archive(path):
    with zipfile.ZipFile(path) as z:
        names=z.namelist()
        assert len(names)==len(set(names))
        assert sum(i.file_size for i in z.infolist())<50_000_000
        assert all(not PurePosixPath(n).is_absolute() and '..' not in PurePosixPath(n).parts for n in names)
        return {n:z.read(n) for n in names if not n.endswith('/')}

tagged_path=CACHE/'masc_tagged.zip';raw_path=CACHE/'masc_500k_texts.zip'
assert sha(tagged_path.read_bytes())==EXPECTED
tagged=read_archive(tagged_path);raw=read_archive(raw_path)
categories={}
for line in tagged['masc_tagged/categories.txt'].decode('utf-8').splitlines():
    name,genre=line.split(' ',1);assert name not in categories;categories[name]=genre
raw_by_name={};duplicates=[]
for name,data in raw.items():
    if not name.endswith('.txt'):continue
    key=PurePosixPath(name).name
    assert key not in raw_by_name
    raw_by_name[key]=(name,data.decode('utf-8-sig'))

genre_counts=defaultdict(Counter);records=[];signatures=defaultdict(list)
for name,data in sorted(tagged.items()):
    if name=='masc_tagged/categories.txt':continue
    key=name.removeprefix('masc_tagged/');genre=categories.get(key,'UNLISTED')
    text=data.decode('utf-8');parsed=[t.rsplit('_',1) for t in text.split()]
    malformed=sum(len(t)!=2 for t in parsed)
    words=[t[0] for t in parsed if len(t)==2]
    matched=raw_by_name.get(PurePosixPath(name).name)
    row=dict(id=key,genre=genre,sha256=sha(data),tagged_tokens=len(words),malformed_tokens=malformed,
             apostrophe_tokens=sum("'" in t or '’' in t for t in words),
             separate_apostrophe_tokens=sum(t.startswith(("'",'’')) for t in words))
    if matched:
        raw_name,original=matched
        raw_signature=re.sub(r'\s+','',original)
        row.update(raw_id=raw_name,raw_sha256=sha(raw[raw_name]),raw_characters=len(original),
                   raw_apostrophes=original.count("'")+original.count('’'),
                   exact_nonwhitespace_text_agreement=''.join(words)==raw_signature)
        signatures[sha(original.casefold().encode('utf-8'))].append(key)
    records.append(row)
    genre_counts[genre].update(documents=1,tokens=len(words),malformed_tokens=malformed,
                                apostrophe_tokens=row['apostrophe_tokens'],
                                raw_pairs=int(bool(matched)),
                                exact_nonwhitespace_pairs=int(row.get('exact_nonwhitespace_text_agreement',False)))

manifest=dict(format=1,role='discovery-only; no train/development/test split or model evaluation',
    tagged_revision='550b6625bcef1f2abff2ff770a5a0d272c9c6b2a',
    archives={p.name:dict(bytes=p.stat().st_size,sha256=sha(p.read_bytes())) for p in [tagged_path,raw_path]},
    package_notice_sha256=sha((CACHE/'package.xml').read_bytes()),
    transport='Tagged archive and notice: verified TLS GitHub mirror with index hash match. Raw archive: publisher HTTPS certificate expired; certificate validation bypassed only for this quarantined public-data request. No production admission.',
    document_count=len(records),category_entries=len(categories),unlisted_documents=[r['id'] for r in records if r['genre']=='UNLISTED'],
    genres={k:dict(v) for k,v in sorted(genre_counts.items())},
    casefolded_whole_text_duplicate_groups=[v for v in signatures.values() if len(v)>1],
    raw_notices=[n for n in raw if any(s in n.lower() for s in ['license','readme','copying'])],
    tagged_notices=[n for n in tagged if any(s in n.lower() for s in ['license','readme','copying'])],
    documents=records)
destination=ROOT/'docs/source-replacement/masc-manifest.json'
destination.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in manifest.items() if k!='documents'},ensure_ascii=False,indent=2))
