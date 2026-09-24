import importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('audit',Path(__file__).with_name('audit-grammar-reference.py'))
audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)


class ReferenceTest(unittest.TestCase):
    def test_annotations_determine_category_not_flags(self):
        def block(tag,word="alpha'beta"):
            return '# newdoc id = GUM_conversation_test\n# sent_id = one\n1-2\t'+word+'\t_\t_\t_\t_\t_\t_\t_\t_\n1\talpha\talpha\tPRON\t_\t_\t0\troot\t_\t_\n2\tbeta\tbeta\t'+tag+'\t_\t_\t1\taux\t_\t_\n'
        for tag,expected in [('AUX','auxiliary'),('VERB','verb-without-auxiliary'),('NOUN','nonverbal')]:
            rows,excluded=audit.references(block(tag),'fixture')
            self.assertEqual(expected,rows[0]['category']);self.assertEqual('conversation',rows[0]['genre'])
            self.assertFalse(excluded)
        rows,_=audit.references(block('AUX','alpha-beta'),'fixture');self.assertEqual([],rows)


if __name__=='__main__':unittest.main()
