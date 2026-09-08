"""Coverage denominators, source integrity and frozen lookup cases; not accuracy labels."""
from pathlib import Path
from collections import Counter,defaultdict
import gzip,hashlib,json,subprocess
r=Path(__file__).resolve().parent.parent;o=r/'docs/taiwan-quality'
def asset(path):return [s.split('\t') for s in path.read_text(encoding='utf8').splitlines() if s and not s.startswith('#')]
before=asset(r/'artifacts/addons-063-baseline.tsv');after=asset(r/'app/src/main/assets/addons.tsv')
assert {tuple(p) for p in before if p[0]!='taiwan'}=={tuple(p) for p in after if p[0]!='taiwan'},'Unrelated language packs changed'
assert {tuple(p[:4]) for p in before if p[0]=='taiwan'}<={tuple(p[:4]) for p in after if p[0]=='taiwan'},'Existing Taiwan aliases removed'
geo=(r/'app/src/main/assets/geography.tsv').read_bytes();original=subprocess.check_output(['git','show','3f25366:app/src/main/assets/geography.tsv'],cwd=str(r))
assert geo==original,'Rudy geography must remain byte-identical'
coverage=json.loads((o/'entity-coverage.json').read_text(encoding='utf8'))
label_data=json.loads(gzip.decompress((r/'third_party/taiwan_encyclopedia/traditional-labels.json.gz').read_bytes()).decode())
labels={k:v.get('labels',{}) for batch in label_data['requests'] for k,v in batch['entities'].items()}
snapshot=json.loads(gzip.decompress((r/'third_party/taiwan_encyclopedia/snapshot.json.gz').read_bytes()).decode())
assert not snapshot['errors'] and all(snapshot['root_counts'].values()),'Do not silently ship failed or empty roots'
old={p[2] for p in before if p[0]=='taiwan'};new={p[2] for p in after if p[0]=='taiwan'}
base={p[2] for p in asset(r/'app/src/main/assets/zh_tw.tsv')};roots=defaultdict(Counter);queries=set()
old_all=old|base;new_all=new|base
for e in coverage['records']:
    for root in e['roots']:
        c=roots[root];c['source_entities']+=1;c[e['status']]+=1;c['old_addon_string_present']+=e['output'] in old;c['old_base_or_addon_string_present']+=e['output'] in old_all;c['new_base_or_addon_string_present']+=e['output'] in new_all
    if e['status']!='included':continue
    if e['name_normalization']=='unchanged_source_title':
        import re
        assert e['output']==re.sub(r'\s*[（(][^()（）]*[)）]$','',e['title']).strip()
    else:
        import re
        explicit=labels[e['wikidata']['id']]
        assert e['output'] in {re.sub(r'\s*[（(][^()（）]*[)）]$','',v['value']).strip() for k,v in explicit.items() if k in ('zh-tw','zh-hant')},'Name identity must be source-attested'
    plain=e['pinyin'];word=e['output'];full=''.join(plain)
    if set(e['accepted_sectors'])&{'people','performers'}:assert any(v.get('id')=='Q5' for v in e['wikidata']['claims']['P31'])
    for mode,key in [('full',full),('initial',''.join(p[0] for p in plain)),('mixed-left',''.join(p if i%2 else p[0] for i,p in enumerate(plain))),('mixed-right',''.join(p if not i%2 else p[0] for i,p in enumerate(plain))),('prefix',full[:-1])]:
        if key:queries.add(('taiwan',mode,key,word))
# Freeze a deterministic sample of every old pack/form to expose new collisions.
for line in gzip.decompress((r/'docs/speculation/addon-inputs.tsv.gz').read_bytes()).decode().splitlines():
    if int(hashlib.sha256(('sector-064\t'+line).encode()).hexdigest()[:8],16)%20==0:queries.add(tuple(line.split('\t')))
payload=''.join('\t'.join(p)+'\n' for p in sorted(queries)).encode();(o/'entity-lookup-inputs.tsv').write_bytes(payload)
report=dict(before_taiwan_outputs=len(old),after_taiwan_outputs=len(new),new_taiwan_outputs=len(new-old),language_rows=len(after),geography_sha256=hashlib.sha256(geo).hexdigest(),geography_unchanged=True,other_language_rows_unchanged=True,existing_taiwan_aliases_preserved=True,roots={k:dict(v) for k,v in roots.items()},lookup_cases=len(queries),lookup_input_sha256=hashlib.sha256(payload).hexdigest(),coverage_limit='String presence does not prove entity disambiguation or correct pronunciation. Source graph and identity gaps are separate from reading resolution. Lookup targets derive from source entries, not unseen conversation labels.')
(o/'coverage-summary.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode());print(json.dumps({k:v for k,v in report.items() if k not in ('roots',)},ensure_ascii=False))
