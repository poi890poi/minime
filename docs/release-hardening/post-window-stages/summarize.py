"""Validate a combined diagnostic and write text-free aggregate evidence."""
import csv
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
HERE = ROOT / 'artifacts/post-window-stages'
sys.path.insert(0, str(ROOT / 'tools'))
from report_touch_latency import report, validate_workload
from report_decoder_queues import analyze, correlate as queue_correlation
from report_language_stages import correlate as stage_correlation


def read(path):
    with path.open(encoding='utf-8-sig') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))


def main():
    tag = sys.argv[1]
    mode = {'chinese': 'chinese', 'taiwanese': 'taiwanese_english', 'japanese': 'japanese_english'}[tag]
    log = HERE / (tag + '.log')
    text = log.read_text(encoding='utf-8-sig')
    identities = re.findall(r'Session evidence: .*?device-tests[\\/]([0-9a-f-]{36})', text)
    if len(identities) != 1 or any(receipt not in text for receipt in
            ['OK (1 test)', 'Cleanup verified: preferences, previous IME, display OFF.',
             'Final display OFF verified after environment collection']):
        raise ValueError('Incomplete session or restoration receipt')
    session = ROOT / 'artifacts/device-tests' / identities[0]
    touch, queues, stages = (session / name for name in
                            ['touch-latency.tsv', 'candidate-queues.tsv', 'candidate-stages.tsv'])
    touches, requests, callbacks = read(touch), read(queues), read(stages)
    corpus = ROOT / 'docs/release-hardening/language-timing/inputs.tsv'
    result = dict(session=identities[0], workload=validate_workload(touches, read(corpus), mode, 0),
        touch=report(touches, 'post-window-stages-' + tag), queues=analyze(requests),
        queue_correlation=queue_correlation(touches, requests), stages=stage_correlation(touches, callbacks))
    result['stage_strata'] = {}
    for key in dict.fromkeys((r['source'], r['genre'], r['condition']) for r in touches):
        # Keep whole episodes, including terminal Space. No key-release window
        # then spans removed episodes when the strict correlator uses adjacency.
        selected = [r for r in touches if (r['source'], r['genre'], r['condition']) == key]
        result['stage_strata']['/'.join(key)] = stage_correlation(selected, callbacks)
    missing = sum(g[k]['unobserved'] for g in result['touch']['groups'].values()
                  for k in ['editor_submission', 'space_submission'])
    result['raw_space_missing'] = missing
    result['over_100_ms'] = []
    result['missing_candidate_frames'] = []
    for index, row in enumerate(touches):
        up = int(row['up_ns'])
        end = int(touches[index + 1]['up_ns']) if index + 1 < len(touches) else float('inf')
        matching = [c for c in callbacks if c['mode'] == row['mode'] and c['query'] == row['expected']
                    and up <= int(c['requested_ns']) < end] if row['action'] == 'key' else []
        detail = dict(action_index=index, query_id=row['query_id'], source=row['source'], genre=row['genre'],
            condition=row['condition'], interval_ms=int(row['interval_ms']), action=row['action'],
            typed_length=len(row['expected']), matching_callbacks=len(matching))
        queue_matches = [q for q in requests if up <= int(q['requested_ns']) < end] if row['action'] == 'key' else []
        detail['matching_queue_requests'] = len(queue_matches)
        if len(queue_matches) == 1:
            q = queue_matches[0]
            detail['queue'] = dict(id=int(q['id']), terminal=int(q['terminal']))
            for name, first, last in [('schedule_wait_ms', 'scheduled_ns', 'worker_ns'),
                    ('provider_ms', 'worker_ns', 'providers_ns'), ('main_queue_ms', 'posted_ns', 'entered_ns'),
                    ('callback_ms', 'entered_ns', 'finished_ns')]:
                detail['queue'][name] = (int(q[last]) - int(q[first])) / 1e6 if int(q[first]) and int(q[last]) else None
        if len(matching) == 1:
            c = matching[0]
            detail['callback'] = {k: int(c[k]) for k in ['glyph_ns', 'render_ns', 'votes_ns', 'candidates']}
            detail['callback']['finished_after_next_up'] = int(c['finished_ns']) >= end
            detail['callback']['presentation_changed'] = c['presentation_changed'] == 'true'
            detail['callback']['delivered'] = bool(int(c['delivered_ns']))
        if row['action'] == 'key' and not int(row['candidate_submit_ns']):
            result['missing_candidate_frames'].append(detail)
        for metric, endpoint in [('raw_or_space', 'editor_submit_ns'), ('candidate', 'candidate_submit_ns')]:
            if int(row[endpoint]) and int(row[endpoint]) - up > 100_000_000:
                result['over_100_ms'].append(dict(detail, metric=metric, delay_ms=(int(row[endpoint]) - up) / 1e6))
    env = ROOT / 'artifacts/release-hardening' / ('post-window-stages-' + tag + '-environment')
    display = env / 'final-display.txt'
    if not re.search(r'Display Id=0\s*\n\s*Display State=OFF\s*\n', display.read_text(encoding='utf-8-sig')):
        raise ValueError('No verified final display OFF')
    cold = json.loads((env / 'cooldown.jsonl').read_text(encoding='utf-8-sig').splitlines()[-1])
    if cold['thermal'] != 0 or cold['batteryC'] >= 34:
        raise ValueError('Unverified cooled start')
    result['cooled_start'] = cold
    result['restored_and_display_off'] = True
    result['hashes'] = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
                        for p in [log, touch, queues, stages, corpus, display, session / 'candidate-work.tsv']}
    result['reporter_hashes'] = {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
        for p in [Path(__file__), *(ROOT / 'tools' / name for name in
          ['report_touch_latency.py', 'report_decoder_queues.py', 'report_language_stages.py', 'report_candidate_stages.py'])]}
    result['limitations'] = ['Combined hooks perturb timing; not release latency or a speedup.',
                            'Stage percentiles cannot be added; ambiguous matches stay excluded.',
                            'Injected events and callback submission are not human touch or displayed pixels.']
    out = HERE / (tag + '.json')
    out.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(tag, 'actions', len(touches), 'missing raw/Space', missing,
          'queue states', result['queues']['counts'], 'over 100 ms', len(result['over_100_ms']))
    if missing:
        raise SystemExit('Missing raw/Space frames block further expansion')


if __name__ == '__main__':
    main()
