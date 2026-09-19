"""Report visible observations without mistaking runner completion for parity."""
import gzip
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
art = root / 'artifacts/suggestion-coverage'
out = root / 'docs/suggestion-coverage'
cases = {}
for i in (1, 2):
    for case in json.loads((out / f'phone-plans/batch-{i:02}.json').read_text(encoding='utf-8')):
        cases[case['id']] = case
rows = []
for phase in ('baseline', 'after'):
    for i in (1, 2):
        file = art / f'phone-{phase}-{i:02}' / 'observations.json'
        (out / 'evidence' / f'phone-{phase}-{i:02}.json.gz').write_bytes(gzip.compress(file.read_bytes(), mtime=0))
        for record in json.loads(file.read_text(encoding='utf-8')):
            case = cases[record['id']]
            typed = next((s for s in record['steps'] if s['stage']=='typed'), None)
            values = []
            if typed:
                for label in typed['visible']:
                    if record['provider']=='minime':
                        if label.startswith('Candidate ') and label!='Candidate list': values.append(label[10:])
                    else:
                        if label=='其他候選鍵': break
                        if label!='…': values.append(label)
            selected = next((s for s in record['steps'] if s['stage']=='stored-prefix'), None)
            expected = case.get('expected_prefix') if phase=='after' else None
            checked = None
            if record['provider']=='minime' and expected and selected:
                checked = (selected['text']==expected['text']+expected['remaining']
                           and selected['composingStart']==len(expected['text'])
                           and selected['composingEnd']==len(expected['text']+expected['remaining']))
                assert checked, (record['id'], selected)
            rows.append(dict(phase=phase, provider=record['provider'], id=record['id'],
                             raw=case['source']['raw'], target=case['source']['target'],
                             status=record['status'], visible=values,
                             phrase_selection_suffix_verified=checked,
                             error=record.get('error')))
report = dict(scope='Repeated reference observations; Google learning state was not reset. Not language accuracy. After is the initial prefix APK before alias/first-page refinements; final APK has separate asserted integration tests.',
              cases=rows)
(out / 'phone-results.json').write_bytes((json.dumps(report, ensure_ascii=False, indent=2)+'\n').encode())
for provider in ('google', 'minime'):
    for phase in ('baseline', 'after'):
        values = [r for r in rows if r['phase']==phase and r['provider']==provider]
        print(provider, phase, 'typed', sum(bool(r['visible']) for r in values), '/', len(values),
              'all requested actions observed', sum(r['status']=='observed' for r in values),
              'verified phrase selections', sum(r['phrase_selection_suffix_verified'] is True for r in values))
