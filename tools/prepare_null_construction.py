"""Export an isolated, pinned Java experiment. Production sources stay untouched."""
from pathlib import Path
import subprocess,hashlib,json
R=Path(__file__).resolve().parent.parent
W=R/'artifacts/null-construction';W.mkdir(parents=True,exist_ok=True)
REV='7f0f179'
paths=subprocess.check_output(['git','ls-tree','-r','--name-only',REV,'core/src/main/java'],cwd=R).decode().splitlines()
manifest={'revision':REV,'scope':'exported experiment only; no runtime source edits','files':{}}
for path in paths:
    if not path.endswith('.java'):continue
    original=subprocess.check_output(['git','show',REV+':'+path],cwd=R)
    source=original.decode('utf-8');guard=None
    if path.endswith('/PhoneticDictionary.java'):
        anchor='                if (paths.get(start).isEmpty()) continue;'
        guard='                if (!EXPERIMENT_JOIN && start > 0) continue;\n'+anchor
        declaration='public final class PhoneticDictionary {'
        prop='characterConstruction'
    elif path.endswith('/ReadingUnitIndex.java'):
        anchor='            List<Candidate> before=paths.get(start); trim(before,BEAM,false);'
        guard='            if (!EXPERIMENT_JOIN && start > 0) continue;\n'+anchor
        declaration='final class ReadingUnitIndex {'
        prop='readingConstruction'
    if guard:
        assert source.count(anchor)==1 and source.count(declaration)==1
        source=source.replace(anchor,guard).replace(declaration,declaration+'\n    private static final boolean EXPERIMENT_JOIN=Boolean.getBoolean("minime.experiment.'+prop+'");')
    target=W/'source'/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(source,encoding='utf-8')
    manifest['files'][path]={'original_sha256':hashlib.sha256(original).hexdigest(),'experiment_sha256':hashlib.sha256(source.encode()).hexdigest(),'guard_added':bool(guard)}
out=R/'docs/null-construction';out.mkdir(exist_ok=True)
(out/'java-export.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
java=Path('C:/Program Files/Microsoft/jdk-17.0.11.9-hotspot/bin')
classes=W/'classes';classes.mkdir(exist_ok=True)
sources=list((W/'source').rglob('*.java'))+[R/'core/src/test/java/dev/minime/core'/name for name in ['NullConstructionReplay.java','DesktopEvaluation.java','NullConstructionContract.java','NullConstructionEffort.java']]
subprocess.run([str(java/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*map(str,sources)],cwd=R,check=True)
for character,reading in [('false','false'),('true','false'),('false','true'),('true','true')]:
    enabled=str(character=='true' or reading=='true').lower()
    subprocess.run([str(java/'java.exe'),'-Dfile.encoding=UTF-8','-Dminime.experiment.characterConstruction='+character,'-Dminime.experiment.readingConstruction='+reading,'-cp',str(classes),'dev.minime.core.NullConstructionContract',enabled],cwd=R,check=True)
print('Exported and checked isolated null/on Java classes.')
