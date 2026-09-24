import unittest
from report_system_queue_work import report


class WorkReportTest(unittest.TestCase):
    def test_only_open_spans_after_measurement_can_be_ignored(self):
        integrity = dict(status='complete', health=[], expected_queues=1)
        queues = [dict(kind='queue', ts='10', dur='10')]
        rows = [dict(kind='slice', id='1', extra='0', ts='10', dur='10', name='draw'),
                dict(kind='state', ts='10', dur='10', name='Running'),
                dict(kind='state', ts='20', dur='-1', name='S')]
        result = report(integrity, queues, rows)
        self.assertEqual(result['leaves'][0]['share'], 1)
        rows[-1]['ts'] = '19'
        with self.assertRaisesRegex(ValueError, 'Incomplete'):
            report(integrity, queues, rows)

    def test_rejects_unhealthy_or_mismatched_inventory(self):
        with self.assertRaises(ValueError):
            report(dict(status='incomplete', health=[], expected_queues=1), [], [])
        with self.assertRaises(ValueError):
            report(dict(status='complete', health=[], expected_queues=1), [], [])


if __name__ == '__main__':
    unittest.main()
