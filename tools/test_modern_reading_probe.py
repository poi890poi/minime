"""Reference normalization and metric boundaries, without production vocabulary."""
import unittest
from audit_moe_readings import pinyin, single_han, zhuyin
from report_modern_reading_probe import score


class ReferenceTest(unittest.TestCase):
    def test_pinyin_preserves_vowel_identity(self):
        self.assertEqual(pinyin(' lǜ nǚ '), "lv'nv")
        self.assertEqual(pinyin('lü\u0300'), 'lv')
        self.assertEqual(pinyin('lù'), 'lu')
        self.assertIsNone(pinyin('lu4'))
        self.assertIsNone(pinyin(''))

    def test_zhuyin_tones_and_invalid_inputs(self):
        self.assertEqual(zhuyin(' ㄉㄜ '), 'ㄉㄜˉ')
        self.assertEqual(zhuyin('˙ㄉㄜ'), 'ㄉㄜ˙')
        self.assertEqual(zhuyin('ㄉㄜ˙'), 'ㄉㄜ˙')
        self.assertIsNone(zhuyin('ㄉㄜˉˋ'))
        self.assertIsNone(zhuyin(''))

    def test_slots_partition_and_partial_is_not_complete(self):
        glyphs = [chr(0x4e00+i) for i in range(5)]
        a, b, c, d, e = glyphs
        row = dict(outputs=f'{a}:0|{b}:0|{c}:0|{d}:1|{a+b}:0|literal:0|{e}:0', space=d)
        values, supported = score(row, {a, d}, {a, b, d, e})
        self.assertEqual(supported, {a})
        self.assertEqual(values['reference_pairs_available'], 1)
        self.assertTrue(values['space_supported_by_reference'])
        self.assertEqual(values['first8_supported_complete_glyph_slots'], 1)
        self.assertEqual(values['first8_reference_mismatch_glyph_slots'], 2)
        self.assertEqual(values['first8_uncovered_glyph_slots'], 1)
        self.assertEqual(values['first8_partial_slots'], 1)
        self.assertEqual(values['first8_phrase_or_literal_slots'], 2)
        self.assertEqual(values['first8_slots'], sum(v for k, v in values.items()
                                                  if k.startswith('first8_') and k != 'first8_slots'))

    def test_empty_and_supplementary_han(self):
        values, supported = score(dict(outputs='', space='raw'), set(), set())
        self.assertEqual(values['first8_slots'], 0)
        self.assertFalse(values['first_nonraw_supported_by_reference'])
        self.assertEqual(supported, set())
        self.assertTrue(single_han(chr(0x20000)))
        self.assertFalse(single_han(chr(0x20000) * 2))


if __name__ == '__main__':
    unittest.main()
