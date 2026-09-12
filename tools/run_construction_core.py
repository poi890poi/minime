"""Reproducible core A/B, exporting the baseline without switching the checkout."""
from pathlib import Path
import argparse,subprocess,os
ROOT=Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser();p.add_argument('--variant',choices=['baseline','policy'],required=True);p.add_argument('--role',choices=['development','reserved'],required=True);args=p.parse_args()
baseline='385a7f0'
work=ROOT/'artifacts/native-metadata'/('core-'+args.variant);work.mkdir(parents=True,exist_ok=True)
classes=work/'classes';classes.mkdir(exist_ok=True)
if args.variant=='baseline':
    paths=subprocess.check_output(['git','ls-tree','-r','--name-only',baseline,'core/src/main/java'],cwd=ROOT).decode().splitlines();sources=[]
    for path in paths:
        if not path.endswith('.java'):continue
        target=work/'source'/path;target.parent.mkdir(parents=True,exist_ok=True)
        target.write_bytes(subprocess.check_output(['git','show',baseline+':'+path],cwd=ROOT));sources.append(str(target))
    # Baseline Android discarded native provenance. This evaluator-only adapter
    # transports exactly the same text/end/order without adding new runtime logic.
    codec=work/'NativeCandidateCodec.java'
    codec.write_text('''package dev.minime.core;
public final class NativeCandidateCodec {
  public static Candidate decode(String raw,String record,int rank) {
    String[] parts=record.split("\\t",3);int end=Integer.parseInt(parts[0]);
    return new Candidate(parts[2],false,100-rank,end==raw.length()?0:end);
  }
}
''',encoding='utf-8');sources.append(str(codec))
else:sources=[str(x) for x in (ROOT/'core/src/main/java').rglob('*.java')]
sources.append(str(ROOT/'core/src/test/java/dev/minime/core/ConstructionReplay.java'))
java=Path(os.environ.get('JAVA_HOME','C:/Program Files/Microsoft/jdk-17.0.11.9-hotspot'))/'bin'
subprocess.run([str(java/'javac.exe'),'-encoding','UTF-8','-d',str(classes),*sources],cwd=ROOT,check=True)
subprocess.run([str(java/'java.exe'),'-Dfile.encoding=UTF-8','-Xmx2g','-cp',str(classes),'dev.minime.core.ConstructionReplay',
    str(ROOT/'artifacts/native-metadata'/(args.role+'-replay.tsv.gz')),str(ROOT/'artifacts/native-metadata'/(args.role+'-core-'+args.variant+'.tsv'))],cwd=ROOT,check=True)
