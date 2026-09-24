"""Structural controls using artificial symbols/readings, not language labels."""
from fractions import Fraction
import unittest
from phrase_reading_prior import estimate


def row(word, reading, frequency, tone='a'):
    return '\t'.join((reading, tone, word, str(frequency), tone))


class PriorTest(unittest.TestCase):
    def fixture(self):
        a, b = chr(0x4E00), chr(0x4E01)
        return a, b, [row(a, 'a', 100), row(a, 'b', 100), row(b, 'c', 30),
                      row(a+b, "a'c", 24), row(b+a, "c'b", 8)]

    def test_conservation_and_ratio(self):
        a, b, lines = self.fixture()
        output, values, stats = estimate(lines)
        self.assertEqual(values[a, 'a'], 75)
        self.assertEqual(values[a, 'b'], 25)
        self.assertEqual(values[b, 'c'], 30)
        self.assertEqual(output[2:], lines[2:])
        self.assertEqual([r.split('\t')[:3] + r.split('\t')[4:] for r in output],
                         [r.split('\t')[:3] + r.split('\t')[4:] for r in lines])

    def test_order_and_duplicate_tones(self):
        a, b, lines = self.fixture()
        output, values, _ = estimate(lines)
        reversed_output, reversed_values, _ = estimate(list(reversed(lines)))
        self.assertEqual(output, list(reversed(reversed_output)))
        self.assertEqual(values, reversed_values)
        extra = lines + [row(a+b, "a'c", 24, 'other-tone')]
        self.assertEqual(values, estimate(extra)[1])

    def test_ambiguous_phrase_distributes_once(self):
        a, b, lines = self.fixture()
        lines[-1] = row(a+b, "b'c", 24)
        _, values, _ = estimate(lines)
        self.assertEqual(values[a, 'a'], 50)
        self.assertEqual(values[a, 'b'], 50)

    def test_fallback_zero_and_unmapped(self):
        a, b, lines = self.fixture()
        output, values, _ = estimate(lines[:3])
        self.assertEqual(values[a, 'a'], 50)
        self.assertEqual(values[a, 'b'], 50)
        _, values, stats = estimate(lines[:3] + [row(a+b, "a'z", 24)])
        self.assertEqual(values[a, 'b'], 0)
        self.assertEqual(values[b, 'c'], 30)
        self.assertEqual(stats['missing_glyph_reading_pairs'], 1)
        zero = [row(a, 'a', 0), row(a, 'b', 0)]
        self.assertEqual(estimate(zero)[0], zero)

    def test_repeated_glyph_contributes_each_occurrence(self):
        a, b, lines = self.fixture()
        _, values, _ = estimate(lines[:3] + [row(a+a+b, "a'a'c", 9), row(b+a, "c'b", 9)])
        self.assertEqual(values[a, 'a'], Fraction(200, 3))

    def test_bad_schema_and_conflicting_frequency_fail(self):
        a, b, lines = self.fixture()
        with self.assertRaises(AssertionError):
            estimate(lines + [row(a, 'd', 101)])
        with self.assertRaises(AssertionError):
            estimate(['invalid'])
        _, _, stats = estimate(lines + [row(a+b, 'a', 24)])
        self.assertEqual(stats['invalid_unit_count_rows'], 1)


if __name__ == '__main__':
    unittest.main()
