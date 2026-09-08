"""Compare aligned frozen desktop runs without interpreting changes as accuracy."""
import collections, gzip, hashlib, io, json
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'docs/dictionary-impact'
def read(name):
    data=(ROOT/name).read_bytes()
    return data,[json.loads(line) for line in data.decode('utf-8').splitlines()]
def compare(before,after):
    a,old=read(before);b,new=read(after);assert len(old)==len(new)
    differences=[];groups={};english=collections.Counter()
    for x,y in zip(old,new):
        assert (x['group'],x['id'],x.get('mode'))==(y['group'],y['id'],y.get('mode'))
        if 'tokens' in x:
            assert len(x['tokens'])==len(y['tokens'])
            english['tokens']+=len(x['tokens'])
            for u,v in zip(x['tokens'],y['tokens']):
                assert u['raw']==v['raw']
                if u['output']!=v['output']:
                    english['changed']+=1
                    differences.append(dict(group=x['group'],id=x['id'],mode=x['mode'],raw=u['raw'],before=u['output'],after=v['output']))
        else:
            g=groups.setdefault(x['group'],dict(n=0,before_top1=0,after_top1=0,before_top8=0,after_top8=0,space_changes=0))
            g['n']+=1
            for prefix,row in [('before',x),('after',y)]:
                g[prefix+'_top1']+=row['rank']==1;g[prefix+'_top8']+=0<row['rank']<=8
            if x['output']!=y['output']:
                g['space_changes']+=1
                differences.append(dict(group=x['group'],id=x['id'],raw=x['input'],expected=x['expected'],before=x['output'],after=y['output']))
    return dict(records=len(old),before_sha256=hashlib.sha256(a).hexdigest(),after_sha256=hashlib.sha256(b).hexdigest(),english=dict(english),chinese=groups,changes=differences)
def compress(source,name):
    buffer=io.BytesIO()
    with gzip.GzipFile(fileobj=buffer,mode='wb',mtime=0) as output:output.write((ROOT/source).read_bytes())
    (OUT/name).write_bytes(buffer.getvalue())
if __name__=='__main__':
    reports={}
    for name,before,after in [
        ('unrestricted-rejected','artifacts/desktop-impact-controlled-baseline.jsonl','artifacts/desktop-impact-controlled-final.jsonl'),
        ('blanket-order-rejected','artifacts/desktop-impact-controlled-baseline.jsonl','artifacts/desktop-impact-safe.jsonl'),
        ('ordering','artifacts/desktop-impact-controlled-baseline.jsonl','artifacts/desktop-impact-order-protected.jsonl'),
        ('new-wikipedia','artifacts/desktop-impact-fresh-baseline.jsonl','artifacts/desktop-impact-new-protected.jsonl'),
        ('apostrophe','artifacts/desktop-impact-order-protected.jsonl','artifacts/desktop-impact-final-protected.jsonl'),
        ('gum','artifacts/desktop-impact-gum-baseline.jsonl','artifacts/desktop-impact-gum-final.jsonl')]:
        reports[name]=compare(before,after)
    (OUT/'apostrophe-evaluation.json').write_bytes((json.dumps(reports,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
    for source,name in [
        ('desktop-impact-controlled-baseline','conversation-baseline'),('desktop-impact-controlled-final','unrestricted-rejected'),
        ('desktop-impact-safe','blanket-order-rejected'),('desktop-impact-order-protected','order-protected'),
        ('desktop-impact-new-protected','new-protected'),('desktop-impact-final-protected','conversation-after'),
        ('desktop-impact-gum-baseline','gum-baseline'),('desktop-impact-gum-final','gum-after')]:
        compress('artifacts/'+source+'.jsonl',name+'.jsonl.gz')
    for name,report in reports.items():
        print(name,json.dumps({k:v for k,v in report.items() if k not in ['changes','chinese']}))
        print('Chinese Space changes:',sum(v['space_changes'] for v in report['chinese'].values()))
        print('Changed forms:',sorted({(x['raw'],x['before'],x['after']) for x in report['changes']}))
