"""Compare frozen desktop outputs; keep acceptance and retrieval metrics separate."""
from pathlib import Path
from collections import Counter,defaultdict
import gzip,hashlib,json,sys
def read(path):
    data=path.read_bytes();return gzip.decompress(data) if path.suffix=='.gz' else data
def compare(before,after):
    left,right=read(before),read(after);a=[json.loads(s) for s in left.decode('utf8').splitlines()];b=[json.loads(s) for s in right.decode('utf8').splitlines()]
    assert len(a)==len(b);english=Counter();zh=defaultdict(Counter);changes=[];changed_records=0
    for x,y in zip(a,b):
        assert (x['group'],x['id'],x.get('mode'))==(y['group'],y['id'],y.get('mode'))
        changed_records+=x!=y
        if 'tokens' in x:
            assert len(x['tokens'])==len(y['tokens'])
            for u,v in zip(x['tokens'],y['tokens']):
                assert u['raw']==v['raw'];english[x['mode']+'_tokens']+=1
                if u['output']!=v['output']:
                    english[x['mode']+'_changed']+=1;changes.append(dict(group=x['group'],mode=x['mode'],before=u,after=v))
        else:
            g=zh[x['group']];g['n']+=1
            for label,z in [('before',x),('after',y)]:
                g[label+'_top1']+=z['rank']==1;g[label+'_top8']+=0<z['rank']<=8
            if x['output']!=y['output']:
                g['space_changes']+=1;changes.append(dict(group=x['group'],input=x['input'],before=x['output'],after=y['output']))
    return dict(records=len(a),changed_records=changed_records,before_sha256=hashlib.sha256(left).hexdigest(),after_sha256=hashlib.sha256(right).hexdigest(),english=dict(english),chinese={k:dict(v) for k,v in zh.items()},acceptance_changes=changes)
if __name__=='__main__':
    report=compare(Path(sys.argv[1]),Path(sys.argv[2]));Path(sys.argv[3]).write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode());print(json.dumps({k:v for k,v in report.items() if k!='acceptance_changes'}))
