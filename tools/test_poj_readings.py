import unittest
from poj_readings import variants


class PojReadingTest(unittest.TestCase):
    def read(self, key, output):
        skipped = []
        result = list(variants({'PojInput': key, 'PojUnicode': output}, 'fixture', skipped))
        return result, skipped

    def test_aligned_variants_keep_tones(self):
        result, skipped = self.read('a2/a3', 'á/à')
        self.assertEqual(result, [(['a', 'a2'], 'á'), (['a', 'a3'], 'à')])
        self.assertEqual(skipped, [])

    def test_unicode_input_does_not_drop_attested_output(self):
        self.assertEqual(self.read('á', 'á')[0], [(['a'], 'á')])
        self.assertEqual(self.read('o͘7', 'ō͘')[0], [(['oo'], 'ō͘')])
        self.assertEqual(self.read('ı̍', 'ı̍')[0], [(['i'], 'ı̍')])

    def test_unaligned_is_not_guessed(self):
        result, skipped = self.read('a2/a3', 'á')
        self.assertEqual(result, [])
        self.assertEqual(len(skipped), 1)

    def test_no_generated_tone_permutations(self):
        self.assertEqual(self.read('a2-bo5', 'á-bô')[0], [(['a-bo', 'a2-bo5'], 'á-bô')])

    def test_punctuation_is_not_deleted_to_invent_words(self):
        result, skipped = self.read('a, b', 'a, b')
        self.assertEqual(result, [])
        self.assertEqual(len(skipped), 1)


if __name__ == '__main__':
    unittest.main()
