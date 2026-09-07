"""Check the exact APKs selected for installation; APK paths may be IDE outputs."""
import hashlib
import json
import sys
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
app=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'app/build/outputs/apk/debug/app-debug.apk'
test=Path(sys.argv[2]) if len(sys.argv)>2 else None
model=json.loads((ROOT/'third_party/rime/model.json').read_text(encoding='utf-8'))
with zipfile.ZipFile(str(app)) as z:
    names=z.namelist()
    assert len(names)==len(set(names)), 'Duplicate APK entries'
    assert any(b'Ldev/minime/ime/RimeBackend;' in z.read(n) for n in names if n.endswith('.dex')), 'Wrong/stale app APK'
    for name,digest in model['assets'].items():
        assert hashlib.sha256(z.read('assets/rime/'+name)).hexdigest()==digest, 'Packaged Rime model mismatch: '+name
    assert z.read('assets/rime/bundle-id.txt').decode().strip()==model['bundle']
    legacy=json.loads((ROOT/'docs/model-report.json').read_text(encoding='utf-8'))
    assert hashlib.sha256(z.read('assets/model.bin')).hexdigest()==legacy['sha256']
    assert 'assets/rime-probes.tsv' not in names, 'Evaluation data must not ship'
    assert 'assets/conversation-probes.tsv' not in names, 'Conversation evaluation must not ship'
    assert z.read('assets/NOTICE.txt')==(ROOT/'app/src/main/assets/NOTICE.txt').read_bytes(), 'Packaged notices stale'
    abis=sorted(n.split('/')[1] for n in names if n.startswith('lib/') and n.endswith('/libminime_rime.so'))
    assert abis, 'Native Rime library missing'
if test:
    with zipfile.ZipFile(str(test)) as z:
        assert any(b'RimeIntegrationTest' in z.read(n) for n in z.namelist() if n.endswith('.dex')), 'Wrong/stale test APK'
        assert z.read('assets/rime-probes.tsv')==(ROOT/'app/src/androidTest/assets/rime-probes.tsv').read_bytes()
print(json.dumps(dict(apk=str(app),sha256=hashlib.sha256(app.read_bytes()).hexdigest(),bytes=app.stat().st_size,abis=abis,rimeBundle=model['bundle']),indent=2))
