"""Reproducible source/data distribution draft; never includes research APKs or test corpora."""
import argparse,hashlib,json,subprocess,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent

def write_zip(target,files):
    manifest=[]
    with zipfile.ZipFile(str(target),'w',zipfile.ZIP_DEFLATED) as z:
        for name,p in sorted(files.items()):
            data=p.read_bytes();entry=zipfile.ZipInfo(name,date_time=(2026,1,1,0,0,0));entry.compress_type=zipfile.ZIP_DEFLATED;entry.external_attr=0o100644<<16
            z.writestr(entry,data);manifest.append({'path':name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()})
        entry=zipfile.ZipInfo('CONTENTS.json',date_time=(2026,1,1,0,0,0));entry.compress_type=zipfile.ZIP_DEFLATED;z.writestr(entry,json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
    return {'file':target.name,'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'entries':len(manifest)}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);args=parser.parse_args();out=Path(args.output);out.mkdir(parents=True,exist_ok=True)
    tracked=subprocess.check_output(['git','ls-files','-z'],cwd=str(ROOT)).decode().split('\0')
    # Add current authored release files before their first commit; paths are
    # allowlisted below and personal artifacts/builds are never traversed.
    candidates=set(filter(None,tracked))
    for base in ['app/src/main','core/src/main','tools','sources']:
        candidates.update(str(p.relative_to(ROOT)).replace('\\','/') for p in (ROOT/base).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
    candidates.update(['LICENSE','NOTICE','LICENSING.md','README.md','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'])
    files={}
    for name in sorted(candidates):
        p=ROOT/name
        admitted=name in ('LICENSE','NOTICE','LICENSING.md','README.md','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','app/build.gradle','core/build.gradle') or name.startswith(('app/src/main/','core/src/main/','gradle/','sources/')) or (name.startswith('tools/') and p.suffix in ('.py','.ps1','.cpp')) or (name.startswith('third_party/') and not name.startswith('third_party/ud/UD_English-GUM/'))
        if admitted and p.is_file() and not name.endswith(('.apk','.jks','.keystore','.conllu.gz')):files[name]=p
    # Reproduction manifests, not evaluation text. Detailed add-on extraction
    # ledgers include source IDs, selections and upstream pins.
    for name in ['docs/play-publishing/RELEASE.md','core/src/test/java/dev/minime/core/CompileModel.java','core/src/test/java/dev/minime/core/CompileAddonPacks.java','docs/addons-learning/source-manifest.json','docs/addons-learning/rudy-manifest.json','docs/dictionary-report.json','docs/context-report.json','docs/model-report.json','docs/dictionary-impact/spelling-sources.json']:
        if (ROOT/name).exists():files[name]=ROOT/name
    result=[write_zip(out/'MinIME-runtime-sources.zip',files)]
    for pack in ['poj','japanese','taiwan']:
        files={name:ROOT/'app/build/generated/minimeAssets'/name for name in ['addon-'+pack+'.tsv','addon-'+pack+'.bin']}
        files.update({'NOTICE.txt':ROOT/'app/src/main/assets/NOTICE.txt','LICENSING.md':ROOT/'LICENSING.md'})
        if pack=='poj':files['paired-forms.tsv']=ROOT/'app/src/main/assets/paired-forms.tsv'
        if pack=='japanese':files['japanese-basic.tsv']=ROOT/'app/src/main/assets/japanese-basic.tsv'
        result.append(write_zip(out/('MinIME-'+pack+'-data.zip'),files))
    result.append(write_zip(out/'MinIME-geography-ODbL.zip',{'geography.tsv':ROOT/'app/src/main/assets/geography.tsv','NOTICE.txt':ROOT/'app/src/main/assets/addon-sources.txt','SOURCE.json':ROOT/'third_party/rudy/source.json'}))
    (out/'manifest.json').write_text(json.dumps({'status':'draft; unresolved release rights review remains','archives':result},indent=2)+'\n',encoding='utf-8');print(json.dumps(result,indent=2))
if __name__=='__main__':main()
