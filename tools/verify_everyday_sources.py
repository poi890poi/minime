"""Check source paired POJ keys and record coverage deltas, not language accuracy."""
from pathlib import Path
import csv, hashlib, json, re
from everyday_addons import poj_letters
ROOT=Path(__file__).resolve().parent.parent
pairs=0; mismatches=[]
for row in csv.DictReader((ROOT/'third_party/taiwanese_basic/vocabulary.csv').open(encoding='utf-8-sig')):
    for suffix in ('', 'Others'):
        keys=row['PojInput'+suffix].split('/');outputs=row['PojUnicode'+suffix].split('/')
        if len(keys)!=len(outputs):continue
        for key,output in zip(keys,outputs):
            key=key.strip().lower()
            if not re.fullmatch('[a-z0-9 -]+',key):continue
            pairs+=1
            expected=re.sub('[1-9 -]','',key)
            actual=(poj_letters(output.strip()) or '').replace('-','').replace(' ','')
            if expected!=actual:mismatches.append([row['DictWordID'],key,output,actual])
assert not mismatches,mismatches[:10]
manifest=json.loads((ROOT/'docs/addons-learning/source-manifest.json').read_text(encoding='utf-8'))
for source in manifest['sources']:
    assert hashlib.sha256((ROOT/source['file']).read_bytes()).hexdigest()==source['sha256']
report=dict(paired_POJ_readings=pairs,mismatches=mismatches,source_hashes_verified=len(manifest['sources']),everyday=manifest['everyday'],outputs_by_pack=manifest['outputs_by_pack'],asset_sha256=manifest['asset_sha256'])
(ROOT/'docs/everyday-phrases/source-verification.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode())
print(json.dumps(report,ensure_ascii=False,indent=2))
