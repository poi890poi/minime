import hashlib,json,tempfile,unittest
from pathlib import Path
from evaluation_configuration import compare_configuration


class ConfigurationTest(unittest.TestCase):
    def setUp(self):
        self.directory=tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.paths=[Path(self.directory.name)/x for x in ('before.tsv','after.tsv')]
        for p in self.paths:p.write_bytes(b'output\n')
    def receipt(self,index,**overrides):
        p=self.paths[index]
        data=dict(format=1,packs=True,inputs=dict(spelling='s',english='e'),runtime={'engine':str(index)},output_sha256=hashlib.sha256(p.read_bytes()).hexdigest())
        data.update(overrides)
        Path(str(p)+'.config.json').write_text(json.dumps(data))
    def test_same_inputs_different_runtime_allowed(self):
        self.receipt(0);self.receipt(1)
        self.assertTrue(compare_configuration(*self.paths)['verified'])
    def test_original_missing_pack_error_rejected(self):
        self.receipt(0);self.receipt(1,packs=False)
        with self.assertRaisesRegex(ValueError,'dictionaries differ'):compare_configuration(*self.paths)
    def test_source_change_must_be_named(self):
        self.receipt(0);self.receipt(1,inputs=dict(spelling='new',english='e'))
        with self.assertRaisesRegex(ValueError,'Undeclared'):compare_configuration(*self.paths)
        result=compare_configuration(*self.paths,changed_inputs=['spelling'])
        self.assertEqual(['spelling'],result['actual_input_changes'])
    def test_stale_or_one_sided_receipt_rejected(self):
        self.receipt(0)
        with self.assertRaisesRegex(ValueError,'Only one'):compare_configuration(*self.paths)
        self.receipt(1);self.paths[1].write_bytes(b'changed\n')
        with self.assertRaisesRegex(ValueError,'stale'):compare_configuration(*self.paths)
    def test_legacy_not_claimed_matched(self):
        self.assertFalse(compare_configuration(*self.paths)['verified'])


if __name__=='__main__':unittest.main()
