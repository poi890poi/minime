"""Follow every prior >100 ms observation through the complete diagnostic replay."""
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
HERE = ROOT / 'artifacts/post-window-stages'


def rows(path):
    with path.open(encoding='utf-8-sig') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))


def main():
    prior_path = ROOT / 'docs/release-hardening/single-window-language/comparison.json'
    prior = json.loads(prior_path.read_text(encoding='utf-8'))
    result = dict(prior_comparison_sha256=hashlib.sha256(prior_path.read_bytes()).hexdigest(), observations=[])
    for item in prior['outliers']:
        language = item['tag'].split('-')[0]
        summary = json.loads((HERE / (language + '.json')).read_text(encoding='utf-8'))
        session = ROOT / 'artifacts/device-tests' / summary['session']
        touch_path = session / 'touch-latency.tsv'
        stage_path = session / 'candidate-stages.tsv'
        queue_path = session / 'candidate-queues.tsv'
        touch = rows(touch_path)
        index = item['action_index']
        row = touch[index]
        for key in ['query_id', 'source', 'genre', 'condition', 'action']:
            if row[key] != item[key]:
                raise ValueError('Different diagnostic action identity')
        if int(row['interval_ms']) != item['interval_ms'] or len(row['expected']) != item['typed_length']:
            raise ValueError('Different diagnostic input condition')
        up = int(row['up_ns'])
        end = int(touch[index + 1]['up_ns']) if index + 1 < len(touch) else float('inf')
        stage = [s for s in rows(stage_path) if s['mode'] == row['mode'] and s['query'] == row['expected']
                 and up <= int(s['requested_ns']) < end]
        queue = [q for q in rows(queue_path) if up <= int(q['requested_ns']) < end]
        endpoint = 'candidate_submit_ns' if item['metric'] == 'candidate' else 'editor_submit_ns'
        entry = dict(prior=item, diagnostic_session=summary['session'], matching_callbacks=len(stage),
            matching_queue_requests=len(queue), diagnostic_delay_ms=
            (int(row[endpoint]) - up) / 1e6 if int(row[endpoint]) else None)
        if len(stage) == len(queue) == 1:
            s, q = stage[0], queue[0]
            entry['glyph_ms'] = int(s['glyph_ns']) / 1e6
            entry['candidates'] = int(s['candidates'])
            for key, owner, first, last in [('callback_ms', s, 'delivered_ns', 'finished_ns'),
                    ('provider_ms', q, 'worker_ns', 'providers_ns'), ('main_queue_ms', q, 'posted_ns', 'entered_ns')]:
                entry[key] = (int(owner[last]) - int(owner[first])) / 1e6 if int(owner[first]) and int(owner[last]) else None
        entry['hashes'] = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
                           for p in [touch_path, stage_path, queue_path]}
        result['observations'].append(entry)
    result['scope'] = 'All prior outliers, matched by action identity. Hooked follow-up timing is not an unhooked speedup.'
    (HERE / 'outlier-followup.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print('Accounted for', len(result['observations']), 'of', len(prior['outliers']), 'prior outliers')


if __name__ == '__main__':
    main()
