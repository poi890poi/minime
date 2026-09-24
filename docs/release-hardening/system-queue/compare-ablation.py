"""Aggregate-only ablation comparison; raw evidence remains local."""
import csv
import hashlib
import json
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(root / 'tools'))
from report_touch_latency import report, validate_workload

def rows(path, delimiter=','):
    with path.open(encoding='utf-8-sig') as stream:
        return list(csv.DictReader(stream, delimiter=delimiter))

result = dict(scope='One instrumented Chinese shard pair; attribution, not release speedup.', runs={})
for label, folder, integrity_name, session in [
    ('baseline', 'system-queue', 'scoped.json', 'f250d978-d29e-40cd-8e68-e9679e4a3bd9'),
    ('suppressed_popup', 'annotation-ablation', 'complete.json', '5c48255b-9ac5-4aeb-9023-0752705f6254')]:
    directory = root / 'artifacts' / folder
    integrity = json.loads((directory / integrity_name).read_text(encoding='utf-8'))
    assert integrity['status'] == 'complete' and not integrity['health']
    queues = [r for r in rows(directory / 'scoped.csv') if r['kind'] == 'queue']
    start = min(int(r['ts']) for r in queues)
    end = max(int(r['ts']) + int(r['dur']) for r in queues)
    relayouts = [r for r in rows(directory / 'frame-detail.csv') if r['kind'] == 'slice'
                and r['name'].startswith('relayoutWindow') and int(r['ts']) < end
                and int(r['ts']) + int(r['dur']) > start]
    work_name = 'frame-work-checked.json' if label == 'baseline' else 'work.json'
    work = json.loads((directory / work_name).read_text(encoding='utf-8'))
    touch_path = root / 'artifacts/device-tests' / session / 'touch-latency.tsv'
    touches = rows(touch_path, '\t')
    corpus = rows(root / 'docs/release-hardening/language-timing/inputs.tsv', '\t')
    validation = validate_workload(touches, corpus, 'chinese', 0)
    touch = report(touches, label)
    result['runs'][label] = dict(session=session, workload=validation,
        queues=integrity['observed_queues'], queue_wait=integrity['stages']['queue'],
        relayout_count=len(relayouts),
        relayout_wall_ms=sum(min(end, int(r['ts']) + int(r['dur'])) - max(start, int(r['ts'])) for r in relayouts) / 1e6,
        relayout_wait_mean_ms=sum(r['mean_ms_per_request'] for r in work['leaves'] if r['name'].startswith('relayoutWindow')),
        touch_groups=touch['groups'], touch_limitations=touch['limitations'],
        files={str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
               for p in [directory / 'phone/trace.pftrace' if label != 'baseline' else directory / 'phone-scoped-fixed/trace.pftrace',
                         directory / 'frame-detail.csv', touch_path]})
out = root / 'artifacts/annotation-ablation/comparison.json'
out.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
for key, run in result['runs'].items():
    print(key, 'relayouts', run['relayout_count'], 'relayout wall ms', run['relayout_wall_ms'],
          'relayout wait ms/request', run['relayout_wait_mean_ms'], 'queue', run['queue_wait'])
