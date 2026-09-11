"""Frozen development screen; labels never reach the native predictor."""
from pathlib import Path
import collections, gzip, hashlib, json, queue, statistics, subprocess, sys, threading, time
HERE=Path(__file__).resolve().parent
PRIOR=HERE.parent/'japanese-predictive-review'
sys.path.insert(0,str(HERE.parent/'japanese-engine-benchmark'))
from run import memory
import jaconv
WORK=HERE.parents[1]/'artifacts/japanese-engine-benchmark'

def read(path):return [json.loads(s) for s in gzip.open(path,'rt',encoding='utf8')]
def write(path,rows):path.write_bytes(gzip.compress(''.join(json.dumps(x,ensure_ascii=False)+'\n' for x in rows).encode(),mtime=0))
def dump(path,x):path.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def quantiles(a):
    a=sorted(a)
    return {'n':len(a),'mean':statistics.mean(a),'p50':statistics.median(a),'p95':a[int(len(a)*.95)],'p99':a[int(len(a)*.99)],'max':max(a)}

class Worker:
    def __init__(self,mode='bounded',directory=None):
        self.p=subprocess.Popen([str(WORK/'build/predictive-server-bounded.exe'),str(directory or WORK/'build'),mode],
            stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,text=True,encoding='utf8',bufsize=1)
        self.q=queue.Queue()
        def reader():
            for line in self.p.stdout:self.q.put(line)
            self.q.put(None)
        self.thread=threading.Thread(target=reader,daemon=True);self.thread.start()
        try:
            h=self.line(10).strip().split('\t');assert h[0]=='READY',h
            self.load={'engine_ns':int(h[1]),'memory':memory(self.p.pid)}
        except BaseException:
            self.p.kill();self.p.wait();raise
    def line(self,seconds):
        try:x=self.q.get(timeout=seconds)
        except queue.Empty:raise TimeoutError('Worker deadline')
        if x is None:raise RuntimeError('Worker exited')
        return x
    def query(self,raw):
        kana=jaconv.alphabet2kana(raw);assert '\n' not in kana and '\t' not in kana
        at=time.perf_counter_ns();self.p.stdin.write(kana+'\n');self.p.stdin.flush()
        p=self.line(5).rstrip('\r\n').split('\t')
        return dict(engine_ns=int(p[0]),wall_ns=time.perf_counter_ns()-at,available=p[1]=='1',checks=int(p[2]),choices=p[3:])
    def close(self):
        self.load['final_memory']=memory(self.p.pid)
        self.p.stdin.close()
        try:self.p.wait(timeout=1)
        except subprocess.TimeoutExpired:self.p.kill();self.p.wait(timeout=5)
        self.thread.join(timeout=1);self.p.stdout.close()

def main():
    manifest=json.loads((PRIOR/'manifest.json').read_text(encoding='utf8'))
    assert hashlib.sha256((PRIOR/'probes.jsonl.gz').read_bytes()).hexdigest()==manifest['probe_sha256']
    probes=read(PRIOR/'probes.jsonl.gz');all_rows=[];summary={'passes':{},'probe_sha256':manifest['probe_sha256']}
    for number in [1,2,3]:
        w=Worker();rows=[]
        try:
            for probe in probes:
                r={**probe,**w.query(probe['input'])}
                assert r['available'] or not r['choices'], 'Truncated ranking exposed'
                r['hit']=r['available'] and r['target'] in r['choices'];rows.append(r)
        finally:w.close()
        write(HERE/f'bounded-{number}.jsonl.gz',rows);dump(HERE/f'bounded-{number}-load.json',w.load)
        groups=collections.defaultdict(list)
        for r in rows:groups[r['source']+'/'+r['unit']+'/'+r['condition']].append(r)
        metrics={'engine_ms':quantiles([r['engine_ns']/1e6 for r in rows]),'wall_ms':quantiles([r['wall_ns']/1e6 for r in rows]),
            'unavailable':sum(not r['available'] for r in rows),'groups':{k:{'n':len(v),'available':sum(r['available'] for r in v),'hits':sum(r['hit'] for r in v)} for k,v in groups.items()}}
        metrics['latency_gate']=metrics['engine_ms']['p95']<=10 and metrics['engine_ms']['max']<=16
        summary['passes'][str(number)]=metrics;all_rows+=rows
        print('pass',number,metrics['engine_ms'],'unavailable',metrics['unavailable'],flush=True)
    # Compare completed outputs with the previously built uncapped upstream API.
    import importlib.util
    spec=importlib.util.spec_from_file_location('prior_predictive_evaluate',PRIOR/'evaluate.py')
    prior=importlib.util.module_from_spec(spec);spec.loader.exec_module(prior)
    reference=prior.Worker('indexed-lexical-fullprefix');parity=[]
    completed={}
    for r in all_rows:
        if r['available']:
            if r['input'] in completed:assert completed[r['input']]==r['choices']
            completed[r['input']]=r['choices']
    try:
        for raw,choices in completed.items():
            ref=reference.query(raw);parity.append({'input':raw,'choices':choices,'reference':ref['choices'],'equal':choices==ref['choices']})
    finally:reference.close();reference.p.stdout.close()
    write(HERE/'parity.jsonl.gz',parity)
    summary['parity']={'unique_completed_inputs':len(parity),'differences':sum(not r['equal'] for r in parity)}
    availability=collections.defaultdict(set)
    for r in all_rows:availability[r['id']+'/'+r['unit']+'/'+r['condition']].add(r['available'])
    summary['availability_changed_between_passes']=sum(len(s)>1 for s in availability.values())
    dump(HERE/'summary.json',summary)
    assert summary['parity']['differences']==0
    if not all(p['latency_gate'] for p in summary['passes'].values()):raise SystemExit('FAIL latency screen; retain negative results')
    print('PASS latency and complete-list parity screen; inspect unavailable coverage before promotion')

if __name__=='__main__':main()
