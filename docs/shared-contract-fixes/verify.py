"""Replay frozen proposal corpora against a captured production source snapshot.

Usage: python docs/shared-contract-fixes/verify.py prefix coverage mechanics
       python docs/shared-contract-fixes/verify.py english coverage mechanics conversations
Labels score returned candidates only; no source refresh or experimental patching.
"""
from pathlib import Path
import subprocess,sys,json,hashlib,gzip,csv,collections
ROOT=Path(__file__).resolve().parents[2];BASE=ROOT/'docs/language-contract-benchmark'
stage=sys.argv[1];ops=sys.argv[2:];assert stage in ['prefix','english']
OUT=ROOT/'docs/shared-contract-fixes'/stage;WORK=ROOT/'artifacts/shared-contract-fixes'/stage
OUT.mkdir(parents=True,exist_ok=True);WORK.mkdir(parents=True,exist_ok=True)
JDK=Path('C:/Program Files/Microsoft/jdk-17.0.11.9-hotspot/bin')
def run(cmd,name):
    with (WORK/(name+'.log')).open('w',encoding='utf8') as f:p=subprocess.run([str(x) for x in cmd],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
    if p.returncode:print((WORK/(name+'.log')).read_text(encoding='utf8'));raise SystemExit(p.returncode)
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
sources=[]
for p in (ROOT/'core/src/main/java/dev/minime/core').glob('*.java'):
    copy=WORK/p.name;copy.write_bytes(p.read_bytes());sources.append(copy)
capture={'revision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT).decode().strip(),
 'source_hashes':{p.name:digest(p) for p in sources},'frozen_input_hashes':{str(p.relative_to(ROOT)):digest(p) for p in [BASE/'inputs.tsv',BASE/'conversations/inputs.tsv']},
 'role':'Regression replay; these corpora were already inspected. No new holdout or lexical-quality change.'}
(OUT/'snapshot.json').write_text(json.dumps(capture,indent=2)+'\n',encoding='utf8')
proposal=(BASE/'ProposalBenchmark.java').read_text(encoding='utf8').replace('OUT=Paths.get("docs/language-contract-benchmark")','OUT=Paths.get("'+OUT.relative_to(ROOT).as_posix()+'")').replace('OUT.resolve("inputs.tsv")','Paths.get("docs/language-contract-benchmark/inputs.tsv")')
(WORK/'ProposalBenchmark.java').write_text(proposal,encoding='utf8')
conversation=(BASE/'ConversationBenchmark.java').read_text(encoding='utf8').replace('Paths.get("docs/language-contract-benchmark/conversations")','Paths.get("'+OUT.relative_to(ROOT).as_posix()+'/conversations")').replace('OUT.resolve("inputs.tsv")','Paths.get("docs/language-contract-benchmark/conversations/inputs.tsv")')
(WORK/'ConversationBenchmark.java').write_text(conversation,encoding='utf8');(OUT/'conversations').mkdir(exist_ok=True)
run([JDK/'javac.exe','-encoding','UTF-8','-d',WORK/'classes',*sources,ROOT/'core/src/test/java/dev/minime/core/SpeculationBenchmark.java',WORK/'ProposalBenchmark.java',WORK/'ConversationBenchmark.java'],'compile')
for op in ops:
    print(stage,op,flush=True)
    cls='ConversationBenchmark' if op=='conversations' else 'ProposalBenchmark'
    run([JDK/'java.exe','-Dfile.encoding=UTF-8','-Xmx2g','-cp',WORK/'classes','dev.minime.core.'+cls,stage,*([] if op=='conversations' else [op])],op)
def rows(path):
    with gzip.open(path,'rt',encoding='utf8') as f:return list(csv.DictReader(f,delimiter='\t'))
result={}
for op in ops:
    if op=='mechanics':
        rs=rows(OUT/(stage+'-acceptance.tsv.gz'))
        result[op]={'controls':len(rs),'pass':sum(r['suffix_ok']=='true' for r in rs),'unavailable':sum(r['available']!='true' for r in rs),
          'private_learning_calls':sum(int(r['focus_learning'])+int(r['other_learning']) for r in rs if r['private']=='true')}
        assert result[op]['pass']==len(rs) and result[op]['private_learning_calls']==0
        cont=collections.defaultdict(collections.Counter)
        for r in rows(OUT/(stage+'-continuation.tsv.gz')):
            g=cont[r['mode']+'/'+r['private']];g['n']+=1;g['nonempty']+=int(r['idle_count'])>0;g['top8']+=0<=int(r['target_rank'])<8;g['learning_calls']+=int(r['learning_calls'])
        result['continuation']={k:dict(v) for k,v in cont.items()}
    elif op in ['coverage','conversations']:
        a=rows(BASE/('baseline-coverage.tsv.gz' if op=='coverage' else 'conversations/baseline.tsv.gz'))
        b=rows(OUT/(stage+'-coverage.tsv.gz' if op=='coverage' else 'conversations/'+stage+'.tsv.gz'))
        assert len(a)==len(b)
        result[op]={'rows':len(a),'order_changes':sum(x['signature']!=y['signature'] for x,y in zip(a,b)),
          'default_changes':sum(x['default_text']!=y['default_text'] for x,y in zip(a,b))}
        assert result[op]['order_changes']==0 and result[op]['default_changes']==0,result[op]
(OUT/'verification.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8');print(json.dumps(result,indent=2))
