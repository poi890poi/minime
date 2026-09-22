"""Score verified Chinese input separately from missing actions and English observations.

Reference-prefix compatibility is task utility, not a judgment that alternative
homophones are meaningless. English rows are retained without using Chinese UI
boundaries or treating a literal recovery choice as a dictionary hit.
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path
from report_usefulness_phone import extract, score


def report(plans, records):
    by_id = {case['id']: case for case in plans}
    seen = set()
    rows = []
    totals = collections.defaultdict(collections.Counter)
    ui_totals = collections.defaultdict(collections.Counter)
    for record in records:
        identity = (record['provider'], record['id'])
        if identity in seen:
            raise ValueError(f'Duplicate observation: {identity}')
        seen.add(identity)
        case = by_id[record['id']]
        source = case['source']
        steps = {step['stage']: step for step in record['steps']}
        typed = steps.get('typed', {})
        input_verified = typed.get('text', '').lower() == source['raw'].lower()
        row = dict(provider=identity[0], id=identity[1], genre=source['genre'],
                   condition=source['condition'], raw=source['raw'], target=source['target'],
                   status=record['status'], input_verified=input_verified, stages={})
        if 'error' in record:
            row['error'] = record['error']
        if case['mode'] == 'english':
            row['ranking_scope'] = 'Not scored: Chinese accessibility boundaries do not establish English candidate provenance.'
        elif record['status'] == 'observed':
            for stage in ('typed', 'expanded'):
                if stage not in steps:
                    row['stages'][stage] = dict(unavailable='Missing snapshot')
                    continue
                try:
                    texts = extract(identity[0], stage, steps[stage]['visible'])
                except ValueError as error:
                    row['stages'][stage] = dict(unavailable=str(error))
                    continue
                texts = [text for text in texts if text != source['raw']]
                metrics = score(texts, source['target'])
                row['stages'][stage] = dict(candidates=texts, metrics=metrics)
                key = '/'.join((identity[0], source['genre'], stage))
                counts = dict(episodes=1, slots=metrics['slots'], compatible_slots=metrics['useful_slots'],
                              whole_available=int(metrics['whole_available']),
                              any_compatible=int(metrics['useful_slots'] > 0))
                ui_totals[key].update(counts)
                if input_verified:
                    totals[key].update(counts)
        accepted = steps.get('accepted')
        if accepted:
            row['accepted'] = accepted['text']
            row['ui_whole_target_committed'] = record['status'] == 'observed' and accepted['text'].rstrip(' ') == source['target'] and accepted['composingStart'] == -1
            row['whole_target_committed'] = input_verified and accepted['text'].rstrip(' ') == source['target'] and accepted['composingStart'] == -1
        rows.append(row)
    missing = [dict(provider=provider, id=case['id']) for provider in ('google', 'minime')
               for case in plans if (provider, case['id']) not in seen]
    return dict(planned_records=2 * len(plans), records=len(rows), missing=missing,
                unverified_input=[dict(provider=r['provider'], id=r['id']) for r in rows if not r['input_verified']],
                totals=dict(totals), end_to_end_ui_totals=dict(ui_totals), rows=rows,
                limitations=['Small already-inspected sample, not a fresh quality holdout',
                             'Google learning persists; MinIME preferences/learning reset per case',
                             'Visible task-compatible text does not prove useful acceptance spans',
                             'Alternative homophones are not labeled meaningless',
                             'English ranking is unscored; acceptance observations are retained',
                             'Literal input verification requires editor spelling; Google Chinese keeps preedit in its IME and leaves the editor empty',
                             'End-to-end UI totals score observed requested touch sequences and include touch/delivery effects; they are not decoder-only accuracy'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plans', type=Path)
    parser.add_argument('observations', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    plans = [case for file in sorted(args.plans.glob('batch-*.json'))
             for case in json.loads(file.read_text(encoding='utf-8'))]
    data = report(plans, json.loads(args.observations.read_text(encoding='utf-8')))
    data['observations_sha256'] = hashlib.sha256(args.observations.read_bytes()).hexdigest()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: data[key] for key in ('planned_records', 'records', 'missing', 'unverified_input', 'totals', 'end_to_end_ui_totals')}, ensure_ascii=False))


if __name__ == '__main__':
    main()
