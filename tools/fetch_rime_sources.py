"""Fetch hash-pinned native sources into the ignored build cache (Python 3.7+)."""
import hashlib
import json
import tarfile
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEST = ROOT / '.tools/native-sources'
DEST.mkdir(parents=True, exist_ok=True)

def safe(name):
    path = (DEST / name).resolve()
    if DEST.resolve() not in path.parents:
        raise ValueError('Archive path escapes source cache: '+name)
    return path

for source in json.loads((ROOT/'third_party/rime/native-sources.json').read_text(encoding='utf-8')):
    archive = DEST / source['archive']
    if not archive.exists():
        print('Downloading '+source['name'], flush=True)
        urllib.request.urlretrieve(source['url'], str(archive))
    if hashlib.sha256(archive.read_bytes()).hexdigest() != source['sha256']:
        raise ValueError('Source hash mismatch: '+source['name'])
    stamp = DEST / (source['name']+'.verified')
    if stamp.exists() and stamp.read_text(encoding='utf-8') == source['sha256']:
        continue
    if archive.name.endswith('.zip'):
        with zipfile.ZipFile(str(archive)) as zipped:
            for item in zipped.infolist():
                safe(item.filename)
            zipped.extractall(str(DEST))
    else:
        with tarfile.open(str(archive)) as tar:
            for item in tar:
                if not (item.name.startswith(source['directory']+'/boost/') or item.name.endswith('/LICENSE_1_0.txt')):
                    continue
                target = safe(item.name)
                if item.isdir():
                    target.mkdir(parents=True, exist_ok=True)
                elif item.isfile():
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(tar.extractfile(item).read())
                else:
                    raise ValueError('Unsupported archive entry: '+item.name)
    stamp.write_text(source['sha256'], encoding='utf-8')
    print('Verified '+source['name'], flush=True)
