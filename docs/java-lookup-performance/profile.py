import collections, json
from pathlib import Path
p=Path('artifacts/java-lookup/profile.json')
events=json.loads(p.read_text(encoding='utf-8-sig'))['recording']['events']
top=collections.Counter(); units=trim=lookup=0
for event in events:
    frames=(event['values'].get('stackTrace') or {}).get('frames',[])
    names=[f['method']['type']['name']+'.'+f['method']['name'] for f in frames]
    lookup+=any(name.endswith('AddonDictionary.lookup') for name in names)
    if any('ReadingUnitIndex' in name for name in names):
        units+=1; top.update(names[:1])
        trim+=any(name.endswith('ReadingUnitIndex.trim') for name in names)
result={'execution_samples':len(events),'addon_lookup_stacks':lookup,
        'reading_unit_stacks':units,'trim_in_reading_unit_stacks':trim,
        'reading_unit_top_frames':top.most_common(20),
        'limitation':'Sampling is causal profiling evidence, not an exact CPU accounting.'}
Path('docs/java-lookup-performance/baseline-profile.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
