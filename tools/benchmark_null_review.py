"""Compare the review changes with the pinned no-construction baseline."""
from pathlib import Path
import argparse,subprocess,gzip,json,hashlib
R=Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser();p.add_argument('stage',choices=['visible','stored','ordered']);a=p.parse_args()
W=R/'artifacts/null-review'/a.stage;W.mkdir(parents=True,exist_ok=True);O=R/'docs/null-review'
sources=[];manifest={}
if a.stage=='visible':
    revision='6b48b69'
    paths=subprocess.check_output(['git','ls-tree','-r','--name-only',revision,'core/src/main/java'],cwd=R).decode().splitlines()
    for path in paths:
        if not path.endswith('.java'):continue
        source=subprocess.check_output(['git','show',revision+':'+path],cwd=R).decode('utf-8')
        if path.endswith('/PhoneticDictionary.java'):
            old='                if (paths.get(start).isEmpty()) continue;';assert source.count(old)==1
            source=source.replace(old,'                if (start > 0) continue;\n'+old)
        if path.endswith('/ReadingUnitIndex.java'):
            old='            List<Candidate> before=paths.get(start); trim(before,BEAM,false);';assert source.count(old)==1
            source=source.replace(old,'            if (start > 0) continue;\n'+old)
        target=W/'source'/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(source,encoding='utf-8');sources.append(target)
        manifest[path]=hashlib.sha256(source.encode()).hexdigest()
else:
    sources=list((R/'core/src/main/java').rglob('*.java'))
    manifest={str(s.relative_to(R)):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources}
(O/(a.stage+'-sources.json')).write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
sources.append(R/'core/src/test/java/dev/minime/core/NullConstructionReplay.java')
java=Path('C:/Program Files/Microsoft/jdk-17.0.11.9-hotspot/bin');classes=W/'classes';classes.mkdir(exist_ok=True)
subprocess.run([str(java/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*map(str,sources)],cwd=R,check=True)
for role in ['development','reserved']:
    output=W/(role+'.jsonl')
    subprocess.run([str(java/'java.exe'),'-Dfile.encoding=UTF-8','-Xmx2g','-cp',str(classes),'dev.minime.core.NullConstructionReplay',str(R/'artifacts/null-construction/native-off'/(role+'-replay.tsv.gz')),str(output),'null'],cwd=R,check=True)
    (O/(role+'-'+a.stage+'.jsonl.gz')).write_bytes(gzip.compress(output.read_bytes(),mtime=0))
