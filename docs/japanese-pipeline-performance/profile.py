import json,collections
from pathlib import Path
j=json.loads(Path('artifacts/japanese-pipeline/profile.json').read_text(encoding='utf-8-sig'))
c=collections.Counter();top=collections.Counter()
for event in j['recording']['events']:
    frames=(event['values'].get('stackTrace') or {}).get('frames',[])
    names=[f['method']['type']['name']+'.'+f['method']['name'] for f in frames]
    top.update(names[:1])
    for label,part in [('regex','java/util/regex'),('classify','IntentClassifier.classify'),('kana','JapaneseKana.lookup'),('refresh','CompositionEngine.refresh')]:
        if any(part in name for name in names):c[label]+=1
out={'samples':len(j['recording']['events']),'stacks_containing':dict(c),'top_frames':top.most_common(20),'limitation':'Desktop samples, including startup; Android effect needs old/new APK measurement.'}
Path('docs/japanese-pipeline-performance/baseline-profile.json').write_text(json.dumps(out,indent=2)+'\n')
print(json.dumps(out,indent=2))
