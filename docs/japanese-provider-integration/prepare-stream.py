"""Replay exactly the consumed desktop key stream; never use targets as input."""
import gzip,hashlib,json,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
sys.path.insert(0,str(HERE.parent/'japanese-engine-benchmark'))
from run import Engine
import jaconv

def main():
    path=HERE.parent/'japanese-determinism/kazuma-indexed-stable-perf-1.jsonl.gz'
    rows=[json.loads(s) for s in gzip.open(path,'rt',encoding='utf8') if s.strip()]
    keys=[r for r in rows if r['cycle']=='first'];out=[]
    engine=Engine('kazuma-portable')
    try:
        for r in keys:
            engine.send('R');engine.send('T',r['raw'])
            out.append({'id':r['id'],'key':r['key'],'raw':r['raw'],'kana':jaconv.alphabet2kana(r['raw']),
                        'expected':[c[0] for c in engine.candidates]})
    finally:engine.close()
    data=''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in out).encode()
    (HERE/'stream.jsonl.gz').write_bytes(gzip.compress(data,mtime=0))
    assets=ROOT/'app/build/generated/japaneseEvaluationAssets/japanese-evaluation';assets.mkdir(parents=True,exist_ok=True)
    (assets/'stream.jsonl').write_bytes(data)
    manifest={'role':'consumed development parity/performance stream','source':str(path.relative_to(ROOT)),
              'source_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'stream_sha256':hashlib.sha256(data).hexdigest(),
              'keys':len(out),'clauses':len(set(r['id'] for r in out)),
              'source_pins':'../japanese-engine-benchmark/source-manifest.json'}
    (HERE/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8')
    print('Prepared',len(out),'keys; frozen exact desktop references')

if __name__=='__main__':main()
