"""Validate shipped JMdict forms against commonness and reading/sense restrictions."""
from pathlib import Path
import json,tarfile,subprocess,hashlib
ROOT=Path(__file__).resolve().parent.parent
source=ROOT/'third_party/jmdict/jmdict-eng-common.json.tgz'
with tarfile.open(source) as archive:
    data=json.load(archive.extractfile(next(m for m in archive.getmembers() if m.name.endswith('.json'))))
words={w['id']:w for w in data['words']}
kana=sorted({k['text'] for w in words.values() for k in w['kana'] if k['common']})
aliases=json.loads(subprocess.check_output(['node',str(ROOT/'tools/romanize_kana.cjs')],input=json.dumps(kana,ensure_ascii=False).encode('utf8')))
allowed=set()
for w in words.values():
    for k in w['kana']:
        if not k['common']:continue
        senses=[s for s in w['sense'] if '*' in s['appliesToKana'] or k['text'] in s['appliesToKana']]
        if not senses:continue
        a=aliases[k['text']];outputs={k['text'],a['romaji'].replace(' ','').replace('・','')}
        for spelling in w['kanji']:
            if (spelling['common'] and ('*' in k['appliesToKanji'] or spelling['text'] in k['appliesToKanji'])
                and any('*' in s['appliesToKanji'] or spelling['text'] in s['appliesToKanji'] for s in senses)):
                outputs.add(spelling['text'])
        allowed.update((a['reading'],output,'jmdict:'+w['id']) for output in outputs)
rows=[]
for line in (ROOT/'app/src/main/assets/addons.tsv').read_text(encoding='utf8').splitlines():
    p=line.split('\t')
    if len(p)==5 and p[3].startswith('jmdict:'):
        assert p[0]=='japanese' and (p[1],p[2],p[3]) in allowed,'Unattested common reading/form: '+line
        rows.append(p)
old=set(subprocess.check_output(['git','show','fb85a39:app/src/main/assets/addons.tsv']).decode('utf8').splitlines())
new=set((ROOT/'app/src/main/assets/addons.tsv').read_text(encoding='utf8').splitlines())
assert old<=new,'Previous rows removed'
assert all(line.split('\t')[3].startswith('jmdict:') for line in new-old),'Unexpected new production source'
report=dict(source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),common_source_records=len(words),validated_rows=len(rows),validated_outputs=len({r[2] for r in rows}),new_rows=len(new-old),preserved_old_rows=len(old)-1,rule='Every row has a source-common kana reading, compatible common spelling and applicable sense. No gloss or inferred spelling. Corpus probes are evaluation only.')
(ROOT/'docs/japanese-coverage/source-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps(report,indent=2))
