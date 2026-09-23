import csv
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


class TouchReportContract(unittest.TestCase):
    def test_actual_cadence_excludes_between_query_waits_and_keeps_missing_frames(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp); source=root/'touch.tsv'
            fields=['mode','interval_ms','query','action','down_ns','up_ns','editor_callback_ns',
                    'editor_submit_ns','candidate_submit_ns','pressed_submit_ns']
            with source.open('w',newline='',encoding='utf-8') as out:
                writer=csv.DictWriter(out,fields,delimiter='\t');writer.writeheader()
                for query,action,up in [('ab','key',100),('ab','key',160),('ab','space',240),
                                        ('c','key',1000),('c','space',1060)]:
                    writer.writerow(dict(mode='chinese',interval_ms=60,query=query,action=action,
                        down_ns=(up-25)*1000000,up_ns=up*1000000,editor_callback_ns=(up+1)*1000000,
                        editor_submit_ns=(up+10)*1000000,candidate_submit_ns=0 if up==160 else (up+(70 if up==100 else 20))*1000000,
                        pressed_submit_ns=(up-15)*1000000))
            subprocess.run([sys.executable,str(Path(__file__).with_name('report_touch_latency.py')),
                            '--input',str(source),'--output',str(root/'out')],check=True,capture_output=True)
            report=json.loads((root/'out'/'summary.json').read_text())['groups']['chinese/60']
            cadence=report['injected_key_interval']
            self.assertEqual(3,cadence['observed'])
            self.assertEqual(60,cadence['p50_ms']);self.assertEqual(80,cadence['max_ms'])
            self.assertEqual(1,report['candidate_submission']['unobserved'])
            self.assertEqual(3,report['editor_submission']['observed'])
            self.assertEqual(dict(letters=3,submitted_before_next_up=1,
                observed_submission_at_or_after_next_up=1,unobserved=1),report['candidate_deadline'])


if __name__=='__main__': unittest.main()
