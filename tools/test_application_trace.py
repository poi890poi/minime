import struct
import unittest
from report_application_trace import parse


def trace(records, overflow=False):
    header = ("*version\n3\ndata-file-overflow="+str(overflow).lower()+"\nclock=dual\n"
              "*threads\n1\tmain\n2\tworker\n*methods\n"
              "0x4\tdev/minime/core/CompositionEngine\tapplyCandidates\t()V\n"
              "0x8\tfixture/Sorter\tsort\t()V\n*end\n").encode()
    binary = struct.pack("<4sHHQH", b"SLOW", 3, 32, 0, 14)+bytes(14)
    return header+binary+b"".join(struct.pack("<HIII", thread, method, clock, clock) for thread, method, clock in records)


class TraceContract(unittest.TestCase):
    def test_attribution_ignores_worker_and_does_not_double_count(self):
        report = parse(trace([(1,4,10),(1,8,12),(2,4,15),(1,9,17),(1,5,20)]))
        self.assertEqual(10, report["sampled_application_us"])
        self.assertEqual(10, sum(v for _, v in report["exclusive_leaf_us"]))
        self.assertEqual(5, dict(report["exclusive_leaf_us"])["fixture/Sorter\tsort\t()V"])
    def test_rejects_overflow(self):
        with self.assertRaises(ValueError): parse(trace([], True))
    def test_rejects_unbalanced_stack(self):
        with self.assertRaises(ValueError): parse(trace([(1,4,10),(1,9,20)]))
    def test_rejects_truncated_record(self):
        with self.assertRaises(ValueError): parse(trace([(1,4,10)])[:-1])
    def test_rejects_clock_regression(self):
        with self.assertRaises(ValueError): parse(trace([(1,4,10),(1,5,9)]))


if __name__ == "__main__": unittest.main()
