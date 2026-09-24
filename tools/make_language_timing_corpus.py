"""Freeze existing, labelled language-specific typing inputs without IME queries."""
from collections import Counter, defaultdict
from pathlib import Path
import csv,gzip,hashlib,io,json,re

ROOT=Path(__file__).resolve().parents[1]
SEED='minime-language-timing-20260924-v1'
LIMIT=16
CONDITIONS={'full','half','initials','mixed','transpose','neighbor','omission'}
BASE='docs/language-contract-benchmark/inputs.tsv'
CONVERSATIONS='docs/language-contract-benchmark/conversations/inputs.tsv'
REFERENCES='docs/language-contract-benchmark/conversations/references.jsonl.gz'
CHAT='docs/release-hardening/chat-inputs.tsv.gz'
OUTPUT='docs/release-hardening/language-timing'
ASSET='app/src/androidTest/assets/language-timing-inputs.tsv'
FIELDS=['id','mode','source','genre','condition','raw','source_file','source_line','document','identity','source_role']

def sha(data):return hashlib.sha256(data).hexdigest()
def identity(row):return '\t'.join(str(row[k]) for k in FIELDS if k!='id')
def order(row):return sha((SEED+'\n'+identity(row)).encode('utf-8'))
def select(rows,limit=LIMIT):
    unique={};excluded=Counter()
    for row in rows:
        if row['condition'] not in CONDITIONS:excluded['unsupported-condition']+=1;continue
        if not re.fullmatch('[a-z]{1,32}',row['raw']):excluded['not-1-to-32-lowercase-letter-keys']+=1;continue
        key=tuple(row[k] for k in ['mode','source','condition','raw'])
        if key in unique:
            excluded['duplicate-input-within-stratum']+=1
            if order(row)>=order(unique[key]):continue
        unique[key]=row
    strata=defaultdict(list)
    for row in unique.values():strata[tuple(row[k] for k in ['mode','source','condition'])].append(row)
    selected=[];counts={}
    for key,values in sorted(strata.items()):
        candidates=sorted(values,key=order);chosen=candidates[:limit]
        counts['/'.join(key)]={'eligible_distinct':len(values),'selected':len(chosen),'letters':sum(len(r['raw']) for r in chosen)}
        for row in chosen:selected.append(dict(id=order(row),**row))
    return selected,dict(excluded),counts

def read_rows(root=ROOT):
    rows=[];skipped=Counter()
    def add(mode,source,genre,condition,raw,file,line,document,ident,role):
        rows.append(dict(mode=mode,source=source,genre=genre,condition=condition,raw=raw,
            source_file=file,source_line=line,document=document,identity=ident,source_role=role))
    base_modes={'en-conversation':('english','conversation'),'en-reserved-conversation':('english','conversation'),
                'en-essay':('english','essay'),'zh-essay':('chinese','essay')}
    for n,line in enumerate((root/BASE).read_text(encoding='utf-8').splitlines(),1):
        p=line.split('\t');assert len(p)==7
        if p[0] not in base_modes:skipped[p[0]]+=1;continue
        mode,genre=base_modes[p[0]]
        add(mode,p[0],genre,p[3],p[4],BASE,n,p[1],p[2],'reused-regression; upstream role not encoded in this TSV')
    with gzip.open(root/REFERENCES,'rt',encoding='utf-8') as f:references=[json.loads(line) for line in f]
    lines=(root/CONVERSATIONS).read_text(encoding='utf-8').splitlines();assert len(lines)==len(references)
    for n,(line,ref) in enumerate(zip(lines,references),1):
        p=line.split('\t');assert len(p)==7 and ref['row']==n-1 and ref['document']==p[1]
        source=ref['source']
        mode,genre={'real-persona-chat':('japanese_english','casual-conversation'),
                    'asdc':('japanese_english','service-roleplay'),
                    'suisiann-thousand':('taiwanese_english','authored-situational-examples'),
                    'taiwanese-basic':('taiwanese_english','historical-authored-examples')}[source]
        add(mode,source,genre,p[3],p[4],CONVERSATIONS,n,p[1],p[2],ref['role']+'; already evaluated')
    with gzip.open(root/CHAT,'rt',encoding='utf-8') as f:
        for n,row in enumerate(csv.DictReader(f,delimiter='\t'),2):
            add('chinese','moztw-g0v-edited','chat-derived-edited-excerpts',row['condition'],row['raw'],CHAT,n,
                row['id'].split('/')[0],row['id'],'reused-regression; original conversations unavailable')
    return rows,dict(skipped)

def build(root=ROOT):
    rows,skipped=read_rows(root);selected,excluded,strata=select(rows)
    stream=io.StringIO(newline='');writer=csv.DictWriter(stream,FIELDS,delimiter='\t',lineterminator='\n');writer.writeheader();writer.writerows(selected)
    payload=stream.getvalue().encode('utf-8');modes=defaultdict(Counter)
    for row in selected:modes[row['mode']].update(queries=1,letters=len(row['raw']),spaces=1)
    manifest=dict(format=1,seed=SEED,limit_per_mode_source_condition=LIMIT,role='reused regression; not a fresh language-quality holdout',
        selection='Source/condition rules and salted SHA-256 only; no output, rank, expected-target availability or manual word choice.',
        source_sha256={p:sha((root/p).read_bytes()) for p in [BASE,CONVERSATIONS,REFERENCES,CHAT]},
        input_sha256=sha(payload),rows=len(selected),modes={k:dict(v) for k,v in sorted(modes.items())},strata=strata,
        excluded=excluded,excluded_source_groups=skipped,
        limitations=['Source conditions independently sampled; their latency differences are not isolated error effects.',
            'Single-query reset workload; no natural multi-turn context or physical touch record.',
            'ASCII letter keys only; punctuation, tone digits and longer input excluded, not certified.',
            'Missing source-condition combinations remain missing; Taiwanese examples are not spontaneous conversations.',
            'Corpora already evaluated for language quality and some lookup costs; these are not fresh holdouts.',
            'Inventory is not measurement or the 10000-action-per-mode, three-session release gate.'])
    directory=root/OUTPUT;directory.mkdir(parents=True,exist_ok=True)
    (directory/'inputs.tsv').write_bytes(payload);(root/ASSET).write_bytes(payload)
    (directory/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return manifest

if __name__=='__main__':
    result=build();print(json.dumps({k:result[k] for k in ['rows','modes','excluded','input_sha256']},indent=2))
