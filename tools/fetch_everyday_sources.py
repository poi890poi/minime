"""Restore pinned everyday sources, verifying exact bytes before replacing files."""
from pathlib import Path
import hashlib, json, urllib.request
root = Path(__file__).resolve().parent.parent
for folder, filename in [('jmdict','jmdict-eng-common.json.tgz'), ('taiwanese_basic','vocabulary.csv')]:
    target = root/'third_party'/folder/filename
    source = json.loads(target.with_name(filename+'.source.json').read_text(encoding='utf-8'))
    data = urllib.request.urlopen(source['url'], timeout=60).read()
    assert hashlib.sha256(data).hexdigest() == source['sha256'], 'Upstream snapshot changed'
    target.write_bytes(data)
    print(folder, len(data))
