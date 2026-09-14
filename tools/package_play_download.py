"""Package already-verified release binaries with public, explicitly allowlisted files."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]


def digest(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()


def package(output,checks,version):
    output=output.resolve();output.mkdir(parents=True,exist_ok=True)
    revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    expected=(checks/'source-commit.txt').read_text(encoding='utf-8-sig').strip()
    assert revision==expected,'Built revision differs from checkout'
    subprocess.run(['git','diff','--exit-code','--','app/src/main','core/src/main','app/build.gradle'],cwd=ROOT,check=True)
    ns='{http://schemas.android.com/apk/res/android}'
    manifest=ET.parse(checks/'manifest.xml').getroot()
    assert manifest.attrib['package']=='app.minime.keyboard'
    assert manifest.attrib[ns+'versionName']==version
    inventory=json.loads((checks/'inventory.json').read_text(encoding='utf-8'))
    assert inventory['signature_block_present'] and not inventory['debuggable']
    aab=ROOT/'app/build/outputs/bundle/release/app-release.aab'
    apk=ROOT/'app/build/outputs/apk/release/app-release.apk'
    assert digest(aab)==inventory['sha256']
    for source,name in [(aab,f'MinIME-{version}-play-signed.aab'),(apk,f'MinIME-{version}-release.apk')]:
        shutil.copyfile(source,output/name)
    # A public certificate is useful for registration; private signing files never enter the archive.
    shutil.copyfile(checks/'upload-certificate.pem',output/'MinIME-upload-certificate.pem')
    notes={}
    for locale in ('en-US','zh-TW'):
        notes[locale]=(ROOT/f'docs/play-publishing/{version}/{locale}.txt').read_text(encoding='utf-8-sig').strip()
        assert len(notes[locale])<=500 and '\ufffd' not in notes[locale]
    note_text='\n\n'.join(f'<{locale}>\n{text}\n</{locale}>' for locale,text in notes.items())+'\n'
    note_name=f'MinIME-{version}-release-notes.txt'
    (output/note_name).write_text(note_text,encoding='utf-8-sig')
    metadata={'version':version,'version_code':int(manifest.attrib[ns+'versionCode']),
              'package':'app.minime.keyboard','source_commit':revision,
              'source_url':f'https://github.com/poi890poi/minime/tree/{revision}',
              'signer_sha256':(checks/'certificate-sha256.txt').read_text().strip(),
              'release_rights_open_items':inventory['release_rights_open_items'],
              'runtime_16kb_certified':False,
              'artifacts':[{ 'file':p.name,'bytes':p.stat().st_size,'sha256':digest(p)} for p in (output/f'MinIME-{version}-play-signed.aab',output/f'MinIME-{version}-release.apk')]}
    (output/'RELEASE.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    common={note_name:output/note_name,'RELEASE.json':output/'RELEASE.json',
            'README.md':ROOT/f'docs/play-publishing/{version}/README.md',
            'LICENSE':ROOT/'LICENSE','LICENSING.md':ROOT/'LICENSING.md',
            'NOTICE.txt':ROOT/'app/src/main/assets/NOTICE.txt',
            'MinIME-upload-certificate.pem':output/'MinIME-upload-certificate.pem'}
    archives=[]
    for suffix,binary in [('apk',f'MinIME-{version}-release.apk'),('Google-Play',f'MinIME-{version}-play-signed.aab')]:
        target=output/f'MinIME-{version}-{suffix}.zip';payload={**common,binary:output/binary}
        with zipfile.ZipFile(target,'w',zipfile.ZIP_STORED) as z:
            for name,path in payload.items():z.write(path,name)
            z.writestr('SHA256SUMS.txt',''.join(digest(path)+'  '+name+'\n' for name,path in payload.items()))
        with zipfile.ZipFile(target) as z:
            assert z.testzip() is None
            assert set(z.namelist())==set(payload)|{'SHA256SUMS.txt'}
            assert hashlib.sha256(z.read(binary)).hexdigest()==digest(output/binary)
            assert z.read(note_name).decode('utf-8-sig')==note_text
        archives.append(target)
    public=[output/x['file'] for x in metadata['artifacts']]+archives+[output/note_name,output/'MinIME-upload-certificate.pem',output/'RELEASE.json']
    sums=output/'SHA256SUMS.txt';sums.write_text(''.join(digest(p)+'  '+p.name+'\n' for p in public),encoding='utf-8')
    (output/'download-files.json').write_text(json.dumps([p.name for p in public]+[sums.name],indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'source_commit':revision,'files':[{ 'name':p.name,'bytes':p.stat().st_size,'sha256':digest(p)} for p in public]},indent=2))


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--checks',type=Path,required=True);p.add_argument('--version',required=True)
    args=p.parse_args();package(args.output,args.checks,args.version)
