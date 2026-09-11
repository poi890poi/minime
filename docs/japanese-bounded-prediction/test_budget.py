"""Synthetic cancellation and recovery contracts, never language accuracy."""
import unittest
from evaluate import Worker, WORK

class BudgetContract(unittest.TestCase):
    def test_zero_budget_discards_all_output(self):
        w=Worker('zero',WORK/'predictive-fixture')
        try:
            for q in ['ka','kaki','a','']:
                r=w.query(q)
                self.assertFalse(r['available'])
                self.assertEqual([],r['choices'])
        finally:w.close()

    def test_complete_prefix_and_repeated_query(self):
        a=Worker('bounded',WORK/'predictive-fixture')
        b=Worker('uncapped',WORK/'predictive-fixture')
        try:
            for raw in ['ka','kaki','kaku','sashi','a','','ka']:
                r=a.query(raw);expected=b.query(raw)
                self.assertTrue(r['available'])
                self.assertEqual(expected['choices'],r['choices'])
                if raw=='kaki':
                    self.assertIn('かきく',r['choices'])
                    self.assertNotIn('かく',r['choices'])
        finally:a.close();b.close()

    def test_large_abort_does_not_poison_next_request(self):
        w=Worker()
        try:
            # Mechanism stress input, not a dictionary inclusion or ranking test.
            large=w.query('na')
            self.assertFalse(large['available'])
            self.assertEqual([],large['choices'])
            absent=w.query('')
            self.assertTrue(absent['available'])
            self.assertEqual([],absent['choices'])
        finally:w.close()

if __name__=='__main__':unittest.main()
