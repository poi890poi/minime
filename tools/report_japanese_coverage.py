"""Record source retrieval, genre-specific mode tradeoffs, and raw telemetry."""
from pathlib import Path
from collections import Counter,defaultdict
import csv,gzip,hashlib,io,json,math,statistics,subprocess
ROOT=Path(__file__).resolve().parent.parent;OUT=ROOT/'docs/japanese-coverage';RUN=ROOT/'artifacts/japanese-coverage'

def table(path):
    with path.open(encoding='utf8') as f:return list(csv.DictReader(f,delimiter='\t'))
def gz(path):
    with gzip.open(str(path),'rt',encoding='utf8') as f:return [json.loads(x) for x in f]
def compress(path,dest):
    with dest.open('wb') as out:
        with gzip.GzipFile(fileobj=out,mode='wb',mtime=0) as f:f.write(path.read_bytes())
def stats(values):
    v=sorted(values);return dict(n=len(v),mean_us=statistics.mean(v),p50_us=statistics.median(v),p95_us=v[math.ceil(len(v)*.95)-1],max_us=v[-1])

def main():
    before=table(RUN/'baseline-retrieval.tsv');after=table(RUN/'current-retrieval.tsv')
    assert len(before)==len(after)
    for a,b in zip(before,after):assert all(a[k]==b[k] for k in ('condition','id','reading','input','target'))
    retrieval={}
    for stage,rows in [('before',before),('after',after)]:
        retrieval[stage]={}
        for condition in sorted({r['condition'] for r in rows}):
            group=[r for r in rows if r['condition']==condition]
            retrieval[stage][condition]=dict(n=len(group),top3=sum(0<int(r['rank'])<=3 for r in group),top8=sum(0<int(r['rank'])<=8 for r in group),any=sum(int(r['rank'])>0 for r in group),lookup=stats([int(r['lookup_ns'])/1000 for r in group]))
    original={(r['row'],r['mode']):r for r in gz(ROOT/'docs/mode-priority/candidate.jsonl.gz')}
    current=gz(RUN/'modes.jsonl.gz');counts=defaultdict(lambda:defaultdict(Counter));unchanged=Counter();changes=Counter()
    inputs=[line.split('\t') for line in (ROOT/'docs/input-modes/coverage-inputs.tsv').read_text(encoding='utf8').splitlines()]
    for b in current:
        a=original[(b['row'],b['mode'])];p=inputs[b['row']];mode=b['mode']
        (unchanged if a==b else changes)[mode]+=1
        if mode!='japanese':assert a==b,'Japanese data changed another mode'
        for stage,r in [('before',a),('after',b)]:
            matches=[i for i,c in enumerate(r['candidates']) if c[0]==p[5] and c[2]==0]
            counts[(p[0],p[3],mode)][stage].update(n=1,optional3=int(any(1<=i<=3 for i in matches)),optional8=int(any(1<=i<=8 for i in matches)),any=int(bool(matches)),highlight=int(r['preferred'] in matches),raw=int(0 in matches))
    old=set(subprocess.check_output(['git','show','fb85a39:app/src/main/assets/addons.tsv']).decode('utf8').splitlines())
    new=set((ROOT/'app/src/main/assets/addons.tsv').read_text(encoding='utf8').splitlines())
    assert old<=new,'Previous source row removed'
    added=new-old;assert all(r.startswith('japanese\t') for r in added)
    result=dict(baseline='fb85a39 ranking with 0.7.2 data',added_rows=len(added),removed_rows=0,retrieval=retrieval,mode_unchanged=dict(unchanged),mode_changed=dict(changes),mode_coverage=[dict(group=k[0],condition=k[1],mode=k[2],**{s:dict(v) for s,v in values.items()}) for k,values in sorted(counts.items())],limitations='Source-derived retrieval, not independent language accuracy. Mode corpus is previously exposed. First 3/8 are ordinal optional slots excluding raw, not measured viewport visibility. Lookup timing excludes native/core merge, rendering and input dispatch.')
    (OUT/'results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    for name in ('baseline-retrieval.tsv','current-retrieval.tsv','core.log','desktop.log'):
        compress(RUN/name,OUT/(name+'.gz'))
    (OUT/'modes.jsonl.gz').write_bytes((RUN/'modes.jsonl.gz').read_bytes())
    print(json.dumps({k:result[k] for k in ('added_rows','retrieval','mode_unchanged','mode_changed')},indent=2))
    for r in result['mode_coverage']:
        if r['mode']=='japanese' and r['condition'] in ('full','half'):print(r)

if __name__=='__main__':main()
