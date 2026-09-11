import gzip,json,statistics
from pathlib import Path
OUT=Path('docs/java-lookup-performance')
RAW=Path('artifacts/java-lookup')
def stats(a):
    a=sorted(a)
    return {'n':len(a),'mean_ms':statistics.mean(a)/1e6,'p50_ms':a[len(a)//2]/1e6,
            'p95_ms':a[int(len(a)*.95)]/1e6,'p99_ms':a[int(len(a)*.99)]/1e6,'max_ms':max(a)/1e6}
result={'direct_lookup':{},'pipeline':{},'physical_touch_measured':False}
references={}
pipeline_references={}
for name in ['baseline','candidate']:
    path=RAW/(name+'-phone.jsonl')
    rows=[json.loads(line) for line in path.read_text(encoding='utf-8-sig').splitlines()]
    assert len(rows)==2550
    groups={}
    for row in rows:
        key=(row['pass'],row['row'])
        fields=(row['count'],row['digest'])
        if name=='baseline':references[key]=fields
        else:assert references[key]==fields,(name,key)
        for field in ['pack','group','condition']:
            group=f"{field}/{row[field]}/{'first' if row['pass']==0 else 'warm'}"
            groups.setdefault(group,[]).append(row)
    result['direct_lookup'][name]={g:{**stats([r['ns'] for r in v]),'empty_outputs':sum(r['count']==0 for r in v)} for g,v in groups.items()}
    (OUT/(name+'-phone.jsonl.gz')).write_bytes(gzip.compress(path.read_bytes(),mtime=0))
    path=RAW/(name+'-pipeline.jsonl')
    rows=[json.loads(line) for line in path.read_text(encoding='utf-8-sig').splitlines()]
    summary=next(r for r in rows if r['kind']=='summary')
    assert summary['ordered_differences']==0
    result['pipeline'][name]={'summary':summary}
    for cycle in range(3):
        v=[r for r in rows if r['kind']=='pipeline' and r['cycle']==cycle]
        assert len(v)==64
        for row in v:
            key=(cycle,row['id']);fields=(row['committed'],row['candidates'])
            if name=='baseline':pipeline_references[key]=fields
            else:assert pipeline_references[key]==fields,('pipeline acceptance',key)
        result['pipeline'][name][str(cycle)]={k:stats([r[k] for r in v]) for k in ['elapsed_ns','last_key_ns','dispatch_ns','addon_and_conversion_ns']}
    (OUT/(name+'-pipeline.jsonl.gz')).write_bytes(gzip.compress(path.read_bytes(),mtime=0))
result['ordered_public_candidate_metadata_comparisons']=2550
result['pipeline_acceptance_comparisons']=192
(OUT/'phone-results.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
for name in result['direct_lookup']:
    print(name,{k:v for k,v in result['direct_lookup'][name].items() if k.startswith('pack/') and k.endswith('/warm')})
    print(name,result['pipeline'][name]['summary'])
