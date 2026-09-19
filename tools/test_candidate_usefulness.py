import unittest
from audit_candidate_usefulness import relevance,evaluate,aggregate
from report_usefulness_phone import extract,score
from audit_mixed_english import latin_kind,summarize

class UsefulnessTest(unittest.TestCase):
    def test_latin_extension_is_not_raw_restoration_or_han(self):
        self.assertEqual('prefix_extension',latin_kind('nylon','nyl'))
        self.assertEqual('raw',latin_kind('hello','hello'))
        self.assertEqual('apostrophe_restoration',latin_kind("can't",'cant'))
        self.assertEqual('other_latin',latin_kind('this','thos'))
        self.assertIsNone(latin_kind('年雨量','nyl'))
        report=summarize([dict(genre='test',id='1',condition='initial',raw='nyl',target='年雨量',outputs='nylon:0|年雨量:0')])
        self.assertEqual(1,report['groups']['all']['latin_top1'])
        self.assertEqual(1,report['groups']['all']['latin_before_first_useful'])

    def test_wrong_span_and_unrelated_homophone_are_not_useful_for_target(self):
        self.assertEqual('whole',relevance('明天下午',0,'mingtianxiawu','明天下午'))
        self.assertEqual('phrase',relevance('明天',8,'mingtianxiawu','明天下午'))
        self.assertEqual('glyph',relevance('明',4,'mingtianxiawu','明天下午'))
        self.assertEqual('off_target',relevance('明天',0,'mingtianxiawu','明天下午'))
        self.assertEqual('off_target',relevance('明天下午',8,'mingtianxiawu','明天下午'))
        self.assertEqual('off_target',relevance('名',4,'mingtianxiawu','明天下午'))

    def test_extra_off_target_choices_reduce_precision_without_changing_recall(self):
        a=evaluate([('明天',8)],'mingtianxiawu','明天下午',8)
        b=evaluate([('明天',8),('名',4),('命',4)],'mingtianxiawu','明天下午',8)
        self.assertEqual(a['first_phrase_rank'],b['first_phrase_rank'])
        self.assertEqual(a['best_glyphs'],b['best_glyphs'])
        self.assertEqual(1,aggregate([a])['target_compatible_precision'])
        self.assertAlmostEqual(1/3,aggregate([b])['target_compatible_precision'])

    def test_glyphs_do_not_count_as_phrases_or_hide_missing_choices(self):
        a=evaluate([('明',4)],'mingtianxiawu','明天下午',8)
        b=evaluate([('名',4)],'mingtianxiawu','明天下午',8)
        out=aggregate([a,b])
        self.assertEqual(0,out['with_phrase_choice'])
        self.assertEqual(1,out['without_useful_choice'])
        self.assertEqual(1,out['search_cost_denominator'])

    def test_phone_controls_and_raw_labels_are_not_candidates(self):
        self.assertEqual(['你好','你'],extract('google','typed',['你好','…','你','其他候選鍵','q','全形句號']))
        self.assertEqual(['你好','你','泥'],extract('google','expanded',['你好','隱藏其他候選鍵','選取拼音 n i','輸入簡體中文','簡','你','泥','','Page Down 鍵','刪除']))
        self.assertEqual(['你好'],extract('minime','typed',['Candidate list','Exact input nihao','你好','Candidate 你好','Expand candidates']))
        with self.assertRaises(ValueError):extract('google','typed',['q','w'])
        self.assertEqual(1,score(['你好','你','泥'],'你好')['off_target_slots'])

if __name__=='__main__':unittest.main()
