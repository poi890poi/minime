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
