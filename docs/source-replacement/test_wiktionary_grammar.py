import importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('audit',Path(__file__).with_name('wiktionary-grammar-audit.py'))
audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)


class DiscoveryTest(unittest.TestCase):
    def test_empty_continued_page_and_category_cycle(self):
        calls=[]
        def fetch(p):
            calls.append(dict(p))
            if len(calls)==1:return dict(query=dict(categorymembers=[]),**{'continue':dict(cmcontinue='next')})
            if p['cmtitle']==audit.ROOT_CATEGORY:
                return dict(query=dict(categorymembers=[dict(pageid=1,title="alpha'beta",type='page',ns=0),dict(pageid=2,title='Category:child',type='subcat',ns=14)]))
            return dict(query=dict(categorymembers=[dict(pageid=3,title=audit.ROOT_CATEGORY,type='subcat',ns=14)]))
        result=audit.walk(fetch)
        self.assertEqual(3,len(calls));self.assertEqual('next',calls[1]['cmcontinue'])
        self.assertEqual(2,len(result['categories']));self.assertEqual(3,len(result['members']))
    def test_continuation_cycle_rejected(self):
        with self.assertRaisesRegex(ValueError,'continuation cycle'):
            audit.walk(lambda p:dict(query=dict(categorymembers=[]),**{'continue':dict(cmcontinue='same')}))
    def test_error_or_missing_page_rejected(self):
        for value in ({'error':{}},{'query':{}}):
            with self.assertRaisesRegex(ValueError,'Incomplete'):audit.walk(lambda p:value)
    def test_duplicate_page_is_not_silently_deduplicated(self):
        item=dict(pageid=1,title="alpha'beta",type='page',ns=0)
        with self.assertRaisesRegex(ValueError,'Repeated'):
            audit.walk(lambda p:dict(query=dict(categorymembers=[item,item])))


if __name__=='__main__':unittest.main()
