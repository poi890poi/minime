"""Shared, offline source catalog and immutable input ledger (Python 3.7+).

Existing compilers and detailed manifests still own extraction and licensing text.
This layer owns source eligibility, review evidence, input identity and freeze rules.
It does not assign vocabulary scores or infer quality from counts or locale tags.
"""
from pathlib import Path
import argparse,fnmatch,hashlib,json,re,sys

ROOT=Path(__file__).resolve().parent.parent
CATALOG='sources/catalog.json';LOCK='sources/lock.json'
EVALUATION='sources/evaluation.json'
DECISIONS={'retain','pilot','hold','replace','exclude','reference'}

class SourceError(ValueError):pass

def read(root,path):return json.loads((root/path).read_text(encoding='utf-8'))
def digest(path):
    raw=path.read_bytes()
    if path.suffix.lower() in ('.txt','.json','.yaml','.yml','.csv','.tsv','.md','.occ'):raw=raw.replace(b'\r\n',b'\n')
    return hashlib.sha256(raw).hexdigest()
def safe_path(root,path):
    if not isinstance(path,str) or '\\' in path or Path(path).is_absolute() or '..' in path.split('/'):
        raise SourceError('Expected a repository-relative source path: '+str(path))
    resolved=(root/path).resolve()
    if root.resolve() not in resolved.parents:raise SourceError('Source path leaves repository: '+path)
    return resolved

def catalog(root):
    doc=read(root,CATALOG)
    if doc.get('format')!=1:raise SourceError('Unsupported catalog format')
    ids=set()
    for item in doc['sources']:
        key=item.get('id','')
        if not re.fullmatch('[a-z0-9][a-z0-9-]*',key) or key in ids:raise SourceError('Invalid or duplicate source ID: '+key)
        ids.add(key)
        for field in ('name','origin','editorial','coverage','limitations','license','evidence','decision','reason','uses','inputs'):
            if field not in item:raise SourceError(key+' missing '+field)
        if item['decision'] not in DECISIONS:raise SourceError(key+' invalid decision')
        if not item['evidence'] or not item['limitations'] or not item['reason']:raise SourceError(key+' lacks review evidence/limitations/reason')
        for use in item['uses']:
            if use not in ('vocabulary','readings','frequency','tooling','evaluation','discovery'):raise SourceError(key+' invalid use')
        if item['inputs'] and item['decision'] not in ('retain','replace','reference'):raise SourceError(key+' is not eligible for production inputs')
        if item['inputs'] and item['origin']=='simplified-chinese' and set(item['uses'])&{'vocabulary','readings','frequency'}:raise SourceError(key+' violates original-source policy')
        if item['decision']=='retain' and item['license'].get('status')!='verified':raise SourceError(key+' has unresolved licensing')
        if item['decision']=='replace' and not item.get('replacement'):raise SourceError(key+' needs an explicit replacement track')
        for pattern in item['inputs']:safe_path(root,pattern.replace('*','placeholder'))
        for evidence in item.get('audit_files',[]):
            if not safe_path(root,evidence).is_file():raise SourceError(key+' missing audit '+evidence)
    return doc

def inventory(root,doc):
    files={}
    for item in doc['sources']:
        for pattern in item['inputs']:
            matches=[p for p in root.glob(pattern) if p.is_file() and '__pycache__' not in p.parts]
            if not matches:raise SourceError(item['id']+' input pattern is empty: '+pattern)
            for path in matches:
                rel=path.relative_to(root).as_posix();safe_path(root,rel)
                if rel in files and files[rel]['source']!=item['id']:raise SourceError('Ambiguous source ownership: '+rel)
                files[rel]={'source':item['id'],'sha256':digest(path)}
    return files

def addon_rows(root,doc):
    path=root/'app/src/main/assets/addons.tsv'
    if not path.exists():return {}
    groups={}
    prefixes=[(prefix,s['id']) for s in doc['sources'] for prefix in s.get('addon_prefixes',[])]
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        columns=line.split('\t')
        if len(columns)!=5:raise SourceError('Malformed add-on provenance row')
        owners={key for prefix,key in prefixes if columns[3].startswith(prefix)}
        if len(owners)!=1:raise SourceError('Unknown or ambiguous add-on provenance: '+columns[3])
        key=owners.pop();source=next(s for s in doc['sources'] if s['id']==key)
        if source['decision'] not in ('retain','replace'):raise SourceError('Unaudited source reached packaged add-on: '+key)
        groups.setdefault(key,[]).append(line)
    return {key:{'rows':len(lines),'sha256':hashlib.sha256(('\n'.join(sorted(lines))+'\n').encode('utf-8')).hexdigest()} for key,lines in groups.items()}

def manifest_sources(root,doc):
    # Reuse the existing add-on manifest instead of inventing a second source URL
    # or license resolver. New file references must have one catalog owner.
    path=root/'docs/addons-learning/source-manifest.json'
    if not path.exists():return
    for source in read(root,'docs/addons-learning/source-manifest.json')['sources']:
        path=source['file'];owners=[s for s in doc['sources'] if any(fnmatch.fnmatchcase(path,p) for p in s['inputs'])]
        if len(owners)!=1:raise SourceError('Unregistered manifest source: '+path)
        if hashlib.sha256(safe_path(root,path).read_bytes()).hexdigest()!=source['sha256']:raise SourceError('Existing source-manifest hash mismatch: '+path)

def evaluation_inventory(root,doc,production):
    """Pin evaluation evidence separately; never claim semantic independence."""
    if not (root/EVALUATION).exists():return {}
    registry=read(root,EVALUATION)
    if registry.get('format')!=1:raise SourceError('Unsupported evaluation catalog format')
    indexed={s['id']:s for s in doc['sources']};files={EVALUATION:digest(root/EVALUATION)};ids=set()
    for corpus in registry['corpora']:
        key=corpus['id']
        if key in ids:raise SourceError('Duplicate evaluation corpus: '+key)
        ids.add(key)
        for field in ('source_ids','genre','selection','limitations','leakage','files','report_by'):
            if not corpus.get(field):raise SourceError(key+' missing evaluation '+field)
        if corpus.get('role') not in ('audit','development','regression','holdout'):raise SourceError(key+' invalid evaluation role')
        if corpus.get('role')=='holdout' and corpus.get('exposure')!='unseen':raise SourceError(key+' exposed data cannot be a fresh holdout')
        if corpus.get('exposure') not in ('unseen','inspected','previously-evaluated'):raise SourceError(key+' missing exposure status')
        if 'genre' not in corpus['report_by']:raise SourceError(key+' must report genre separately')
        for source in corpus['source_ids']:
            if source not in indexed or 'evaluation' not in indexed[source]['uses']:raise SourceError(key+' source not registered for evaluation: '+source)
        for p in corpus['files']:
            path=safe_path(root,p)
            if p in production:raise SourceError(key+' evaluation artifact is also a production input: '+p)
            if not path.is_file():raise SourceError(key+' missing evaluation artifact: '+p)
            files[p]=digest(path)
    return files

def make_lock(root,doc,previous=None):
    result={'format':1,'digest_policy':'Text CRLF normalized to LF; binary bytes exact. Existing upstream manifests separately retain original-byte hashes.','files':inventory(root,doc),'addon_rows':addon_rows(root,doc),'audit_evidence':{p:digest(safe_path(root,p)) for s in doc['sources'] for p in s.get('audit_files',[])}}
    result['evaluation']=evaluation_inventory(root,doc,result['files'])
    if previous:
        frozen={s['id'] for s in doc['sources'] if s['decision']=='replace'}
        for key in frozen:
            old={p:v for p,v in previous['files'].items() if v['source']==key}
            new={p:v for p,v in result['files'].items() if v['source']==key}
            if old!=new or previous['addon_rows'].get(key)!=result['addon_rows'].get(key):
                raise SourceError(key+' is frozen pending replacement; lock refresh cannot bless changed data')
    return result

def verify(root=ROOT):
    doc=catalog(root);expected=read(root,LOCK);actual=make_lock(root,doc)
    if expected!=actual:
        changed=sorted(p for p in set(expected['files'])|set(actual['files']) if expected['files'].get(p)!=actual['files'].get(p))
        sections=[k for k in ('addon_rows','audit_evidence','evaluation') if expected.get(k)!=actual.get(k)]
        raise SourceError('Source lock drift: '+(', '.join(changed[:8]+sections) or 'lock metadata'))
    manifest_sources(root,doc)
    return doc,actual

def require_sources(*ids):
    doc,_=verify();indexed={s['id']:s for s in doc['sources']}
    for key in ids:
        if key not in indexed or indexed[key]['decision'] not in ('retain','replace'):
            raise SourceError('Source unavailable for this compiler: '+key)

def report(doc):
    lines=['# Source decisions','',
      'Generated from sources/catalog.json. Retain is scoped to the listed use; pilot is not production approval. Replace preserves the existing locked release while prohibiting silent refresh or expansion. Counts do not establish taste, frequency or coverage outside the source inventory.','',
      '| Source | Decision | Intended use | Reason |','|---|---|---|---|']
    for s in doc['sources']:lines.append('| '+s['name']+' | '+s['decision']+' | '+', '.join(s['uses'])+' | '+s['reason'].replace('|','/')+' |')
    return '\n'.join(lines)+'\n'

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('command',choices=('check','lock','report'));args=parser.parse_args()
    try:
        if args.command=='check':
            doc,lock=verify();print('PASS source catalog: {} sources, {} pinned inputs, {} add-on source groups'.format(len(doc['sources']),len(lock['files']),len(lock['addon_rows'])))
            for s in doc['sources']:
                if s['decision']=='replace':print('FROZEN / REPLACE:',s['id'])
        elif args.command=='lock':
            doc=catalog(ROOT);previous=read(ROOT,LOCK) if (ROOT/LOCK).exists() else None
            result=make_lock(ROOT,doc,previous);manifest_sources(ROOT,doc)
            (ROOT/LOCK).write_bytes((json.dumps(result,ensure_ascii=False,indent=2)+'\n').encode('utf-8'));print('Source lock written; review the diff before committing.')
        else:sys.stdout.write(report(catalog(ROOT)))
    except (SourceError,KeyError,FileNotFoundError) as e:parser.exit(1,str(e)+'\n')

if __name__=='__main__':main()
