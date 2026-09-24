import unittest
import report_touch_latency as timing

class TimingReportTest(unittest.TestCase):
    def row(self,ident='a',source='one',up=100,observed=True,action='key'):
        r=dict(mode='english',interval_ms='150',query='abc',query_id=ident,source=source,genre='conversation',condition='full',action=action,down_ns=up-20,up_ns=up)
        for k in ['editor_callback_ns','editor_submit_ns','candidate_submit_ns','pressed_submit_ns']:r[k]=up+10 if observed else 0
        return r
    def test_missing_is_not_zero_and_quantiles_are_conditional(self):
        rows=[self.row(up=1000000),self.row(up=2000000,observed=False),self.row(up=3000000,action='space')]
        result=timing.report(rows,'fixture');cell=result['groups']['english/150']
        self.assertEqual(2,cell['candidate_submission']['n']);self.assertEqual(1,cell['candidate_submission']['unobserved'])
        self.assertEqual(.00001,cell['candidate_submission']['p95_ms'])
        self.assertEqual(dict(letters=2,submitted_before_next_up=1,observed_submission_at_or_after_next_up=0,unobserved=1),cell['candidate_deadline'])
    def test_duplicate_spellings_do_not_link_episodes_or_stitch_strata(self):
        rows=[self.row('a','one',100),self.row('b','two',200),self.row('c','one',300),self.row('c','one',400,action='space')]
        result=timing.report(rows,'fixture')
        self.assertEqual(1,result['groups']['english/150']['candidate_deadline']['letters'])
        self.assertEqual(2,len(result['strata']))
        one=result['strata']['english/one/conversation/full/150']
        self.assertEqual(2,one['queries']);self.assertEqual(1,one['candidate_deadline']['letters'])
    def test_incomplete_labels_fail_and_legacy_remains_readable(self):
        rows=[self.row()];del rows[0]['source']
        with self.assertRaises(ValueError):timing.report(rows,'fixture')
        for k in ['query_id','genre','condition']:rows[0].pop(k,None)
        self.assertEqual({},timing.report(rows,'legacy')['strata'])
    def test_negative_timestamps_fail(self):
        row=self.row();row['editor_submit_ns']=1
        with self.assertRaises(AssertionError):timing.report([row],'fixture')

if __name__=='__main__':unittest.main()
