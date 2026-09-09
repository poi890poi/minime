"""Join frozen output lists with Android font/viewport measurements."""
from pathlib import Path
from collections import Counter,defaultdict
import csv,gzip,hashlib,json,math,statistics,io
ROOT=Path(__file__).resolve().parent.parent;OUT=ROOT/'docs/input-modes'
def gz(path):
    with gzip.open(str(path),'rt',encoding='utf8') as f:return [json.loads(line) for line in f]
def compress(data):
    out=io.BytesIO()
    with gzip.GzipFile(fileobj=out,mode='wb',mtime=0) as f:f.write(data)
    return out.getvalue()
def table(path):
    with path.open(encoding='utf8') as f:return list(csv.DictReader(f,delimiter='\t'))
inputs=[line.split('\t') for line in (OUT/'coverage-inputs.tsv').read_text(encoding='utf8').splitlines()]
before={(x['row'],x['mode']):x for x in gz(ROOT/'artifacts/modes-coverage-baseline.jsonl.gz')}
after=gz(ROOT/'artifacts/modes-coverage-candidate.jsonl.gz')
assert all(before[(x['row'],x['mode'])]==x for x in after),'Mode control output changed'
old_geometry=json.loads((ROOT/'artifacts/modes-geometry-baseline.json').read_text(encoding='utf8'))
new_geometry=json.loads((ROOT/'artifacts/modes-geometry-candidate.json').read_text(encoding='utf8'))
font_path=ROOT/'artifacts/modes-font-baseline.tsv';fonts={r['text']:{k:int(v) for k,v in r.items() if k!='text'} for r in table(font_path)}
assert font_path.read_bytes()==(ROOT/'artifacts/modes-font-candidate.tsv').read_bytes(),'Typography changed; cannot isolate geometry'
def visible(record,mode,geometry,raw,target):
    english=mode=='english';cells=record['candidates'];preferred=record['preferred'];start=1 if not english and preferred!=0 else 0
    frame=geometry[mode];divider=round(geometry['density']);row_width=frame['rowWidth'];page_width=frame['pageWidth'];page_height=frame['pageHeight']
    x=0;line=set();page=set();px=py=rowheight=0
    for i in range(start,len(cells)):
        text=cells[i][0];metrics=fonts[text];width=metrics[('en_' if english else 'zh_')+('bold' if preferred==i else 'normal')]
        if x+width<=row_width:line.add(i)
        x+=width+divider
        w,h=metrics['page_width'],metrics['page_height']
        if px>0 and px+w>page_width:px=0;py+=rowheight;rowheight=0
        if px+w<=page_width and py+h<=page_height:page.add(i)
        px+=w;rowheight=max(rowheight,h)
    matches={i for i,c in enumerate(cells) if c[0]==target and c[2]==0}
    return dict(n=1,row=int(bool(matches&line)),page=int(bool(matches&page)),any=int(bool(matches)),highlight=int(preferred in matches),raw_available=int(raw==target))
counts=defaultdict(lambda:defaultdict(Counter))
per_case=[]
for record in after:
    row=record['row'];group,doc,identity,condition,raw,target,context=inputs[row];mode=record['mode']
    original=before[(row,'legacy-english' if mode=='english' else 'legacy-mixed')]
    stages={'original_all':visible(original,mode,old_geometry,raw,target),'scoped_old_geometry':visible(record,mode,old_geometry,raw,target),'scoped_new_geometry':visible(record,mode,new_geometry,raw,target)}
    key=(group,condition,mode)
    for stage,values in stages.items():counts[key][stage].update(values)
    per_case.append(dict(row=row,mode=mode,stages=stages))
coverage=[]
for (group,condition,mode),stages in sorted(counts.items()):
    applicable=(group.startswith('en-') or (group=='zh-essay' and mode!='english') or (group=='japanese-retrieval' and mode=='japanese') or (group=='poj-retrieval' and mode=='taiwanese'))
    coverage.append(dict(group=group,condition=condition,mode=mode,applicable=applicable,stages={name:dict(values) for name,values in stages.items()}))
def stats(rows,key):
    values=sorted(int(r[key]) for r in rows);return dict(n=len(values),mean_us=statistics.mean(values),p50_us=statistics.median(values),p95_us=values[math.ceil(len(values)*.95)-1],max_us=values[-1])
latency={}
for phase in ('baseline','candidate'):
    path=ROOT/('artifacts/modes-latency-'+phase+'.tsv')
    if not path.exists():continue
    values=table(path);by_mode={}
    for mode in sorted({r['mode'] for r in values}):
        rows=[r for r in values if r['mode']==mode]
        by_mode[mode]=dict(overall=stats(rows,'last_key_us'),conditions={c:stats([r for r in rows if r['condition']==c],'last_key_us') for c in sorted({r['condition'] for r in rows})})
    latency[phase]=by_mode
result=dict(control_pairs=len(after),exact_control_parity=True,baseline_geometry=old_geometry,candidate_geometry=new_geometry,coverage=coverage,latency=latency,definition='Fully visible whole-input target in initial row / expanded viewport, using measured Android cell sizes. Raw spelling availability and highlight are separate. Shared dictionary controls have exact output parity; geometry-only loss is separate from language-scope effects. No natural conversation coverage is claimed for Taiwan.md essays or dictionary retrieval probes.')
(OUT/'results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
(OUT/'visibility-cases.jsonl.gz').write_bytes(compress((''.join(json.dumps(r)+'\n' for r in per_case)).encode()))
for name in ('modes-coverage-baseline.jsonl.gz','modes-coverage-candidate.jsonl.gz'):(OUT/name).write_bytes((ROOT/'artifacts'/name).read_bytes())
for name in ('modes-font-baseline.tsv','modes-latency-baseline.tsv','modes-latency-candidate.tsv'):
    path=ROOT/'artifacts'/name
    if path.exists():(OUT/(name+'.gz')).write_bytes(compress(path.read_bytes()))
print(json.dumps({'parity':len(after),'latency':latency},indent=2))
for row in coverage:
    if row['applicable'] and row['condition'] in ('full','half'):
        a=row['stages']['original_all'];b=row['stages']['scoped_new_geometry']
        print(row['group'],row['condition'],row['mode'],'n',a['n'],'row',a['row'],b['row'],'page',a['page'],b['page'])
