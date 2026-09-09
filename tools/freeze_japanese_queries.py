"""Freeze source-common reading probes without reading decoder outputs."""
from pathlib import Path
import tarfile,json,subprocess,re,hashlib
ROOT=Path(__file__).resolve().parent.parent
with tarfile.open(ROOT/'third_party/jmdict/jmdict-eng-common.json.tgz') as archive:
    data=json.load(archive.extractfile(next(m for m in archive.getmembers() if m.name.endswith('.json'))))
eligible=[(w['id'],k['text']) for w in data['words'] for k in w['kana'] if k['common']]
aliases=json.loads(subprocess.check_output(['node',str(ROOT/'tools/romanize_kana.cjs')],input=json.dumps(sorted({k for _,k in eligible}),ensure_ascii=False).encode('utf8')))
rows=[];excluded=[]
for identity,kana in eligible:
    key=aliases[kana]['reading'];raw=key.replace("'",'')
    if not re.fullmatch('[a-z]{1,64}',raw):excluded.append([identity,kana,'unsupported ASCII alias']);continue
    rows.append((identity,kana,key,raw))
sample=sorted(rows,key=lambda r:hashlib.sha256(('japanese-basic-v1\t'+'\t'.join(r)).encode()).hexdigest())[:1024]
queries=[('full',identity,key,raw,kana) for identity,kana,key,raw in rows]
for identity,kana,key,raw in sample:
    if len(raw)>1:queries.append(('half',identity,key,raw[:max(1,len(raw)//2)],kana))
    if "'" in key:queries.append(('initials',identity,key,''.join(p[0] for p in key.split("'") if p),kana))
out=ROOT/'docs/japanese-coverage';payload=''.join('\t'.join(r)+'\n' for r in queries).encode('utf8')
path=out/'inputs.tsv'
if path.exists():assert path.read_bytes()==payload,'Frozen queries changed'
else:path.write_bytes(payload)
manifest=dict(source='third_party/jmdict/jmdict-eng-common.json.tgz',source_sha256=hashlib.sha256((ROOT/'third_party/jmdict/jmdict-eng-common.json.tgz').read_bytes()).hexdigest(),eligible_readings=len(rows),queries=len(queries),partial_sample=len(sample),excluded=excluded,sha256=hashlib.sha256(payload).hexdigest(),role='Source-derived retrieval regression, frozen before outputs; no independent language-accuracy claim.')
(out/'inputs-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({k:v for k,v in manifest.items() if k!='excluded'},indent=2))
