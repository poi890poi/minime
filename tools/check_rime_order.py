"""Confirm candidate-order policy changes preserve all bounded native choices."""
import json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
def start(legacy):
    user=ROOT/'artifacts'/('order-legacy' if legacy else 'order-natural');user.mkdir(exist_ok=True)
    args=[str(ROOT/'artifacts/desktop-rime.exe'),str(ROOT/'.tools/rime-evaluation/msvc/dist/lib/rime.dll'),str(ROOT/'app/src/main/rimeAssets/rime'),str(user)]
    if not legacy:args.append('--natural-order')
    p=subprocess.Popen(args,stdin=subprocess.PIPE,stdout=subprocess.PIPE,universal_newlines=True,encoding='utf-8');assert p.stdout.readline().strip()=='READY 1.16.1';return p
def query(p,raw):
    p.stdin.write(raw+'\n');p.stdin.flush();rows=[]
    while True:
        line=p.stdout.readline()
        if not line:raise RuntimeError('Native process ended early')
        line=line.strip()
        if not line:continue
        if line=='END':return rows
        end,text=line.split('\t',1);rows.append((int(end),text))
inputs=set()
for line in (ROOT/'docs/conversation-ranking/corpus/inputs.tsv').read_text(encoding='utf-8').splitlines():
    group,identity,raw,expected=line.split('\t');inputs.update(raw.split(' ') if group.startswith('en-') else [raw])
a=start(True);b=start(False);changed=0;examples=[];mismatch=[]
try:
    for i,raw in enumerate(sorted(inputs)):
        old=query(a,raw);new=query(b,raw)
        complete=lambda rows:next((text for end,text in rows if end==len(raw)),None)
        if sorted(old)!=sorted(new) or complete(old)!=complete(new):mismatch.append(raw)
        if old!=new:
            changed+=1
            if len(examples)<20:examples.append(dict(raw=raw,before=old[:8],after=new[:8]))
        if (i+1)%2000==0:print('Compared',i+1,flush=True)
finally:
    for p in [a,b]:p.stdin.close();assert p.wait(timeout=10)==0
report=dict(inputs=len(inputs),reordered=changed,candidate_set_or_first_complete_mismatches=mismatch,examples=examples,scope='Mechanical ordering/coverage gate; not independent language-model accuracy')
(ROOT/'docs/conversation-ranking/native-order.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print(json.dumps({k:v for k,v in report.items() if k!='examples'}));assert not mismatch
