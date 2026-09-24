"""Offline experiment: estimate toneless reading shares from retained phrase counts.

No external labels, new readings or production writes. See PHRASE-READING-PLAN.md.
"""
import argparse
import collections
from fractions import Fraction
import hashlib
import json
from pathlib import Path
import unicodedata


def han(text):
    return bool(text) and all(unicodedata.name(c, '').startswith(
        ('CJK UNIFIED IDEOGRAPH-', 'CJK COMPATIBILITY IDEOGRAPH-')) for c in text)


def estimate(lines):
    rows = [line.split('\t') for line in lines]
    assert all(len(row) == 5 for row in rows), 'Unexpected dictionary schema'
    frequencies = {}
    readings = collections.defaultdict(set)
    phrases = collections.defaultdict(set)
    stats = collections.Counter(rows=len(rows))
    for reading, _, word, count, _ in rows:
        frequency = Fraction(count)
        assert frequency >= 0
        assert word not in frequencies or frequencies[word] == frequency, 'Inconsistent word frequency'
        frequencies[word] = frequency
        units = tuple(reading.split("'"))
        if not han(word):
            stats['non_han_rows'] += 1
        elif len(word) != len(units) or any(not unit for unit in units):
            stats['invalid_unit_count_rows'] += 1
        elif len(word) == 1:
            readings[word].add(reading)
        elif frequency > 0:
            phrases[word].add(units)

    evidence = collections.defaultdict(lambda: collections.defaultdict(Fraction))
    missing = set()
    for word, tuples in phrases.items():
        share = frequencies[word] / len(tuples)
        for units in tuples:
            for glyph, reading in zip(word, units):
                if reading not in readings.get(glyph, ()):
                    stats['missing_glyph_reading_observations'] += 1
                    missing.add((glyph, reading))
                else:
                    evidence[glyph][reading] += share
    stats['missing_glyph_reading_pairs'] = len(missing)
    stats['training_words'] = len(phrases)
    stats['training_distinct_reading_tuples'] = sum(map(len, phrases.values()))
    stats['training_multi_reading_words'] = sum(len(v) > 1 for v in phrases.values())
    stats['single_glyphs'] = len(readings)
    allocated = {}
    for glyph, alternatives in readings.items():
        mass = sum(evidence[glyph].values(), Fraction())
        stats['glyphs_with_phrase_evidence' if mass else 'glyphs_without_phrase_evidence'] += 1
        stats['multi_reading_glyphs'] += len(alternatives) > 1
        for reading in alternatives:
            share = evidence[glyph][reading] / mass if mass else Fraction(1, len(alternatives))
            allocated[glyph, reading] = frequencies[glyph] * share
            stats['reading_pairs_without_phrase_evidence'] += not evidence[glyph][reading]
        assert sum(allocated[glyph, r] for r in alternatives) == frequencies[glyph]
    result = []
    for row in rows:
        reading, _, word, count, _ = row
        value = allocated.get((word, reading), Fraction(count))
        if value != Fraction(count):
            row[3] = format(float(value), '.17g')
            stats['changed_rows'] += 1
        result.append('\t'.join(row))
    return result, allocated, dict(sorted(stats.items()))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path('app/src/main/assets/zh_tw.tsv'))
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    artifact_root = Path('artifacts').resolve()
    assert args.output.resolve().is_relative_to(artifact_root), 'Experiment output must stay in artifacts/'
    raw = args.source.read_bytes()
    rows, _, stats = estimate(raw.decode('utf-8').splitlines())
    data = ('\n'.join(rows) + '\n').encode('utf-8')
    args.output.mkdir(parents=True, exist_ok=True)
    target = args.output / 'phrase-reading-priors.tsv'
    assert target.resolve() != args.source.resolve()
    target.write_bytes(data)
    manifest = dict(role='Experimental toneless Pinyin prior; not admitted to production',
                    source_sha256=hashlib.sha256(raw).hexdigest(),
                    output_sha256=hashlib.sha256(data).hexdigest(),
                    estimator_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                    statistics=stats)
    (args.output / 'manifest.json').write_bytes((json.dumps(manifest, indent=2) + '\n').encode())
    print(json.dumps(manifest, indent=2))


if __name__ == '__main__':
    main()
