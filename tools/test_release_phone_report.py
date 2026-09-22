import unittest
from report_release_phone import report


class ReleasePhoneReportTest(unittest.TestCase):
    def setUp(self):
        self.plans = [dict(id='one', mode='pinyin', source=dict(
            genre='fixture', condition='full', raw='ab', target='甲乙'))]
        self.record = dict(provider='minime', id='one', status='observed', steps=[
            dict(stage='typed', text='ab', visible=['Candidate 甲', 'Candidate 甲乙']),
            dict(stage='accepted', text='甲乙', composingStart=-1)])

    def test_missing_provider_stays_missing(self):
        value = report(self.plans, [self.record])
        self.assertEqual(value['missing'], [dict(provider='google', id='one')])
        self.assertEqual(value['totals']['minime/fixture/typed']['compatible_slots'], 2)
        self.assertNotIn('expanded', value['totals'])

    def test_unverified_input_excluded_from_literal_verified_totals(self):
        self.record['steps'][0]['text'] = 'a'
        value = report(self.plans, [self.record])
        self.assertEqual(value['totals'], {})
        self.assertFalse(value['rows'][0]['whole_target_committed'])
        self.assertEqual(len(value['unverified_input']), 1)

    def test_internal_preedit_has_only_end_to_end_ui_evidence(self):
        self.record['provider'] = 'google'
        self.record['steps'][0].update(text='', visible=['甲乙', '其他候選鍵'])
        value = report(self.plans, [self.record])
        self.assertEqual(value['totals'], {})
        self.assertEqual(value['end_to_end_ui_totals']['google/fixture/typed']['whole_available'], 1)
        self.assertTrue(value['rows'][0]['ui_whole_target_committed'])

    def test_english_not_scored_as_chinese(self):
        self.plans[0]['mode'] = 'english'
        value = report(self.plans, [self.record])
        self.assertEqual(value['totals'], {})
        self.assertIn('ranking_scope', value['rows'][0])

    def test_duplicate_observation_rejected(self):
        with self.assertRaises(ValueError):
            report(self.plans, [self.record, self.record])

    def test_missing_candidate_boundary_stays_unavailable(self):
        self.record['provider'] = 'google'
        value = report(self.plans, [self.record])
        self.assertEqual(value['totals'], {})
        self.assertIn('unavailable', value['rows'][0]['stages']['typed'])


if __name__ == '__main__':
    unittest.main()
