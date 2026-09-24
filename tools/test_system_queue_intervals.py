import unittest
from system_queue_intervals import partition,union_duration,exclusive_work

class Intervals(unittest.TestCase):
    def test_clipped_states_and_missing_coverage(self):
        self.assertEqual(partition(10,40,[(0,15,'Running'),(15,20,'R+'),(25,35,'S'),(35,50,'D')]),
                         dict(running=5,runnable=5,sleeping=10,other=5,unknown=5))
    def test_overlaps_are_not_double_counted_as_cpu_time(self):
        with self.assertRaises(ValueError):partition(0,20,[(0,10,'Running'),(9,20,'S')])
    def test_nested_frame_slices_have_union_not_sum(self):
        self.assertEqual(union_duration(10,30,[(0,15),(10,25),(12,18),(27,40)]),18)
    def test_no_state_is_unknown_not_sleep(self):
        self.assertEqual(partition(0,10,[])['unknown'],10)
        self.assertEqual(partition(0,10,[(0,5,'[NULL]')])['unknown'],10)
    def test_boundaries_and_invalid_ranges(self):
        self.assertEqual(sum(partition(5,5,[]).values()),0)
        self.assertEqual(union_duration(10,20,[(0,10),(20,30)]),0)
        with self.assertRaises(ValueError):partition(5,1,[])
        with self.assertRaises(ValueError):union_duration(0,20,[(12,11)])
    def test_exclusive_child_and_state_accounting(self):
        totals,states=exclusive_work([(0,20)],[(1,0,2,18,'frame'),(2,1,5,12,'child')],[(0,10,'Running'),(10,20,'S')])
        self.assertEqual(dict(totals),{'[untraced]':4,'frame':9,'child':7})
        self.assertEqual(dict(states['child']),{'Running':5,'S':2})
    def test_overlapping_requests_are_duration_weighted(self):
        totals,_=exclusive_work([(0,10),(5,15)],[],[])
        self.assertEqual(totals['[untraced]'],20)
    def test_ambiguous_leaf_and_states_fail(self):
        with self.assertRaises(ValueError):exclusive_work([(0,20)],[(1,0,0,15,'a'),(2,0,10,20,'b')],[])
        with self.assertRaises(ValueError):exclusive_work([(0,20)],[],[(0,15,'S'),(10,20,'R')])

if __name__=='__main__':unittest.main()
