"""Join synthetic touch and callback episodes by time; publish aggregate stages only."""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path
from report_candidate_stages import summary
from report_touch_latency import validate_workload


def correlate(touches, requests):
    groups = {}
    for i, touch in enumerate(touches):
        if touch['action'] != 'key':
            continue
        key = touch['mode'] + '/' + touch['interval_ms']
        group = groups.setdefault(key, dict(counts=collections.Counter(), stages=collections.defaultdict(list)))
        counts, stages = group['counts'], group['stages']
        counts['letters'] += 1
        up = int(touch['up_ns'])
        end = int(touches[i+1]['up_ns']) if i+1 < len(touches) else float('inf')
        matching = [r for r in requests if r['mode'] == touch['mode'] and r['query'] == touch['expected']
                    and up <= int(r['requested_ns']) < end]
        if len(matching) != 1:
            counts['unmatched_request' if not matching else 'ambiguous_request'] += 1
            continue
        row = matching[0]
        requested, delivered, finished = (int(row[k]) for k in ('requested_ns','delivered_ns','finished_ns'))
        counts['matched_request'] += 1
        if not delivered or not finished:
            counts['undelivered'] += 1
            continue
        if not up <= requested <= delivered <= finished:
            raise ValueError('Nonmonotonic request/callback timestamps')
        counts['delivered'] += 1
        for name, value in [('dispatch',requested-up),('delivery',delivered-requested),('application',finished-delivered)]:
            stages[name].append(value / 1e6)
        for name in ('glyph','render','votes'):
            stages[name].append(int(row[name+'_ns']) / 1e6)
        predraw, submitted = int(touch['candidate_pre_draw_ns']), int(touch['candidate_submit_ns'])
        if not predraw or not submitted:
            counts['no_candidate_frame'] += 1
            continue
        if not finished <= predraw <= submitted:
            # Do not assign a frame belonging to a different/earlier callback.
            counts['frame_before_callback_finished'] += 1
            continue
        counts['complete_timeline'] += 1
        stages['after_application_to_predraw'].append((predraw-finished)/1e6)
        stages['predraw_to_submission'].append((submitted-predraw)/1e6)
        stages['total'].append((submitted-up)/1e6)
    return {key: dict(counts=dict(value['counts']), stages={k:summary(v) for k,v in value['stages'].items()})
            for key,value in groups.items()}


def read(path):
    with path.open(encoding='utf-8') as stream:
        return list(csv.DictReader(stream,delimiter='\t'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('session',type=Path)
    parser.add_argument('output',type=Path)
    parser.add_argument('--mode',required=True)
    parser.add_argument('--shard',type=int,default=0)
    parser.add_argument('--corpus',type=Path,default=Path('docs/release-hardening/language-timing/inputs.tsv'))
    args = parser.parse_args()
    touch=args.session/'touch-latency.tsv';stages=args.session/'candidate-stages.tsv'
    touches,requests=read(touch),read(stages)
    result=dict(scope='Hooked diagnostics, not release latency or physical screen presentation. Delivery includes scheduled delay, worker queue/work and main queue. Stage percentiles cannot be added.',
                workload=validate_workload(touches,read(args.corpus),args.mode,args.shard),
                hashes={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (touch,stages,args.corpus)},
                groups=correlate(touches,requests))
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result['groups'],indent=2))


if __name__ == '__main__':
    main()
