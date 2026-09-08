"""Source admission and freeze contracts, independent of real vocabulary examples."""
import copy,json,tempfile,unittest
from pathlib import Path
import sources

class SourceRegistryTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name)
        (self.root/'sources').mkdir();(self.root/'third_party').mkdir()
        (self.root/'third_party/input.txt').write_bytes(b'original\n')
        self.item=dict(id='local',name='Authored source',origin='taiwan-authored',decision='retain',uses=['vocabulary'],inputs=['third_party/input.txt'],license={'id':'CC0-1.0','status':'verified'},editorial='Named editors',coverage='Known inventory',limitations='Not frequency',evidence=['https://example.org/source'],reason='Source review recorded',addon_prefixes=['local:'])
        self.doc={'format':1,'sources':[self.item]};self.write_catalog()
    def write_catalog(self):
        (self.root/sources.CATALOG).write_text(json.dumps(self.doc),encoding='utf8')
    def lock(self):
        value=sources.make_lock(self.root,sources.catalog(self.root))
        (self.root/sources.LOCK).write_text(json.dumps(value),encoding='utf8');return value
    def test_valid_catalog_and_lock(self):
        self.lock();sources.verify(self.root)
    def test_changed_input_fails_verification(self):
        self.lock();(self.root/'third_party/input.txt').write_text('changed',encoding='utf8')
        with self.assertRaises(sources.SourceError):sources.verify(self.root)
    def test_line_ending_only_change_is_portable(self):
        self.lock();(self.root/'third_party/input.txt').write_bytes(b'original\r\n');sources.verify(self.root)
    def test_unaudited_pilot_cannot_have_production_inputs(self):
        self.item['decision']='pilot';self.write_catalog()
        with self.assertRaises(sources.SourceError):sources.catalog(self.root)
    def test_simplified_corpus_cannot_be_relabelled_retain(self):
        self.item['origin']='simplified-chinese';self.write_catalog()
        with self.assertRaises(sources.SourceError):sources.catalog(self.root)
    def test_unresolved_rights_cannot_be_retained(self):
        self.item['license']['status']='unresolved';self.write_catalog()
        with self.assertRaises(sources.SourceError):sources.catalog(self.root)
    def test_frozen_source_refresh_rejected(self):
        self.item.update(decision='replace',replacement='New authored dataset');self.write_catalog();old=self.lock()
        (self.root/'third_party/input.txt').write_bytes(b'expanded\n')
        with self.assertRaises(sources.SourceError):sources.make_lock(self.root,self.doc,old)
    def test_frozen_rows_cannot_expand_even_with_same_input(self):
        path=self.root/'app/src/main/assets';path.mkdir(parents=True)
        asset=path/'addons.tsv';asset.write_text('taiwan\tfixture\tfixture\tlocal:1\ttest\n',encoding='utf8')
        self.item.update(decision='replace',replacement='Replacement');self.write_catalog();old=self.lock()
        asset.write_text(asset.read_text(encoding='utf8')+'taiwan\textra\textra\tlocal:2\ttest\n',encoding='utf8')
        with self.assertRaises(sources.SourceError):sources.make_lock(self.root,self.doc,old)
    def test_unknown_packaged_source_rejected(self):
        path=self.root/'app/src/main/assets';path.mkdir(parents=True)
        (path/'addons.tsv').write_text('taiwan\tfixture\tfixture\tunknown:1\ttest\n',encoding='utf8')
        with self.assertRaises(sources.SourceError):self.lock()
    def test_changed_audit_evidence_requires_review(self):
        (self.root/'audit.json').write_text('{}',encoding='utf8');self.item['audit_files']=['audit.json'];self.write_catalog();self.lock()
        (self.root/'audit.json').write_text('{"changed":true}',encoding='utf8')
        with self.assertRaises(sources.SourceError):sources.verify(self.root)
    def test_ambiguous_file_ownership_rejected(self):
        second=copy.deepcopy(self.item);second['id']='other';self.doc['sources'].append(second)
        with self.assertRaises(sources.SourceError):sources.inventory(self.root,self.doc)
    def test_missing_source_pattern_rejected(self):
        self.item['inputs']=['third_party/missing.*']
        with self.assertRaises(sources.SourceError):sources.inventory(self.root,self.doc)
    def test_relative_path_escape_rejected(self):
        with self.assertRaises(sources.SourceError):sources.safe_path(self.root,'../input.txt')
    def test_duplicate_source_id_rejected(self):
        self.doc['sources'].append(copy.deepcopy(self.item));self.write_catalog()
        with self.assertRaises(sources.SourceError):sources.catalog(self.root)

    def evaluation(self,**changes):
        self.item['uses'].append('evaluation');self.write_catalog()
        (self.root/'essay.txt').write_text('Independent prose',encoding='utf8')
        corpus=dict(id='essays',source_ids=['local'],genre=['essay'],selection='Frozen source inventory',limitations='Not conversational',leakage='Overlap not yet measured',files=['essay.txt'],report_by=['genre'],role='audit',exposure='inspected')
        corpus.update(changes)
        (self.root/sources.EVALUATION).write_text(json.dumps({'format':1,'corpora':[corpus]}),encoding='utf8')
    def test_essay_audit_is_separate_from_production(self):
        self.evaluation();self.lock();sources.verify(self.root)
    def test_evaluation_file_cannot_also_be_production(self):
        self.evaluation(files=['third_party/input.txt'])
        with self.assertRaises(sources.SourceError):self.lock()
    def test_evaluation_requires_genre_breakdown(self):
        self.evaluation(report_by=['input-mode'])
        with self.assertRaises(sources.SourceError):self.lock()
    def test_inspected_essays_are_not_fresh_holdout(self):
        self.evaluation(role='holdout')
        with self.assertRaises(sources.SourceError):self.lock()
    def test_evaluation_drift_fails_verification(self):
        self.evaluation();self.lock();(self.root/'essay.txt').write_text('Changed sample',encoding='utf8')
        with self.assertRaisesRegex(sources.SourceError,'evaluation'):sources.verify(self.root)

if __name__=='__main__':unittest.main()
