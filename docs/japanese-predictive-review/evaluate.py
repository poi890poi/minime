"""Frozen development screen; external workers receive typed kana, never labels."""
import argparse, collections, gzip, hashlib, json, queue, subprocess, sys, threading, time
from pathlib import Path
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
sys.path.insert(0,str(HERE.parent/'japanese-engine-benchmark'))
from run import Engine, memory
import jaconv

WORK=ROOT/'artifacts/japanese-engine-benchmark'
def digest(b):return hashlib.sha256(b).hexdigest()
def ordered(s):return digest(('predictive-v1:'+s).encode())
def write(path,rows):path.write_bytes(gzip.compress(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in rows).encode(),mtime=0))
def read(path):return [json.loads(x) for x in gzip.open(path,'rt',encoding='utf8')]
def dump(path,value):path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def freeze():
    corpus=read(HERE.parent/'japanese-engine-benchmark/utterances.jsonl.gz');probes=[]
    for unit in ['clause','word']:
        for source in ['real-persona-chat','asdc']:
            pool=[]
            for u in corpus:
                if u['source']!=source or u['role']!='development':continue
                for i,p in enumerate(u['parts']):
                    if p['kind']!='japanese':continue
                    items=[(str(i),p)] if unit=='clause' else [(str(i)+'/'+str(j),v) for j,v in enumerate(p['words'])]
                    for ident,v in items:
                        if v['valid'] and 2<=len(v['raw'])<=24:
                            pool.append({'id':u['id']+':'+ident,'raw':v['raw'],'target':v['text'],'document':u['document']})
            for v in sorted(pool,key=lambda x:ordered(x['id']))[:32]:
                for condition,raw in [('full',v['raw']),('half',v['raw'][:max(1,len(v['raw'])//2)]),('three-quarter',v['raw'][:max(1,len(v['raw'])*3//4)])]:
                    probes.append({**v,'unit':unit,'source':source,'condition':condition,'input':raw})
    write(HERE/'probes.jsonl.gz',probes)
    dump(HERE/'manifest.json',{'role':'previously evaluated development data','rows':len(probes),'salt':'predictive-v1:',
        'source_corpus_sha256':digest((HERE.parent/'japanese-engine-benchmark/utterances.jsonl.gz').read_bytes()),
        'probe_sha256':digest((HERE/'probes.jsonl.gz').read_bytes()),'source_pins':'../japanese-engine-benchmark/source-manifest.json',
        'counts':dict(collections.Counter(x['unit']+'/'+x['source'] for x in probes))})
    print('Frozen',len(probes),'probes')

class Worker:
    def __init__(self,mode,directory=None):
        indexed=mode.startswith('indexed-');mode=mode.removeprefix('indexed-')
        self.responses=queue.Queue();self.stderr=(WORK/f'predictive-{mode}-stderr.txt').open('w',encoding='utf8')
        executable='predictive-server-indexed.exe' if indexed else 'predictive-server.exe'
        self.p=subprocess.Popen([str(WORK/'build'/executable),str(directory or WORK/'build'),mode],stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,stderr=self.stderr,text=True,encoding='utf8',bufsize=1)
        def reader():
            for line in self.p.stdout:self.responses.put(line)
            self.responses.put(None)
        self.reader=threading.Thread(target=reader,daemon=True);self.reader.start()
        header=self.line(10).strip().split('\t');assert header[0]=='READY',header
        self.load={'engine_ns':int(header[1]),'memory':memory(self.p.pid)}
    def line(self,timeout):
        try:value=self.responses.get(timeout=timeout)
        except queue.Empty:raise TimeoutError('Query deadline exceeded')
        if value is None:raise RuntimeError('Worker exited before response')
        return value
    def query(self,raw):
        kana=jaconv.alphabet2kana(raw);assert '\t' not in kana and '\n' not in kana
        at=time.perf_counter_ns();self.p.stdin.write(kana+'\n');self.p.stdin.flush()
        p=self.line(5).rstrip('\r\n').split('\t')
        return {'engine_ns':int(p[0]),'wall_ns':time.perf_counter_ns()-at,'nodes':int(p[1]),'mismatched_nodes':int(p[2]),'choices':p[3:]}
    def close(self):
        self.load['final_memory']=memory(self.p.pid)
        self.p.stdin.close()
        try:self.p.wait(timeout=1)
        except subprocess.TimeoutExpired:self.p.kill();self.p.wait(timeout=5)
        self.reader.join(timeout=1);self.stderr.close()

def should_stop(rows):
    return any(r.get('timeout') for r in rows) or sum(r.get('engine_ns',0)>250_000_000 for r in rows)>=3

def run(mode,unit):
    manifest=json.loads((HERE/'manifest.json').read_text(encoding='utf8'))
    assert digest((HERE/'probes.jsonl.gz').read_bytes())==manifest['probe_sha256']
    probes=[r for r in read(HERE/'probes.jsonl.gz') if r['unit']==unit];output=[]
    worker=Engine('minime') if mode=='minime' else Worker(mode)
    try:
        for probe in probes:
            try:
                if mode=='minime':
                    worker.send('R');worker.send('T',probe['input'])
                    response={'engine_ns':worker.times[-1],'typed_sequence_engine_ns':worker.engine_ns,'wall_ns':worker.wall_ns,
                              'choices':[text if consumed==0 else '' for text,consumed in worker.candidates[:8]]}
                else:response=worker.query(probe['input'])
                result={**probe,**response,'timeout':False,'hit':probe['target'] in response['choices']}
            except TimeoutError:result={**probe,'timeout':True,'hit':False,'wall_ns':5_000_000_000,'choices':[]}
            output.append(result)
            if should_stop(output):break
    finally:worker.close()
    name=mode+'-'+unit
    write(HERE/(name+'.jsonl.gz'),output)
    dump(HERE/(name+'-load.json'),{**worker.load,'attempted':len(output),'planned':len(probes),'stopped':should_stop(output),
        'unrun':len(probes)-len(output)})
    print(name,len(output),'attempted;',sum(x['hit'] for x in output),'hits; stopped',should_stop(output),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('mode',choices=['freeze','cps','graph','lexical','lexical-fullprefix','minime','indexed-cps','indexed-lexical','indexed-lexical-fullprefix']);parser.add_argument('--unit',default='clause',choices=['clause','word']);args=parser.parse_args()
    if args.mode=='freeze':freeze()
    else:run(args.mode,args.unit)
