import hashlib,json,zipfile
from pathlib import Path
OUT=Path('docs/japanese-pipeline-performance'); RAW=Path('artifacts/japanese-pipeline')
result={'baseline_revision':'8f2003b','files':{}}
for name in ['baseline','candidate','focused','tests']:
    p=RAW/(name+'.apk')
    with zipfile.ZipFile(p) as z:
        assets=[n for n in z.namelist() if n.startswith('assets/japanese-evaluation/')]
        if name!='tests':assert not assets
    result['files'][name]={'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bytes':p.stat().st_size,'evaluation_assets':assets}
old=json.loads(Path('docs/java-lookup-performance/build-manifest.json').read_text())
assert result['files']['baseline']['sha256']==old['files']['candidate']['sha256']
source=json.loads(Path('docs/java-lookup-performance/manifest.json').read_text())
for name,digest in source['assets'].items():assert hashlib.sha256(Path(name).read_bytes()).hexdigest()==digest
result['compiled_dictionaries_unchanged']=True
(OUT/'build-manifest.json').write_text(json.dumps(result,indent=2)+'\n')
print('Baseline identity, unchanged dictionaries and test-only native assets verified')
