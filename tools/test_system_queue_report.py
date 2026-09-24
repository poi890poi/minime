import unittest
from report_system_queue import analyze

def row(kind,name='',ts=0,dur=0):return dict(kind=kind,id='1',name=name,ts=str(ts),dur=str(dur),aux='1')
def fixture():
    return [row('main'),row('queue','MinIME.decoder.queue.1',100,100),
            row('callback','MinIME.decoder.callback.1',201,10),
            row('state','Running',90,40),row('state','S',130,100),row('frame','Choreographer#doFrame',120,50)]
REQUESTS=[dict(id='1',posted_ns='1',entered_ns='2')]

class Capture(unittest.TestCase):
    def test_complete_partition_and_frame_overlap(self):
        r=analyze(fixture(),REQUESTS)
        self.assertEqual(r['status'],'complete')
        self.assertAlmostEqual(r['duration_weighted_state_shares']['running'],.3)
        self.assertAlmostEqual(r['stages']['frame_wall_overlap']['mean_ms'],50/1e6)
    def test_loss_cannot_emit_causal_statistics(self):
        r=analyze(fixture()+[row('health','traced_buf_chunks_discarded',dur=1)],REQUESTS)
        self.assertEqual(r['status'],'incomplete');self.assertNotIn('stages',r)
    def test_missing_or_duplicate_markers_reject(self):
        for rows in [fixture()[1:],fixture()+[fixture()[1]],fixture()[:2]]:
            self.assertEqual(analyze(rows,REQUESTS)['status'],'incomplete')
    def test_unfinished_span_and_reversed_boundary_reject(self):
        rows=fixture();rows[1]['dur']='-1'
        self.assertEqual(analyze(rows,REQUESTS)['status'],'incomplete')
        rows=fixture();rows[2]['ts']='199'
        self.assertEqual(analyze(rows,REQUESTS)['status'],'incomplete')

if __name__=='__main__':unittest.main()
