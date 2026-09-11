import unittest
from evaluate import should_stop

class ScreenTests(unittest.TestCase):
    def test_one_timeout_stops(self):self.assertTrue(should_stop([{'timeout':True}]))
    def test_three_slow_queries_stop(self):
        self.assertFalse(should_stop([{'engine_ns':300_000_000}]*2))
        self.assertTrue(should_stop([{'engine_ns':300_000_000}]*3))
    def test_fast_failures_are_not_slow_queries(self):
        self.assertFalse(should_stop([{'hit':False,'engine_ns':5_000_000}]*100))

if __name__=='__main__':unittest.main()
