"""Score intended input against observations, keeping ambiguous downs outside the gate."""
import argparse
import gzip
from collections import Counter
import json
import math
from pathlib import Path

def edits(expected, actual):
    # Counts of deletion, insertion, substitution in a minimum-edit alignment.
    row = [(j, (0, j, 0)) for j in range(len(actual) + 1)]
    for i, a in enumerate(expected, 1):
        new = [(i, (i, 0, 0))]
        for j, b in enumerate(actual, 1):
            if a == b:
                new.append(row[j - 1])
            else:
                choices = []
                for prior, kind in ((row[j], 0), (new[-1], 1), (row[j - 1], 2)):
                    counts = list(prior[1]); counts[kind] += 1
                    choices.append((prior[0] + 1, tuple(counts)))
                new.append(min(choices))
        row = new
    return row[-1][1]

def summarize(root):
    result = {'matrix': {}, 'exploratory': {}, 'replay': {}, 'complete': True, 'gate_failures': 0}
    metadata = root / 'run-metadata.json'
    if metadata.exists():
        phase = json.loads(metadata.read_text(encoding='utf-8-sig'))['phase']
        expected = {'Matrix': ('portrait', 'landscape'), 'Development': ('replay-dev',), 'Holdout': ('replay-test',),
                    'All': ('portrait', 'landscape', 'replay-dev', 'replay-test')}[phase]
        missing = [name for name in expected if not any((root / ('human-input-' + name + suffix)).exists() for suffix in ('.json', '.json.gz'))]
        assert not missing, 'Missing phase evidence: ' + ', '.join(missing)
    durations = []
    names = {p.name[:-3] if p.suffix == '.gz' else p.name for pattern in ('human-input-*.json', 'human-input-*.json.gz') for p in root.glob(pattern)}
    for name in sorted(names):
        path = root / name
        if path.exists():
            report = json.loads(path.read_text(encoding='utf-8'))
        else:
            with gzip.open(str(path) + '.gz', 'rt', encoding='utf-8') as stream:
                report = json.load(stream)
        result['complete'] &= report['complete']
        for row in report['cases']:
            if report['virtualEventTime']:
                category = result['matrix' if row['gate'] else 'exploratory']
                profile = row['id'].split(':')[0]
                stats = category.setdefault(profile, Counter())
                stats['cases'] += 1
                passed = row['actual'] == row['expected']
                assert passed == row['pass'], 'Recorded pass disagrees with independent comparison'
                stats['failed'] += not passed
                if row['gate']:
                    result['gate_failures'] += not passed
                if profile != 'intentional-up':
                    for label, count in zip(('deletions', 'insertions', 'substitutions'), edits(row['expected'], row['actual'])):
                        stats[label] += count
                    stats['unexpected_slides'] += sum(c.startswith('LITERAL:') for c in row['commands'])
                    stats['unexpected_traces'] += row['commands'].count('TRACE')
            else:
                split = row['id'].split(':')[0]
                stats = result['replay'].setdefault(split, Counter())
                stats['cases'] += 1
                stats['complete'] += row['complete']
                stats['touches'] += len(row['events'])
                stats['cancelled_touches'] += sum(e['cancel'] for e in row['events'])
                durations.extend(e['actualHoldMs'] for e in row['events'])
                for state in row['states']:
                    stats['checkpoints'] += 1
                    passed = state['expected'] == state['actual'] and (not state['expected'] or (state['composingStart'] == 0 and state['composingEnd'] == len(state['expected'])))
                    stats['failed_checkpoints'] += not passed
                    result['gate_failures'] += not passed
                result['complete'] &= row['complete']
    if durations:
        values = sorted(durations)
        result['observed_touch_hold_ms'] = {'count': len(values), 'p50': values[math.ceil(.5 * len(values)) - 1],
                                          'p95': values[math.ceil(.95 * len(values)) - 1], 'max': max(values)}
    assert result['matrix'] or result['replay'], 'No test records found'
    return result

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('directory', type=Path, nargs='?')
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        assert edits('letter', 'leter') == (1, 0, 0)
        assert edits('cat', 'cart') == (0, 1, 0)
        assert edits('cat', 'cut') == (0, 0, 1)
        assert edits('nihao', 'nihao') == (0, 0, 0)
        assert sum(edits('abcd', 'acbd')) == 2
        import tempfile
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            rows = [{'id': 'drift:a:0', 'expected': 'ab', 'actual': 'a', 'commands': ['a'], 'gate': True, 'pass': False},
                    {'id': 'outside-right:a', 'expected': 'a', 'actual': 'b', 'commands': ['b'], 'gate': False, 'pass': False}]
            (root / 'human-input-control.json').write_text(json.dumps({'complete': False, 'virtualEventTime': True, 'cases': rows}), encoding='utf-8')
            control = summarize(root)
            assert control['gate_failures'] == 1 and not control['complete']
            assert control['matrix']['drift']['deletions'] == 1 and control['exploratory']['outside-right']['substitutions'] == 1
        print('PASS metric detects edit controls, failing gates, incomplete runs and separate exploratory errors')
    else:
        assert args.directory is not None
        result = summarize(args.directory)
        text = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
        (args.directory / 'summary.json').write_text(text, encoding='utf-8')
        print(text)
        raise SystemExit(0 if result['complete'] and result['gate_failures'] == 0 else 1)
