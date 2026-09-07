"""Compile pinned Rime data with an installed librime 1.16.1 rime_deployer.

Usage: python tools/compile_rime_data.py /path/to/rime_deployer
Upstream sources and our configuration patches stay in third_party/rime.
"""
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
SOURCE=ROOT/'third_party/rime'
OUT=ROOT/'app/src/main/rimeAssets/rime'
OUT.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory(prefix='minime-rime-') as scratch:
    base=Path(scratch);shared=base/'shared';user=base/'user';compiled=base/'compiled'
    for p in (shared,user,compiled):p.mkdir()
    for item in json.loads((SOURCE/'data-sources.json').read_text(encoding='utf-8')):
        archive=SOURCE/'data-sources'/item['archive']
        assert hashlib.sha256(archive.read_bytes()).hexdigest()==item['sha256']
        with zipfile.ZipFile(str(archive)) as zipped:
            for name in zipped.namelist():
                parts=name.split('/')
                if len(parts)==2 and (parts[1].endswith('.yaml') or parts[1]=='essay.txt'):
                    (shared/parts[1]).write_bytes(zipped.read(name))
    for p in (SOURCE/'data-sources').glob('*.custom.yaml'):shutil.copyfile(str(p),str(user/p.name))
    # librime records source mtimes in the compiled schema and hashes that schema
    # into the prism. Fix source times rather than altering binary metadata.
    for folder in (shared,user):
        # Half-second avoids clock-conversion rounding across an integer boundary
        # in librime's filesystem::to_time_t implementation on Windows.
        for p in folder.iterdir():os.utime(str(p),(1704067200.5,1704067200.5))
    subprocess.check_call([str(Path(sys.argv[1]).resolve()),'--build',str(user),str(shared),str(compiled)])
    for name in ('default.yaml','luna_pinyin.schema.yaml','luna_pinyin.prism.bin','luna_pinyin.table.bin','luna_pinyin.reverse.bin'):
        data=(compiled/name).read_bytes()
        (OUT/name).write_bytes(data)
hashes={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(OUT.iterdir()) if p.name!='bundle-id.txt'}
bundle=hashlib.sha256(json.dumps(hashes,sort_keys=True).encode()).hexdigest()
(OUT/'bundle-id.txt').write_text(bundle+'\n',encoding='utf-8')
(SOURCE/'model.json').write_text(json.dumps(dict(librime='1.16.1',bundle=bundle,assets=hashes),indent=2)+'\n',encoding='utf-8')
print('Rime bundle '+bundle)
