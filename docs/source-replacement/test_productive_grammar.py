import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('grammar', Path(__file__).with_name('productive-grammar.py'))
grammar = importlib.util.module_from_spec(spec)
spec.loader.exec_module(grammar)


class ProductiveGrammar(unittest.TestCase):
    def test_attachment_requires_source_component_shape_and_exact_reconstruction(self):
        source = '''_exc={
"we're": [{ORTH:"we"},{ORTH:"'re"}],
"We're": [{ORTH:"We"},{ORTH:"'re"}],
"can't": [{ORTH:"ca"},{ORTH:"n't"}],
"o'clock": [{ORTH:"o'clock"}],
"who'd've": [{ORTH:"who"},{ORTH:"'d"},{ORTH:"'ve"}]
}'''
        self.assertEqual({"'re"}, grammar.source_suffixes(source))
        with self.assertRaises(ValueError):
            grammar.source_suffixes('''_exc={"we're":[{ORTH:"we"},{ORTH:"'ll"}]}''')

    def test_category_membership_and_attestation_are_separate_requirements(self):
        def page(i, word, ns=0, kind='page'):
            return dict(pageid=i, title=word, ns=ns, type=kind)
        snapshot = dict(members=[page(1,'Everyone'),page(1,'Everyone'),page(2,'some one'),
                                 page(3,'Category:English pronouns',14,'subcat'),page(4,'Her',1)])
        pronouns, pages = grammar.source_pronouns(snapshot)
        self.assertEqual(({'everyone'},3),(pronouns,pages))
        forms = grammar.evidence({"isn't"}, pronouns, {"'s", "'re"})
        self.assertEqual({"isn't","everyone's","everyone're"},forms)
        # The downstream export only admits pre-existing dictionary words.
        self.assertEqual({"isn't","everyone's"}, forms & {"isn't","everyone's","house's"})


if __name__ == '__main__':
    unittest.main()
