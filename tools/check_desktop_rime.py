"""Cross-check desktop C API endpoint recovery against saved Android telemetry."""
import argparse, json, subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser();parser.add_argument('--fixture',default='docs/conversation-ranking/taiwan-only.json');parser.add_argument('--natural',action='store_true');parser.add_argument('--output',default='docs/conversation-ranking/desktop-crosscheck.json');options=parser.parse_args()
args=[str(ROOT/'artifacts/desktop-rime.exe'),str(ROOT/'.tools/rime-evaluation/msvc/dist/lib/rime.dll'),str(ROOT/'app/src/main/rimeAssets/rime'),str(ROOT/'artifacts/desktop-rime-user')]
if options.natural:args.append('--natural-order')
process=subprocess.Popen(args,stdin=subprocess.PIPE,stdout=subprocess.PIPE,universal_newlines=True,encoding='utf-8')
assert process.stdout.readline().strip()=='READY 1.16.1'
samples=json.loads((ROOT/options.fixture).read_text(encoding='utf-8'))
checked={};mismatches=[]
for row in samples:
    if row['mode']!='pinyin':continue
    for token in row['tokens']:
        raw=token['raw']
        if raw in checked:continue
        expected=[(c['consumed'],c['text'],c['score']) for c in token['candidates'] if c['score']>=64 and any(ord(ch)>127 for ch in c['text'])]
        process.stdin.write(raw+'\n');process.stdin.flush();actual=[]
        while True:
            line=process.stdout.readline()
            if not line:raise RuntimeError('Desktop Rime exited before END')
            line=line.strip()
            if not line:continue
            if line=='END':break
            end,text=line.split('\t',1);end=int(end)
            actual.append((0 if end==len(raw) else end,text,100-len(actual)))
        checked[raw]=len(expected)
        if expected!=actual[:len(expected)]:mismatches.append(dict(raw=raw,phone=expected,desktop=actual))
process.stdin.close();assert process.wait(timeout=10)==0
report=dict(policy='natural' if options.natural else 'legacy-preview',queries=len(checked),candidate_comparisons=sum(checked.values()),mismatches=mismatches,scope='Recorded phone first 24 displayed candidates, native score/text/consumption; not the Android UI or runtime timing')
(ROOT/options.output).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False));assert not mismatches
