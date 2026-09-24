"""Selection boundaries independent of vocabulary targets and decoder output."""
import copy,csv,io,json,unittest
import make_language_timing_corpus as corpus

class SelectionTest(unittest.TestCase):
    def row(self,raw='abc',mode='english',source='s',condition='full',line=1):
        return dict(mode=mode,source=source,genre='fixture',condition=condition,raw=raw,
                    source_file='fixture.tsv',source_line=line,document='doc',identity=str(line),source_role='fixture')
    def test_mode_source_condition_boundaries(self):
        rows=[self.row(mode=m,source=s,condition=c) for m in ['english','chinese'] for s in ['s','t'] for c in ['full','half']]
        selected,excluded,stats=corpus.select(rows,1)
        self.assertEqual(8,len(selected));self.assertEqual({},excluded);self.assertEqual(8,len(stats))
    def test_no_invented_spelling_and_physical_key_filter(self):
        good=['a','z'*32,'neutral','chhiah'];bad=['','A','foo bar',"can't",'a1','é','あ','x'*33]
        selected,excluded,_=corpus.select([self.row(s,line=i) for i,s in enumerate(good+bad)])
        self.assertEqual(set(good),{r['raw'] for r in selected})
        self.assertEqual(len(bad),excluded['not-1-to-32-lowercase-letter-keys'])
    def test_stable_hash_sampling_and_dedup_without_targets(self):
        rows=[self.row(chr(97+i//26)+chr(97+i%26),line=i) for i in range(80)]
        rows.append(self.row(rows[0]['raw'],line=1000))
        before=copy.deepcopy(rows);a=corpus.select(rows,16);b=corpus.select(list(reversed(rows)),16)
        self.assertEqual(a,b);self.assertEqual(before,rows)
        self.assertEqual(16,len(a[0]));self.assertEqual(1,a[1]['duplicate-input-within-stratum'])
        self.assertEqual(16,len({r['raw'] for r in a[0]}))
    def test_sparse_stratum_is_not_filled_with_other_source(self):
        rows=[self.row('abc',source='sparse')]+[self.row(chr(97+i)+'x',source='other') for i in range(20)]
        selected,_,stats=corpus.select(rows,16)
        self.assertEqual(1,stats['english/sparse/full']['selected']);self.assertEqual(17,len(selected))
    def test_pinned_corpus_reproduces_and_asset_is_identical(self):
        rows,_=corpus.read_rows();selected,excluded,strata=corpus.select(rows)
        stream=io.StringIO(newline='');writer=csv.DictWriter(stream,corpus.FIELDS,delimiter='\t',lineterminator='\n');writer.writeheader();writer.writerows(selected)
        data=stream.getvalue().encode('utf-8');directory=corpus.ROOT/corpus.OUTPUT
        manifest=json.loads((directory/'manifest.json').read_text(encoding='utf-8'))
        self.assertEqual(data,(directory/'inputs.tsv').read_bytes());self.assertEqual(data,(corpus.ROOT/corpus.ASSET).read_bytes())
        self.assertEqual(corpus.sha(data),manifest['input_sha256']);self.assertEqual(excluded,manifest['excluded']);self.assertEqual(strata,manifest['strata'])
        for path,digest in manifest['source_sha256'].items():self.assertEqual(digest,corpus.sha((corpus.ROOT/path).read_bytes()),path)

if __name__=='__main__':unittest.main()
