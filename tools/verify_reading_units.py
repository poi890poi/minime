"""Verify build-time boundary enrichment preserves every normalized source alias."""
from pathlib import Path
import hashlib,json,re,sys
root=Path(__file__).resolve().parent.parent
def records(path):
    result=set()
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#'):continue
        p=line.split('\t');p[1]=re.sub("[- ']",'',p[1]);result.add(tuple(p))
    return result
before=root/sys.argv[1];after=root/'app/src/main/assets/addons.tsv'
a,b=records(before),records(after)
assert a==b,{'removed':list(a-b)[:5],'added':list(b-a)[:5]}
result={'normalized_records':len(a),'equal':True,'before_sha256':hashlib.sha256(before.read_bytes()).hexdigest(),'after_sha256':hashlib.sha256(after.read_bytes()).hexdigest(),
    'unit_source':'Pinned WanaKana conversion tokens; every token concatenation asserted equal to public toRomaji output. POJ/Chinese retain existing source separators.'}
(root/'docs/speculation/reading-unit-verification.json').write_bytes((json.dumps(result,indent=2)+'\n').encode())
print(json.dumps(result))
