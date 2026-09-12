"""Run the frozen controlled experiment locally; no Android or production mutation."""
from pathlib import Path
import subprocess,sys,json,gzip,hashlib,argparse
R=Path(__file__).resolve().parent.parent;W=R/'artifacts/null-construction';O=R/'docs/null-construction'
p=argparse.ArgumentParser();p.add_argument('stage',choices=['native','replay','english','effort','timing']);a=p.parse_args()
def run(args):subprocess.run(list(map(str,args)),cwd=R,check=True)
java=Path('C:/Program Files/Microsoft/jdk-17.0.11.9-hotspot/bin/java.exe')
def command(variant):
    enabled=str(variant in ('java','both')).lower()
    return [java,'-Dfile.encoding=UTF-8','-Xmx2g','-Dminime.experiment.characterConstruction='+enabled,'-Dminime.experiment.readingConstruction='+enabled,'-cp',W/'classes']
if a.stage=='native':
    for kind in ['off','on']:
        output=O/('native-'+kind)
        for role in ['development','reserved']:
            cmd=[sys.executable,'-X','utf8',R/'tools/benchmark_native_construction.py','--role',role,'--output-dir',output,'--probe',W/('native-'+kind+'.exe')]
            if kind=='off':cmd+=['--require-no-assembly']
            run(cmd)
        run([sys.executable,'-X','utf8',R/'tools/make_construction_replay.py','--input-dir',output,'--output-dir',W/('native-'+kind)])
elif a.stage=='replay':
    for role,order in [('development',['null','native','java','both']),('reserved',['both','java','native','null'])]:
        for variant in order:
            native='on' if variant in ('native','both') else 'off'
            output=W/(role+'-'+variant+'.jsonl')
            run(command(variant)+['dev.minime.core.NullConstructionReplay',W/('native-'+native)/(role+'-replay.tsv.gz'),output,variant])
            (O/(output.name+'.gz')).write_bytes(gzip.compress(output.read_bytes(),mtime=0))
elif a.stage=='english':
    rows=[s for s in (R/'docs/conversation-ranking/corpus/gum-test.tsv').read_text(encoding='utf-8').splitlines() if s.startswith(('en-gum-conversation\t','en-gum-essay\t'))]
    corpus=O/'english.tsv';corpus.write_text('\n'.join(rows)+'\n',encoding='utf-8')
    for variant in ['null','native','java','both']:
        native='on' if variant in ('native','both') else 'off';output=O/('english-'+variant+'.jsonl')
        run(command(variant)+['dev.minime.core.DesktopEvaluation',corpus,output,W/('native-'+native+'.exe'),R/'.tools/rime-evaluation/msvc/dist/lib/rime.dll',R/'app/src/main/rimeAssets/rime',W/('english-user-'+variant)])
elif a.stage=='effort':
    labels=json.loads(gzip.decompress((R/'docs/construction-confidence/inputs.json.gz').read_bytes()))
    selected=[]
    for role in ['development','reserved']:
        for condition in ['full','initials','mixed','partial']:
            rows=[r for r in labels if r['role']==role and r['condition']==condition]
            selected+=sorted(rows,key=lambda r:hashlib.sha256(('null-effort-v1\0'+r['document']+'\0'+r['raw']).encode()).hexdigest())[:64]
    corpus=O/'effort.tsv';corpus.write_text(''.join('\t'.join(r[k] for k in ['role','condition','raw','target','document'])+'\n' for r in selected),encoding='utf-8')
    for variant in ['null','native','java','both']:
        native='on' if variant in ('native','both') else 'off';output=O/('effort-'+variant+'.jsonl')
        run(command(variant)+['dev.minime.core.NullConstructionEffort',corpus,output,W/('native-'+native+'.exe'),R/'.tools/rime-evaluation/msvc/dist/lib/rime.dll',R/'app/src/main/rimeAssets/rime',W/('effort-user-'+variant)])
else:
    # Same hash-selected inputs across providers; reference text does not select cases.
    for native in ['on','off']:
        blocks=gzip.decompress((W/('native-'+native)/'development-replay.tsv.gz').read_bytes()).decode().strip().split('\nEND\n')
        selected=[]
        for condition in ['full','initials','mixed','partial']:
            values=[b.removesuffix('\nEND') for b in blocks if b.startswith(condition+'\t')]
            selected+=sorted(values,key=lambda b:hashlib.sha256(('null-timing-v1\0'+b.split('\n')[0].split('\t')[2]).encode()).hexdigest())[:32]
        (W/('timing-'+native+'.tsv.gz')).write_bytes(gzip.compress(('\nEND\n'.join(selected)+'\nEND\n').encode(),mtime=0))
    for pass_id,order in enumerate([['null','native','java','both'],['both','java','native','null'],['java','null','both','native']]):
        for variant in order:
            native='on' if variant in ('native','both') else 'off';output=O/('timing-'+str(pass_id)+'-'+variant+'.jsonl')
            run(command(variant)+['dev.minime.core.NullConstructionReplay',W/('timing-'+native+'.tsv.gz'),output,variant])
