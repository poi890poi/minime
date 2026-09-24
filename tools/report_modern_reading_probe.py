"""Score complete-reading support after decoding, without changing source labels."""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path
from audit_moe_readings import single_han

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'artifacts/moe-reading-reference'


def inventory(row):
    return [(word, int(end)) for item in row['outputs'].split('|') if item
            for word, end in [item.rsplit(':', 1)]]


def score(row, expected, known):
    result = collections.Counter(queries=1, reference_pairs=len(expected))
    values = inventory(row)
    covered = {word for word, end in values if end == 0}
    result['reference_pairs_available'] = len(covered & expected)
    result['space_supported_by_reference'] = row['space'] in expected
    result['first_nonraw_supported_by_reference'] = bool(values) and values[0][1] == 0 and values[0][0] in expected
    top = values[:8]
    result['first8_slots'] = len(top)
    supported = set()
    for word, end in top:
        if end != 0:
            result['first8_partial_slots'] += 1
        elif not single_han(word):
            result['first8_phrase_or_literal_slots'] += 1
        elif word in expected:
            result['first8_supported_complete_glyph_slots'] += 1
            supported.add(word)
        elif word in known:
            result['first8_reference_mismatch_glyph_slots'] += 1
        else:
            result['first8_uncovered_glyph_slots'] += 1
    return result, supported


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--candidate', type=Path, default=OUT / 'sets.tsv')
    parser.add_argument('--baseline', type=Path, default=OUT / 'before.tsv')
    parser.add_argument('--output', type=Path, default=OUT)
    args = parser.parse_args()
    manifest = json.loads((OUT / 'probe-manifest.json').read_text())
    reference_path = OUT / 'references.json'
    assert hashlib.sha256(reference_path.read_bytes()).hexdigest() == manifest['references_sha256']
    refs = {k: set(v) for k, v in json.loads(reference_path.read_text(encoding='utf-8')).items()}
    known = set().union(*refs.values())
    tables = {}
    for side, path in (('before', args.baseline), ('after', args.candidate)):
        with path.open(encoding='utf-8') as stream:
            tables[side] = {row['raw']: row for row in csv.DictReader(stream, delimiter='\t')}
        assert tables[side].keys() == refs.keys()
        assert all(not row['target'] for row in tables[side].values()), 'Labels reached evaluation input'
    groups = collections.defaultdict(lambda: {'before': collections.Counter(), 'after': collections.Counter(), 'changes': collections.Counter()})
    changes = []
    for raw, expected in refs.items():
        before, a = score(tables['before'][raw], expected, known)
        after, b = score(tables['after'][raw], expected, known)
        delta = collections.Counter(first8_supported_pairs_gained=len(b-a), first8_supported_pairs_lost=len(a-b),
            space_reference_gains=int(after['space_supported_by_reference'] and not before['space_supported_by_reference']),
            space_reference_losses=int(before['space_supported_by_reference'] and not after['space_supported_by_reference']))
        for group in ('all', 'one-letter' if len(raw) == 1 else 'multiple-letter'):
            groups[group]['before'].update(before)
            groups[group]['after'].update(after)
            groups[group]['changes'].update(delta)
        if before != after or a != b:
            changes.append(dict(raw=raw, before=dict(before), after=dict(after), gained=sorted(b-a), lost=sorted(a-b)))
    report = dict(scope='Complete-reading support against all supplied MOE concise single-Han Pinyin records; not conversation accuracy, frequency or a fresh holdout',
        groups={g: {side: dict(values) for side, values in sides.items()} for g, sides in groups.items()},
        manifest=manifest, hashes={name: hashlib.sha256(path.read_bytes()).hexdigest()
                                 for name, path in [('baseline', args.baseline), ('candidate', args.candidate),
                                                    ('references', reference_path), ('inputs', OUT / 'inputs.tsv.gz')]})
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / 'probe-changes.json').write_bytes((json.dumps(changes, ensure_ascii=False, indent=2) + '\n').encode())
    (args.output / 'probe-summary.json').write_bytes((json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode())
    print(json.dumps(report['groups'], indent=2))


if __name__ == '__main__':
    main()
