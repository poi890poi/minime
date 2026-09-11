"""Run frozen isolated experiments sequentially to avoid CPU competition."""
from pathlib import Path
import subprocess,sys,json,time
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'docs/language-contract-benchmark';WORK=ROOT/'artifacts/language-contract-benchmark'
JDK=Path('C:/Program Files/Microsoft/jdk-17.0.11.9-hotspot/bin')
variants=json.loads((OUT/'manifest.json').read_text(encoding='utf8'))['variants']
def run(cmd,name):
    print(name,flush=True)
    with (WORK/(name+'.log')).open('w',encoding='utf8') as log:
        p=subprocess.run([str(x) for x in cmd],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
    if p.returncode:print((WORK/(name+'.log')).read_text(encoding='utf8'));raise SystemExit(p.returncode)
def java(v,op,*args):return [JDK/'java.exe','-Dfile.encoding=UTF-8','-Xmx2g','-cp',WORK/v/'classes','dev.minime.core.ProposalBenchmark',v,op,*args]
if '--compile' in sys.argv:
    for v in variants:
        classes=WORK/v/'classes';classes.mkdir(exist_ok=True)
        run([JDK/'javac.exe','-encoding','UTF-8','-d',classes,*sorted((WORK/v/'src').glob('*.java')),ROOT/'core/src/test/java/dev/minime/core/SpeculationBenchmark.java',OUT/'ProposalBenchmark.java'],'compile-'+v)
elif '--coverage' in sys.argv:
    run(java('dedup','build'),'build-dedup')
    for v in variants:run(java(v,'coverage'),'coverage-'+v)
elif '--mechanics' in sys.argv:
    for v in ['baseline','span','english-context']:run(java(v,'mechanics'),'mechanics-'+v)
elif '--perf' in sys.argv:
    for i in range(3):
        for v in (variants if i%2==0 else list(reversed(variants))):run(java(v,'perf',str(i)),'perf-'+v+'-'+str(i))
elif '--conversations' in sys.argv:
    for v in variants:
        classes=WORK/v/'classes'
        run([JDK/'javac.exe','-encoding','UTF-8','-cp',classes,'-d',classes,OUT/'ConversationBenchmark.java'],'compile-conversations-'+v)
        run([JDK/'java.exe','-Dfile.encoding=UTF-8','-Xmx2g','-cp',classes,'dev.minime.core.ConversationBenchmark',v],'conversations-'+v)
elif '--working-set' in sys.argv:
    classes=WORK/'baseline/classes'
    run([JDK/'javac.exe','-encoding','UTF-8','-cp',classes,'-d',classes,OUT/'WorkingSetBenchmark.java'],'compile-working-set')
    for i in range(3):
        for policy in (['keep-all','warm-pair'] if i%2==0 else ['warm-pair','keep-all']):
            run([JDK/'java.exe','-Dfile.encoding=UTF-8','-Xmx2g','-cp',classes,'dev.minime.core.WorkingSetBenchmark',policy,str(i)],'working-set-'+policy+'-'+str(i))
