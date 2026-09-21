import json,csv,math,collections,functools,time
from pathlib import Path
root=Path.cwd();folder=root/'artifacts/retrieval-losses';misses=json.loads((folder/'misses.json').read_text(encoding='utf-8'))
entries=collections.defaultdict(list)
for line in (root/'app/src/main/assets/zh_tw.tsv').read_text(encoding='utf-8').splitlines():
    p=line.split('\t');entries[p[0]].append((p[2],math.log10(float(p[3])+1)-9))
def compatible(raw,units):
    @functools.lru_cache(None)
    def go(i,at):
        if i==len(units):return at==len(raw)
        if at>=len(raw):return False
        unit=units[i]
        for used in range(1,len(unit)+1):
            if not raw.startswith(unit[:used],at):break
            end=at+used
            if end<len(raw) and raw[end]=="'":end+=1
            if go(i+1,end):return True
        return False
    return go(0,0)
byinitial=collections.defaultdict(list)
for reading,values in entries.items():byinitial[reading[0]].append((reading,reading.split("'"),values))
results={}
for raw in sorted({r['raw'] for r in misses}):
    scores={}
    for reading,units,values in byinitial[raw[0]]:
        if len(units)>len(raw) or not compatible(raw,units):continue
        missing=len(reading.replace("'",''))-len(raw.replace("'",''));penalty=0 if missing==0 else .7+.08*missing
        for text,score in values:
            if text not in scores or score-penalty>scores[text]:scores[text]=score-penalty
    ordered=sorted(scores,key=lambda text:(-scores[text],text));lengths=collections.Counter();kept=[]
    for text in ordered:
        if len(kept)<128 or lengths[len(text)]<6:kept.append(text);lengths[len(text)]+=1
    results[raw]={'ranks':{text:i+1 for i,text in enumerate(ordered)},'kept':set(kept)}
for row in misses:
    ref=results[row['raw']];row['unit_reference_rank']=ref['ranks'].get(row['target'],0);row['unit_reference_retained']=row['target'] in ref['kept']
(folder/'reference-misses.json').write_text(json.dumps(misses,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(collections.Counter('unbounded-miss' if not r['unit_reference_rank'] else 'within-budget' if r['unit_reference_retained'] else 'candidate-cap' for r in misses))
for r in misses:
    if r['unit_reference_retained']:print(r['raw'],r['target'],r['unit_reference_rank'])
