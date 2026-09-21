from pathlib import Path
import csv,collections,json,functools
root=Path.cwd();out=root/'artifacts/retrieval-losses';out.mkdir(exist_ok=True)
sources=collections.defaultdict(list)
for name in ['zh_tw.tsv','addons.tsv','geography.tsv']:
    with (root/'app/src/main/assets'/name).open(encoding='utf-8') as f:
        for line in f:
            p=line.rstrip('\n').split('\t')
            if len(p)>=3:sources[p[2]].append((name,p[0] if name=='zh_tw.tsv' else p[1]))
def match(raw,reading):
    units=reading.lower().replace('-','\'').replace(' ','\'').split("'")
    @functools.lru_cache(None)
    def go(i,at):
        if i==len(units):return at==len(raw)
        unit=units[i]
        for used in range(1,len(unit)+1):
            if not raw.startswith(unit[:used],at):break
            end=at+used
            if end<len(raw) and raw[end]=="'":end+=1
            if go(i+1,end):return True
        return False
    return go(0,0)
cases=[];groups=collections.Counter()
with (root/'artifacts/match-evidence/chinese.tsv').open(encoding='utf-8') as f:
    for row in csv.DictReader(f,delimiter='\t'):
        if row['target'] and row['target'] in sources and int(row['target_rank'])==0:
            aliases=sources[row['target']];raw=row['raw']
            matches=[(asset,r,'units' if match(raw,r) else 'prefix') for asset,r in aliases if match(raw,r) or r.replace("'",'').startswith(raw)]
            groups['compatible' if matches else 'incompatible']+=1
            cases.append({k:row[k] for k in ['genre','id','condition','raw','target']}|{'aliases':aliases,'compatible':matches,'first8':row['outputs'].split('|')[:8]})
(out/'misses.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(groups)
for row in cases[:30]:print(row['raw'],row['target'],row['compatible'][:2],row['aliases'][:2])
