"""Authenticate original MASC text bytes against the publisher's pinned Git tree.

Discovery only: no training, splits, model calls, production writes or scripts run.
--fetch obtains only text files absent from the byte-verified local audit cache.
"""
import argparse,collections,hashlib,json,re,urllib.parse,urllib.request,zipfile
from pathlib import Path,PurePosixPath

ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'artifacts/source-audit/masc'
REVISION='7a718b01af317f7ed8e330b0edd8db30c162da24'
PREFIX='metadata/FULL_MASC/'
BASE='https://raw.githubusercontent.com/oanc/masc/'+REVISION+'/'

def sha(data):return hashlib.sha256(data).hexdigest()
def git_blob(data):return hashlib.sha1(b'blob '+str(len(data)).encode('ascii')+b'\0'+data).hexdigest()
def fetch(url,limit):
    with urllib.request.urlopen(url,timeout=45) as response:data=response.read(limit+1)
    if len(data)>limit:raise ValueError('Source response exceeds audit limit')
    return data

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--fetch',action='store_true');args=parser.parse_args()
    CACHE.mkdir(parents=True,exist_ok=True)
    if args.fetch:
        metadata={'publisher-commit.json':'https://api.github.com/repos/oanc/masc/commits/'+REVISION,
                  'publisher-tree.json':'https://api.github.com/repos/oanc/masc/git/trees/97d8b0b9ab6575d5f0a88ab08b30b3a43bbbe45f?recursive=1',
                  'publisher-resource-header.xml':BASE+'metadata/MASC3-resource-header.xml'}
        for name,url in metadata.items():
            if not (CACHE/name).exists():(CACHE/name).write_bytes(fetch(url,2_000_000))
    tree=json.loads((CACHE/'publisher-tree.json').read_text(encoding='utf-8-sig'))
    commit=json.loads((CACHE/'publisher-commit.json').read_text(encoding='utf-8-sig'))
    assert commit['sha']==REVISION and commit['commit']['tree']['sha']==tree['sha'] and not tree['truncated']
    header=next(p for p in tree['tree'] if p['path']=='metadata/MASC3-resource-header.xml')
    header_bytes=(CACHE/'publisher-resource-header.xml').read_bytes();assert git_blob(header_bytes)==header['sha']
    files=[p for p in tree['tree'] if p['type']=='blob' and p['path'].startswith(PREFIX) and p['path'].endswith('.txt')]
    assert len(files)==393 and sum(p['size'] for p in files)<5_000_000
    with zipfile.ZipFile(CACHE/'masc_500k_texts.zip') as z:
        raw={}
        for name in z.namelist():
            if name.endswith('.txt'):
                key=PurePosixPath(name).name;assert key not in raw;raw[key]=z.read(name)
    with zipfile.ZipFile(CACHE/'masc_tagged.zip') as z:
        tagged={PurePosixPath(n).name:z.read(n).decode('utf-8') for n in z.namelist() if n.endswith('.txt') and not n.endswith('categories.txt')}
    records=[];counts=collections.Counter();genres=collections.defaultdict(collections.Counter)
    for item in sorted(files,key=lambda p:p['path']):
        relative=PurePosixPath(item['path'][len(PREFIX):]);assert not relative.is_absolute() and '..' not in relative.parts
        destination=CACHE/'publisher-texts'/str(relative)
        assert destination.resolve().is_relative_to((CACHE/'publisher-texts').resolve())
        cached=raw.get(relative.name);archive_match=cached is not None and git_blob(cached)==item['sha']
        if destination.exists():data=destination.read_bytes()
        elif archive_match:data=cached
        elif args.fetch:
            data=fetch(BASE+urllib.parse.quote(item['path']),1_000_000)
        else:raise RuntimeError('Run --fetch for missing publisher text: '+item['path'])
        assert len(data)==item['size'] and git_blob(data)==item['sha'],item['path']
        text=data.decode('utf-8-sig');destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes(data)
        genre='/'.join(relative.parts[:-1]);counts['documents']+=1;counts['archive_git_blob_matches']+=archive_match
        counts['archive_missing']+=cached is None;counts['archive_different']+=cached is not None and not archive_match
        row=dict(path=item['path'],git_blob=item['sha'],sha256=sha(data),bytes=len(data),genre=genre,
                 archive_comparison='exact-git-blob-match' if archive_match else 'missing' if cached is None else 'different',
                 apostrophes=text.count("'")+text.count('’'))
        if relative.name in tagged:
            units=[t.rsplit('_',1) for t in tagged[relative.name].split()]
            row['tagged_nonwhitespace_agreement']=''.join(p[0] for p in units if len(p)==2)==re.sub(r'\s+','',text)
            counts['tagged_pairs']+=1;counts['tagged_nonwhitespace_agreement']+=row['tagged_nonwhitespace_agreement']
        genres[genre].update(documents=1,bytes=len(data),apostrophes=row['apostrophes']);records.append(row)
    manifest=dict(format=1,role='discovery-only; no model, split or production use',repository='https://github.com/oanc/masc',
        revision=REVISION,tree=tree['sha'],resource_header_sha256=sha(header_bytes),selection='Every .txt blob below metadata/FULL_MASC/; no lexical/genre selection.',
        authentication='Publisher commit/tree obtained over verified HTTPS. Every text is verified against its exact Git blob hash; unmatched texts fetched from pinned publisher HTTPS URL. Earlier archive bytes are reused only when that authenticated blob identity matches.',
        counts=dict(counts),genres={k:dict(v) for k,v in sorted(genres.items())},documents=records,
        limitations=['Repository snapshot is a distinct version; do not silently replace NLTK or prior archive texts.',
            'Git blob equality establishes byte identity, not linguistic correctness or license scope.',
            'Publisher resource header says free; publisher public MASC page states CC BY 3.0 US. Preserve both evidence scopes.',
            'No overlap analysis, split, tokenization or language evaluation performed yet.'])
    (ROOT/'docs/source-replacement/masc-publisher-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in manifest.items() if k!='documents'},indent=2))

if __name__=='__main__':main()
