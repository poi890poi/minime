"""Restore pinned evaluation-only dependencies from the reviewed manifest."""
import hashlib,json,urllib.request,zipfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
WORK=HERE.parents[1]/'artifacts/japanese-engine-benchmark'

def main():
    manifest=json.loads((HERE/'source-manifest.json').read_text(encoding='utf8'))
    for item in manifest['downloads']:
        path=WORK/item['path'];path.parent.mkdir(parents=True,exist_ok=True)
        if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest()!=item['sha256']:
            request=urllib.request.Request(item['url'],headers={'User-Agent':'MinIME-evaluation'})
            with urllib.request.urlopen(request,timeout=120) as response:data=response.read()
            assert hashlib.sha256(data).hexdigest()==item['sha256'],item['path']
            path.write_bytes(data)
        if item.get('extract'):
            dest=(WORK/item['extract']).resolve();dest.mkdir(parents=True,exist_ok=True)
            with zipfile.ZipFile(path) as archive:
                for name in archive.namelist():
                    assert dest in (dest/name).resolve().parents,('Unsafe archive path',name)
                archive.extractall(dest)
    print('Verified',len(manifest['downloads']),'pinned downloads; evaluation only')

if __name__=='__main__':main()
