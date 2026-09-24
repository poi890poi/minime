import importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('prepare',Path(__file__).with_name('prepare-grammar-holdout.py'))
prepare=importlib.util.module_from_spec(spec);spec.loader.exec_module(prepare)


class HoldoutTest(unittest.TestCase):
    def test_document_boundaries_and_duplicate_failure(self):
        text='# newdoc id = alpha\n# text = one two\n# newdoc id = beta\n# text = three four\n'
        self.assertEqual({'alpha':'one two','beta':'three four'},prepare.documents(text))
        with self.assertRaisesRegex(ValueError,'Repeated'):prepare.documents(text+'# newdoc id = alpha\n')
        with self.assertRaisesRegex(ValueError,'before'):prepare.documents('# text = unidentified\n')
    def test_twenty_tokens_and_normalization(self):
        words=['token'+str(i) for i in range(20)]
        self.assertEqual([],list(prepare.windows(' '.join(words[:19]))))
        self.assertEqual(1,len(list(prepare.windows(' '.join(words)))))
        self.assertEqual(list(prepare.windows(' '.join(words))),list(prepare.windows(' '.join(words).upper())))
        self.assertEqual(list(prepare.windows("alpha'beta "+' '.join(words))),list(prepare.windows('alpha’beta '+ ' '.join(words))))


if __name__=='__main__':unittest.main()
