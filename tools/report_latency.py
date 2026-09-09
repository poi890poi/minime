"""Compare immutable benchmark exports; timings are not language-model accuracy."""
import csv,gzip,io,json,math,statistics
from pathlib import Path
root=Path(__file__).resolve().parent.parent
out=root/'docs/suggestion-latency'
def compress(data):
    buffer=io.BytesIO()
    with gzip.GzipFile(fileobj=buffer,mode='wb',mtime=0) as f:f.write(data)
    return buffer.getvalue()
def rows(path):
    with path.open(encoding='utf8') as f:return list(csv.DictReader((line for line in f if not line.startswith('#')),delimiter='\t'))
def stats(data,key):
    values=sorted(int(row[key]) for row in data)
    return dict(n=len(values),p50_us=statistics.median(values),p95_us=values[math.ceil(len(values)*.95)-1],max_us=values[-1])
def compare(baseline,candidate,columns,signature):
    a,b=rows(root/'artifacts'/baseline),rows(root/'artifacts'/candidate)
    assert len(a)==len(b)
    for x,y in zip(a,b):
        assert all(x[k]==y[k] for k in signature),(x,y)
    result=dict(rows=len(a),candidate_output_parity=True,overall={},groups={})
    for name,key in columns.items():result['overall'][name]=dict(before=stats(a,key),after=stats(b,key))
    for group,condition in sorted({(x['group'],x['condition']) for x in a}):
        aa=[x for x in a if (x['group'],x['condition'])==(group,condition)]
        bb=[x for x in b if (x['group'],x['condition'])==(group,condition)]
        result['groups'][group+'/'+condition]={name:dict(before=stats(aa,key),after=stats(bb,key)) for name,key in columns.items()}
    for name in (baseline,candidate):
        (out/(name+'.gz')).write_bytes(compress((root/'artifacts'/name).read_bytes()))
    return result
desktop=compare('latency-baseline-unprofiled.tsv','latency-candidate-repeat.tsv',{'core':'core_us','addons':'addons_us'},['round','group','condition','query','core_n','addons_n','core_signature','addons_signature'])
report={'desktop':desktop,'phone':{}}
phone_a=root/'artifacts/latency-phone-baseline.tsv';phone_b=root/'artifacts/latency-phone-candidate.tsv'
if phone_a.exists() and phone_b.exists():
    a,b=rows(phone_a),rows(phone_b)
    assert len(a)==len(b)
    for x,y in zip(a,b):assert all(x[k]==y[k] for k in ('stage','round','group','condition','query','signature')),(x,y)
    report['phone']['candidate_output_parity']=True
    for stage in sorted({x['stage'] for x in a}):
        aa=[x for x in a if x['stage']==stage];bb=[x for x in b if x['stage']==stage]
        keys=['core_us','rime_us','addons_us'] if stage=='components' else ['delivery_us']
        report['phone'][stage]={key:dict(before=stats(aa,key),after=stats(bb,key)) for key in keys}
        report['phone'][stage]['conditions']={condition:{key:dict(before=stats([x for x in aa if x['condition']==condition],key),after=stats([x for x in bb if x['condition']==condition],key)) for key in keys} for condition in sorted({x['condition'] for x in aa})}
    for path in (phone_a,phone_b):(out/(path.name+'.gz')).write_bytes(compress(path.read_bytes()))
(out/'results.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
print(json.dumps({'desktop':desktop['overall'],'phone':report['phone']},indent=2))
