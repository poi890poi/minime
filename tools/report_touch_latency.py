"""Summarize raw OS-injected timing telemetry; missing samples stay missing."""
import argparse, csv, gzip, json, math, statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--input',type=Path,default=ROOT/'artifacts/touch-latency-corrected.tsv')
parser.add_argument('--output',type=Path,default=ROOT/'docs/touch-latency')
parser.add_argument('--build',default='5bcb90f / 0.7.6')
args=parser.parse_args()
OUT=args.output
rows = list(csv.DictReader(args.input.open(encoding='utf-8'), delimiter='\t'))


def summary(group, endpoint, origin='up_ns'):
    values = sorted((int(r[endpoint])-int(r[origin]))/1e6 for r in group if int(r[endpoint]))
    result = dict(n=len(group), observed=len(values), unobserved=len(group)-len(values))
    if values:
        assert min(values)>=0
        result.update(mean_ms=statistics.mean(values),p50_ms=statistics.median(values),
                      p95_ms=values[math.ceil(.95*len(values))-1],p99_ms=values[math.ceil(.99*len(values))-1],max_ms=max(values))
    return result


groups = {}
for mode in dict.fromkeys(r['mode'] for r in rows):
    for interval in ('150','60'):
        group=[r for r in rows if r['mode']==mode and r['interval_ms']==interval]
        keys=[r for r in group if r['action']=='key'];spaces=[r for r in group if r['action']=='space']
        groups[mode+'/'+interval] = dict(editor_callback=summary(keys,'editor_callback_ns'),
            editor_submission=summary(keys,'editor_submit_ns'),candidate_submission=summary(keys,'candidate_submit_ns'),
            pressed_submission=summary(keys,'pressed_submit_ns','down_ns'),space_submission=summary(spaces,'editor_submit_ns'))
OUT.mkdir(parents=True,exist_ok=True)
sources=[(args.input,'samples.tsv.gz')]
if args.input==ROOT/'artifacts/touch-latency-corrected.tsv':
    sources.append((ROOT/'artifacts/touch-latency-rejected-ondraw.tsv','rejected-ondraw.tsv.gz'))
for source,target in sources:
    with (OUT/target).open('wb') as f:
        with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as compressed:compressed.write(source.read_bytes())
(OUT/'summary.json').write_text(json.dumps(dict(baseline=args.build,samples=len(rows),groups=groups,
    limitations=['injected events, not physical digitizer latency','frame callback delivery, not display presentation',
                 'one session, small sample; no acceptance certification','same-process test editor, not Chrome',
                 'candidate metrics conditional on observing a fresh matching frame before the next action; unchanged rows and supersession are not distinguished']),indent=2)+'\n',encoding='utf-8')
for key,value in groups.items():
    print(key, 'raw p95',round(value['editor_submission'].get('p95_ms',float('nan')),2), 'candidates',value['candidate_submission'])
