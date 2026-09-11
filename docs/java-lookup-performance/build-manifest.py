import hashlib,json,zipfile
from pathlib import Path
OUT=Path('docs/java-lookup-performance')
result={'baseline_revision':'dce6ddb','build_property':'japaneseEvaluation=true; arm64-v8a',
        'files':{},'runtime':'Microsoft OpenJDK 17.0.11; Windows; desktop -Xmx1g'}
for name in ['baseline','candidate','tests']:
    p=Path(f'artifacts/java-lookup/{name}.apk')
    with zipfile.ZipFile(p) as z:
        assets=[s for s in z.namelist() if s.startswith('assets/japanese-evaluation/')]
        if name!='tests':assert not assets
        else:
            payload=z.read('assets/japanese-evaluation/java-lookup.tsv')
            assert payload.decode('utf-8').splitlines()==(OUT/'phone-inputs.tsv').read_text(encoding='utf-8').splitlines()
            result['test_input']={'sha256':hashlib.sha256(payload).hexdigest(),
                'same_frozen_rows':True,'line_endings':'CRLF in APK; LF in repository; Android BufferedReader removes terminators'}
    result['files'][name]={'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bytes':p.stat().st_size,'evaluation_assets':assets}
original=json.loads(Path('docs/japanese-provider-integration/build-manifest.json').read_text())[0]['sha256']
assert result['files']['baseline']['sha256']==original
result['baseline_matches_previous_final_apk']=True
manifest=json.loads((OUT/'manifest.json').read_text())
for name,digest in manifest['assets'].items():
    assert hashlib.sha256(Path(name).read_bytes()).hexdigest()==digest
result['compiled_dictionary_assets_unchanged']=True
(OUT/'build-manifest.json').write_text(json.dumps(result,indent=2)+'\n')
print('APK identities and instrumentation-only assets verified; dictionary bytes unchanged')
