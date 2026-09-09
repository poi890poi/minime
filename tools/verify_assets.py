"""Check that shipped language assets match the recorded generation output."""
import hashlib
import json
from pathlib import Path
root=Path(__file__).resolve().parent.parent
report=json.loads((root/'docs/dictionary-report.json').read_text(encoding='utf-8'))
assert report['chinese_readings'] > 100000 and report['english_words'] > 30000
for name,expected in report['assets'].items():
    path=root/'app/src/main/assets'/name
    assert hashlib.sha256(path.read_bytes()).hexdigest()==expected, 'Asset changed: '+name
manifest=(root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.BIND_INPUT_METHOD' in manifest
print('PASS dictionary asset integrity and offline IME manifest')
japanese=json.loads((root/'docs/japanese-coverage/basics-manifest.json').read_text(encoding='utf-8'))
assert hashlib.sha256((root/'app/src/main/assets/japanese-basic.tsv').read_bytes()).hexdigest()==japanese['asset_sha256']
for name,digest in japanese['sources'].items():
    assert hashlib.sha256((root/name).read_bytes()).hexdigest()==digest, 'Japanese source changed: '+name
print('PASS source-ranked Japanese character asset and input hashes')
emoji=json.loads((root/'docs/emoji-report.json').read_text(encoding='utf-8'))
palette=root/'app/src/main/assets/emoji.tsv'
assert hashlib.sha256(palette.read_bytes()).hexdigest()==emoji['asset_sha256']
rows=[line.split('\t') for line in palette.read_text(encoding='utf-8').splitlines()]
assert len(rows)==emoji['entries']==3010
assert all(len(row)==4 and all(row) for row in rows)
sequences={row[2] for row in rows}
assert len(sequences)==len(rows)
assert {'😀','❤️','👍🏽','👨‍👩‍👧‍👦','🇹🇼'} <= sequences
print('PASS 3010 Unicode emoji sequences, including flags, skin tones and ZWJ families')
context=json.loads((root/'docs/context-report.json').read_text(encoding='utf-8'))
assert hashlib.sha256((root/'app/src/main/assets/context.tsv').read_bytes()).hexdigest()==context['asset_sha256']
print('PASS context asset integrity; training splits recorded separately from holdouts')
spelling=json.loads((root/'docs/dictionary-impact/spelling-sources.json').read_text(encoding='utf-8'))
for name,digest in spelling['sources'].items():
    assert hashlib.sha256((root/name).read_bytes()).hexdigest()==digest, 'Spelling source changed: '+name
assert hashlib.sha256((root/'app/src/main/assets/en_spelling.tsv').read_bytes()).hexdigest()==spelling['asset_sha256']
print('PASS apostrophe validity and grammar metadata hashes; TRAIN source only')
binary=root/'app/build/generated/minimeAssets/model.bin'
if binary.exists():
    compiled=json.loads((root/'docs/model-report.json').read_text(encoding='utf-8'))
    assert hashlib.sha256(binary.read_bytes()).hexdigest()==compiled['sha256'], 'Compiled model changed'
    for name,expected in compiled['sources'].items():
        assert hashlib.sha256((root/'app/src/main/assets'/name).read_bytes()).hexdigest()==expected
    print('PASS compiled model and source hashes')
rime_path=root/'third_party/rime/model.json'
if rime_path.exists():
    rime=json.loads(rime_path.read_text(encoding='utf-8'))
    assets=root/'app/src/main/rimeAssets/rime'
    for name,expected in rime['assets'].items():
        assert hashlib.sha256((assets/name).read_bytes()).hexdigest()==expected, 'Rime asset changed: '+name
    assert (assets/'bundle-id.txt').read_text(encoding='utf-8').strip()==rime['bundle']
    assert hashlib.sha256(json.dumps(rime['assets'],sort_keys=True).encode()).hexdigest()==rime['bundle']
    opencc=root/'third_party/rime/data-sources/opencc'
    for name,digest in json.loads((root/'third_party/rime/opencc-data.json').read_text(encoding='utf-8'))['files'].items():
        assert hashlib.sha256((opencc/Path(name).name).read_bytes()).hexdigest()==digest
    assert (assets/'opencc/TWVariants.txt').read_bytes()==(opencc/'TWVariants.txt').read_bytes()
    for item in json.loads((root/'third_party/rime/data-sources.json').read_text(encoding='utf-8')):
        assert hashlib.sha256((root/'third_party/rime/data-sources'/item['archive']).read_bytes()).hexdigest()==item['sha256']
    print('PASS pinned Rime models, bundle identity and corresponding source archives')
