import unittest
from run import complete, rank, variants, distance, rows

class FakeEngine:
    """Controlled provider; no labels are passed to send()."""
    def __init__(self, entries):
        self.entries=entries;self.raw='';self.text='';self.candidates=[]
        self.engine_ns=0;self.wall_ns=0;self.commands=[]
    def send(self, command, value=''):
        self.commands.append((command,value))
        if command=='R':self.raw='';self.text='';self.candidates=[]
        elif command=='T':self.raw+=value;self.candidates=self.entries.get(self.raw,[])
        elif command=='B':self.raw=self.raw[:-int(value)] if value else self.raw;self.candidates=[]
        elif command=='L':self.text+=value
        elif command=='S':self.text+=self.candidates[value][0];self.raw='';self.candidates=[]
        return self

def utterance():
    return {'id':'fixture','source':'fixture','role':'test','previously_seen_text':False,'cross_role_repeat':False,
            'text':'甲乙。','parts':[{'kind':'japanese','text':'甲乙','raw':'ab','valid':True,
            'words':[{'text':'甲','raw':'a'},{'text':'乙','raw':'b'}]}, {'kind':'literal','text':'。'}]}

class EvaluatorTests(unittest.TestCase):
    def test_missing_output_is_failure_without_gold_insertion(self):
        e=FakeEngine({});r=complete(e,utterance(),'clause-first')
        self.assertFalse(r['success']);self.assertEqual(r['committed'],'。')
        self.assertEqual([v for c,v in e.commands if c=='L'],['。'])
        self.assertEqual(r['counts']['missing_word'],2)

    def test_retry_cost_includes_abandoned_input(self):
        e=FakeEngine({'a':[('甲',0)],'b':[('乙',0)]})
        r=complete(e,utterance(),'clause-first')
        self.assertTrue(r['success']);self.assertEqual(r['counts']['actions'],9)
        self.assertEqual(r['counts']['typed'],4);self.assertEqual(r['counts']['deletes'],2)
        direct=complete(e,utterance(),'word-first')
        self.assertEqual(direct['counts']['actions'],5)

    def test_prefix_consumption_is_not_whole_clause_success(self):
        e=FakeEngine({});e.candidates=[('甲乙',1)]
        self.assertEqual(rank(e,['甲乙']),-1)

    def test_ninth_slot_is_outside_budget(self):
        e=FakeEngine({});e.candidates=[(str(i),0) for i in range(9)]
        self.assertEqual(rank(e,['8']),-1);self.assertEqual(rank(e,['7']),7)

    def test_scoring_sensitive_to_wrong_and_empty_output(self):
        self.assertEqual(distance('甲乙','甲乙'),0)
        self.assertEqual(distance('甲乙',''),2);self.assertEqual(distance('甲乙','甲丙'),1)

    def test_error_control_reports_unchanged_transposition(self):
        values=dict(variants('aa','fixed'))
        self.assertEqual(values['transpose'],'aa');self.assertNotEqual(values['neighbor'],'aa')

    def test_frozen_corpus_keeps_entire_dialogues_separate(self):
        data=rows('utterances.jsonl.gz');by={role:{r['document'] for r in data if r['role']==role} for role in ['development','holdout']}
        self.assertFalse(by['development']&by['holdout']);self.assertEqual(len(data),1024)
        for r in data:self.assertEqual(''.join(p['text'] for p in r['parts']),r['text'])

if __name__=='__main__':unittest.main()
