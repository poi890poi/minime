import itertools,unittest
from pinyin_boundaries import unique_boundaries

class BoundariesTest(unittest.TestCase):
    def test_exhaustive_small_language(self):
        units={'a','b','aa','ab','ba','aba'}
        for length in range(1,7):
            for chars in itertools.product('ab',repeat=length):
                raw=''.join(chars)
                for count in range(1,5):
                    expected={parts for parts in itertools.product(sorted(units),repeat=count) if ''.join(parts)==raw}
                    value="'".join(next(iter(expected))) if len(expected)==1 else None
                    self.assertEqual(value,unique_boundaries(raw,units,count),(raw,count))
    def test_explicit_boundaries_disambiguate(self):
        units={'a','b','ab','ba'}
        self.assertIsNone(unique_boundaries('aba',units,2))
        for raw in ["a'ba",'a ba','a-ba']:
            self.assertEqual("a'ba",unique_boundaries(raw,units,2))
        self.assertIsNone(unique_boundaries("a'b'a",units,2))
    def test_no_spelling_repair_or_count_guess(self):
        for raw in ['',"'a",'A','a1','á','ax']:
            self.assertIsNone(unique_boundaries(raw,{'a'},1))
        self.assertIsNone(unique_boundaries('a',{'a'},2))

if __name__=='__main__':unittest.main()
