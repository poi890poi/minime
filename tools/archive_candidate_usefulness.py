"""Archive reproducible observations and bounded non-private cleanup evidence."""
from pathlib import Path
import gzip,hashlib,json,shutil

root=Path(__file__).resolve().parents[1]
out=root/'docs/candidate-usefulness/evidence';out.mkdir(exist_ok=True)
manifest={}
for label,folder in [('before',''),('final','final'),('english','english-phone')]:
    base=root/'artifacts/candidate-usefulness'/folder
    for batch in (1,2):
        src=base/f'phone-{batch:02}'/'observations.json'
        if not src.exists():continue
        dest=out/f'{label}-{batch:02}-observations.json.gz'
        dest.write_bytes(gzip.compress(src.read_bytes(),mtime=0))
        manifest[dest.name]=dict(source=str(src.relative_to(root)),source_sha256=hashlib.sha256(src.read_bytes()).hexdigest())
        version=src.with_name('google-version.txt')
        if version.exists():shutil.copyfile(version,out/f'{label}-{batch:02}-google-version.txt')
# Representative screenshots: first case, English-interference example,
# a Google off-target construction, and a MinIME whole-target win. Full records
# above preserve all cases; these presentation choices never tune production.
for batch,case in [(1,0),(1,7),(2,12),(2,14)]:
    for provider in ('google','minime'):
        src=root/f'artifacts/candidate-usefulness/final/phone-{batch:02}/parity-{provider}-useful-{case:02}-step-0.png'
        if src.exists():shutil.copyfile(src,out/src.name)
for provider in ('google','minime'):
    src=root/f'artifacts/candidate-usefulness/english-phone/phone-01/parity-{provider}-english-interference-00-step-0.png'
    if src.exists():shutil.copyfile(src,out/src.name)
for name in ('core-final.log','desktop-final.log','android-final-build.log','no-preview.log','chinese-first.log','english-before.log','english-after.log'):
    src=root/'artifacts/candidate-usefulness'/name
    if src.exists():(out/name).write_bytes(src.read_bytes().rstrip()+b'\n')
for name in ('english-before.tsv','english-after.tsv'):
    src=root/'artifacts/candidate-usefulness'/name
    if src.exists():(out/(name+'.gz')).write_bytes(gzip.compress(src.read_bytes(),mtime=0))
# Selection/session identities are explicit: do not sweep other tasks' device runs.
sessions=['fce91f4e-7cfd-49b7-97f0-746d696f9c3e','dfcbee51-5b9b-46b6-a669-49f0a781d831','896764de-9615-4779-89fa-ad6086f609b8','cb33c430-5388-42ee-b857-47806b80d701']
cleanup=[]
for session in sessions:
    folder=root/'artifacts/device-tests'/session
    display=folder/'display-after.txt'
    if display.exists():
        text=display.read_text(encoding='utf-8-sig')
        cleanup.append(dict(session=session,display_off_evidence=[line.strip() for line in text.splitlines() if 'Display State=OFF' in line],
            display_evidence_sha256=hashlib.sha256(display.read_bytes()).hexdigest(),
            note='Session wrapper verified previous IME and preferences restoration; private XML is not archived.'))
(out/'cleanup.json').write_text(json.dumps(cleanup,indent=2)+'\n',encoding='utf-8')
manifest['archive_sha256']={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(out.iterdir()) if p.is_file() and p.name!='manifest.json'}
(out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
