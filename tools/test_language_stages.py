import unittest
from report_language_stages import correlate


class StageIdentity(unittest.TestCase):
    def touch(self,up=100,draw=180,frame=190):
        return dict(mode='chinese',interval_ms='60',action='key',expected='ab',up_ns=str(up),
                    candidate_pre_draw_ns=str(draw),candidate_submit_ns=str(frame))
    def request(self,at=110,delivered=140,finished=160):
        return dict(mode='chinese',query='ab',requested_ns=str(at),delivered_ns=str(delivered),
                    finished_ns=str(finished),glyph_ns='2',render_ns='3',votes_ns='4')
    def result(self,touches,requests):return correlate(touches,requests)['chinese/60']

    def test_same_spelling_is_joined_to_its_own_time_window(self):
        result=self.result([self.touch(),self.touch(200,280,290)],
                           [self.request(),self.request(210,240,260),self.request(20,40,60)])
        self.assertEqual(2,result['counts']['complete_timeline'])
        self.assertAlmostEqual(.00009,result['stages']['total']['mean_ms'])
        self.assertEqual(2,result['stages']['delivery']['observed'])

    def test_missing_and_ambiguous_requests_are_not_silently_selected(self):
        self.assertEqual(1,self.result([self.touch()],[])['counts']['unmatched_request'])
        result=self.result([self.touch()],[self.request(),self.request(120)])
        self.assertEqual(1,result['counts']['ambiguous_request'])
        self.assertFalse(result['stages'])

    def test_missing_callback_is_not_zero_latency(self):
        result=self.result([self.touch()],[self.request(delivered=0,finished=0)])
        self.assertEqual(1,result['counts']['undelivered']);self.assertFalse(result['stages'])

    def test_earlier_frame_is_not_assigned_to_later_callback(self):
        result=self.result([self.touch(draw=150)],[self.request()])
        self.assertEqual(1,result['counts']['frame_before_callback_finished'])
        self.assertNotIn('total',result['stages'])

    def test_out_of_order_callback_fails(self):
        with self.assertRaises(ValueError):self.result([self.touch()],[self.request(delivered=120,finished=115)])


if __name__ == '__main__':unittest.main()
