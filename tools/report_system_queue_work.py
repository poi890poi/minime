"""Exclusive main-thread work attribution for an integrity-checked system trace."""
import argparse
import collections
import csv
import hashlib
import json
import re
from pathlib import Path
from system_queue_intervals import exclusive_work


def report(integrity, queues, rows):
    if integrity['status'] != 'complete' or integrity['health']:
        raise ValueError('Complete, healthy trace required')
    queues = [r for r in queues if r['kind'] == 'queue']
    if len(queues) != integrity['expected_queues']:
        raise ValueError('Queue inventory differs from integrity report')
    spans = [(int(r['ts']), int(r['ts']) + int(r['dur'])) for r in queues]
    if not spans:
        raise ValueError('Empty queue intervals')
    horizon = max(end for start, end in spans)
    slices, states = [], []
    frames = collections.Counter()
    for row in rows:
        start, duration = int(row['ts']), int(row['dur'])
        if duration < 0:
            if start >= horizon:
                continue  # Final open state after every measured request.
            raise ValueError('Incomplete main-thread span')
        if duration == 0:
            continue
        name = re.sub(r'\b\d+\b', '#', row['name'])
        if row['kind'] == 'slice':
            slices.append((int(row['id']), int(row['extra']), start, start + duration, name))
            if name.startswith('Choreographer#doFrame'):
                frames[name] += 1
        elif row['kind'] == 'state':
            states.append((start, start + duration, row['name']))
        else:
            raise ValueError('Unexpected detail row kind')
    totals, by_state = exclusive_work(spans, slices, states)
    total = sum(end - start for start, end in spans)
    if not total:
        raise ValueError('Empty queue intervals')
    return dict(scope='Deepest active main-thread trace slice, exclusively accounted during queued-request waiting; untraced gaps retained. Instrumented Chinese fixture only.',
        requests=len(queues), total_wait_ms=total / 1e6,
        leaves=[dict(name=name, share=duration / total, mean_ms_per_request=duration / len(queues) / 1e6,
                     state_mean_ms={state: value / len(queues) / 1e6 for state, value in by_state[name].items()})
                for name, duration in totals.most_common()], frame_names=dict(frames))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('integrity', 'queues', 'detail', 'output'):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    def rows(path):
        with path.open(encoding='utf-8-sig') as stream:
            return list(csv.DictReader(stream))
    result = report(json.loads(args.integrity.read_text(encoding='utf-8')), rows(args.queues), rows(args.detail))
    result['inputs'] = {str(path): hashlib.sha256(path.read_bytes()).hexdigest()
                        for path in (args.integrity, args.queues, args.detail)}
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f"{result['requests']} requests; {result['total_wait_ms']:.3f} ms summed wait")


if __name__ == '__main__':
    main()
