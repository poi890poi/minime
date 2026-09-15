"""Summarize a phone audit without confusing repeated views with distinct glyphs."""
import argparse
import hashlib
import json
import math
from pathlib import Path


def summarize(path):
    data=json.loads(path.read_text(encoding='utf-8'))
    failures=data['displayFailures']
    groups={}
    for provider in sorted({q['provider'] for q in data['queries']}):
        rows=[row for row in failures if row['provider']==provider]
        groups[provider]={
            'queries':sum(q['provider']==provider for q in data['queries']),
            'failing_view_occurrences':len(rows),
            'unique_affected_inputs':len({r['input'] for r in rows}),
            'unique_unreadable_texts':len({r['text'] for r in rows}),
            'missing_clusters':sorted({g for r in rows for g in r['missingGlyphs']}),
            'malformed_view_occurrences':sum(bool(r['malformed']) for r in rows),
        }
        costs=sorted(q['glyphCheckMicros'] for q in data['queries'] if q['provider']==provider and 'glyphCheckMicros' in q)
        if costs:
            groups[provider]['glyph_check_microseconds_per_probe']={
                'n':len(costs),'p50':costs[math.ceil(len(costs)*.5)-1],
                'p95':costs[math.ceil(len(costs)*.95)-1],'max':costs[-1],
                'conditions':'Fresh capability cache per probe; summed checks across all typed prefixes. Nearest-rank percentiles. Excludes lookup, rendering and touch latency.',
            }
    return {
        'report_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
        'device':data['device'],'sdk':data['sdk'],
        'source_rows':data['sourceRows'],'source_code_points':data['sourceCodePoints'],
        'source_font_census_gaps':len(data['sourceFontGaps']),
        'candidate_view_occurrences_checked':data['renderedViews'],
        'definition':'View occurrences include repeated candidates and offscreen strip/grid entries; they are not unique glyph counts or typing accuracy.',
        'providers':groups,'display_gate_passed':not failures,
        'screenshot_input':{q['provider']:q for q in data['queries'] if q['input']=='rime'},
        'screenshot_failures':[r for r in failures if r['input']=='rime'],
    }


if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('report',type=Path)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    result=summarize(args.report)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k not in ('providers','screenshot_input','screenshot_failures')},indent=2))
