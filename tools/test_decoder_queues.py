import unittest
from report_decoder_queues import analyze,correlate


class Queues(unittest.TestCase):
    def row(self,**changes):
        row=dict(id=1,requested_ns=10,scheduled_ns=20,worker_ns=50,providers_ns=70,
                 posted_ns=80,entered_ns=110,finished_ns=130,cancel_ns=0,terminal=1)
        row.update(changes);return row

    def test_independent_intervals_and_identity(self):
        result=analyze([self.row()],delay_ns=20)
        expected=dict(scheduled_wait=30,schedule_deadline_overshoot=10,provider_work=20,
                      before_post=10,main_queue=30,callback=20,request_to_main=100,request_to_finish=120)
        for key,value in expected.items():self.assertAlmostEqual(value/1e6,result['stages'][key]['mean_ms'])
        with self.assertRaises(ValueError):analyze([self.row(id=2)],delay_ns=20)

    def test_never_started_cancellation_has_no_latency(self):
        r=self.row(worker_ns=0,providers_ns=0,posted_ns=0,entered_ns=0,finished_ns=0,cancel_ns=30,terminal=0)
        result=analyze([r],delay_ns=20)
        self.assertEqual(1,result['counts']['cancelled_before_worker']);self.assertFalse(result['stages'])

    def test_pipeline_and_stale_cancellation_are_distinct(self):
        rows=[self.row(posted_ns=0,entered_ns=0,finished_ns=0,cancel_ns=60,terminal=2),
              self.row(id=2,cancel_ns=90,terminal=3)]
        result=analyze(rows,delay_ns=20)
        self.assertEqual(1,result['counts']['cancelled_pipeline']);self.assertEqual(1,result['counts']['stale_delivery'])
        self.assertNotIn('accepted_main_queue',result['stages'])

    def test_inconsistent_chronology_is_rejected(self):
        for changes in [dict(worker_ns=30),dict(providers_ns=40),dict(posted_ns=60),dict(entered_ns=70),dict(finished_ns=100),dict(terminal=2)]:
            with self.assertRaises(ValueError):analyze([self.row(**changes)],delay_ns=20)

    def test_pending_is_reported_without_inventing_completion(self):
        result=analyze([self.row(entered_ns=0,finished_ns=0,terminal=0)],delay_ns=20)
        self.assertEqual(1,result['counts']['posted_unresolved']);self.assertNotIn('main_queue',result['stages'])

    def test_attribution_is_temporal_and_ambiguity_is_preserved(self):
        def touch(up):return dict(action='key',mode='chinese',interval_ms='60',up_ns=up,source='fixture',genre='conversation',condition='whole')
        def pending(identity,at):return self.row(id=identity,requested_ns=at,scheduled_ns=at+1,worker_ns=0,providers_ns=0,posted_ns=0,entered_ns=0,finished_ns=0,cancel_ns=at+2,terminal=0)
        requests=[pending(1,110),pending(2,210),pending(3,220)]
        result=correlate([touch(100),touch(200),dict(action='space',up_ns=300)],requests)
        group=result['groups']['chinese/60']
        self.assertEqual(dict(letters=2,unique_request=1,ambiguous=1),group['attribution'])
        self.assertEqual(2,result['requests_not_uniquely_attributed'])


if __name__=='__main__':unittest.main()
