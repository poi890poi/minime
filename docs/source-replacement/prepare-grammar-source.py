"""Isolate licensed grammar evidence without changing words or context counts."""
import hashlib
import argparse
import importlib.util
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
OUT = ROOT / 'artifacts/source-audit/grammar-source'


def prepare(source_kind='spacy'):
    out = OUT if source_kind == 'spacy' else ROOT / ('artifacts/source-audit/'+source_kind+'-grammar-source')
    extra_inputs=[]
    if source_kind == 'spacy':
        spec = importlib.util.spec_from_file_location('spacy_audit', HERE / 'spacy-contraction-audit.py')
        audit = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(audit)  # repository-owned bounded reader, not upstream code
        source = audit.CACHE / 'tokenizer_exceptions.py'
        notice = audit.CACHE / 'LICENSE'
        assert hashlib.sha256(source.read_bytes()).hexdigest() == audit.SOURCE_SHA
        assert hashlib.sha256(notice.read_bytes()).hexdigest() == audit.LICENSE_SHA
        forms, excluded, total, steps = audit.extract(source.read_text(encoding='utf-8'))
        extra_inputs=[notice]
    elif source_kind == 'masc':
        source = ROOT / 'artifacts/source-audit/masc-contractions/manifest.json'
        data = json.loads(source.read_text(encoding='utf-8'))
        tagged = ROOT / 'artifacts/source-audit/masc/masc_tagged.zip'
        assert hashlib.sha256(tagged.read_bytes()).hexdigest() == data['tagged_package_sha256']
        for doc in data['documents']:
            original = ROOT / 'artifacts/source-audit/masc/publisher-texts' / doc['path'].removeprefix('metadata/FULL_MASC/')
            assert hashlib.sha256(original.read_bytes()).hexdigest() == doc['sha256']
        forms=data['inventory'];excluded=data['document_status'];total=len(data['documents']);steps=0
        extra_inputs=[tagged]
    elif source_kind in ('wiktionary', 'productive'):
        source=ROOT/'artifacts/source-audit/wiktionary-grammar/snapshot.json'
        raw=source.read_bytes()
        assert hashlib.sha256(raw).hexdigest()=='612f26f10ba6967c746168e94b7d29e07c6f1956d95ada0e7682240da213760b'
        data=json.loads(raw);pages={item['pageid']:item for item in data['members'] if item['type']=='page'}
        forms=set();excluded={};total=len(pages);steps=0
        for item in pages.values():
            word=item['title'].lower().replace('’',"'")
            if item['ns']==0 and re.fullmatch("[a-z]+(?:'[a-z]+)+",word):forms.add(word)
            else:excluded['outside-shape-or-mainspace']=excluded.get('outside-shape-or-mainspace',0)+1
        if source_kind == 'productive':
            spec=importlib.util.spec_from_file_location('productive',HERE/'productive-grammar.py')
            productive=importlib.util.module_from_spec(spec);spec.loader.exec_module(productive)
            pronoun_path=ROOT/'artifacts/source-audit/wiktionary-pronouns/snapshot.json'
            pronoun_bytes=pronoun_path.read_bytes()
            assert hashlib.sha256(pronoun_bytes).hexdigest()=='2d5bdf0595170391a33e877c793afa4606d90ec1071286ee4b9a6d6e0984a51a'
            pronouns,pronoun_pages=productive.source_pronouns(json.loads(pronoun_bytes))
            suffix_path=ROOT/'artifacts/source-audit/spacy/tokenizer_exceptions.py'
            suffix_bytes=suffix_path.read_bytes()
            assert hashlib.sha256(suffix_bytes).hexdigest()=='06767ba67f7dca70d6068cb63d8669b865d0f0645e541990fd5129ff40e14259'
            suffixes=productive.source_suffixes(suffix_bytes.decode('utf-8'))
            forms=productive.evidence(forms,pronouns,suffixes)
            excluded.update(pronoun_pages=pronoun_pages,pronouns=len(pronouns),suffixes=sorted(suffixes))
            extra_inputs=[pronoun_path,suffix_path,HERE/'productive-grammar.py']
    else:raise ValueError('Unsupported grammar source')
    assets = ROOT / 'app/src/main/assets'
    base = {line.split('\t')[0].lower() for line in (assets / 'en_us.tsv').read_text(encoding='utf-8').splitlines() if "'" in line.split('\t')[0]}
    unigrams = set()
    for line in (assets / 'context.tsv').read_text(encoding='utf-8').splitlines():
        lang, context, word, count = line.split('\t')
        if lang == 'en' and not context and re.fullmatch("[a-z]+'[a-z]+", word) and int(count) >= 2:
            unigrams.add(word)
    eligible = set(forms) & (base | unigrams)
    old_bytes = (assets / 'en_spelling.tsv').read_bytes()
    protected = b''.join(line for line in old_bytes.splitlines(keepends=True) if line.startswith(b'valid\t'))
    old = {line.split('\t')[1] for line in old_bytes.decode('utf-8').splitlines() if line.startswith('contraction\t')}
    result = protected + ''.join('contraction\t' + word + '\n' for word in sorted(eligible)).encode('utf-8')
    assert b''.join(line for line in result.splitlines(keepends=True) if line.startswith(b'valid\t')) == protected
    out.mkdir(parents=True, exist_ok=True)
    (out / 'en_spelling.tsv').write_bytes(result)
    inputs = [source, assets / 'en_us.tsv', assets / 'context.tsv', assets / 'en_spelling.tsv'] + extra_inputs
    manifest = dict(
        scope='Only grammatical flags; accepted lexical/frequency eligibility and bare-word bytes fixed',
        runtime='6bffd17', grammar_source=source_kind, table_entries=total, interpreter_steps=steps, excluded=excluded,
        forms=len(forms), old_flags=len(old), new_flags=len(eligible),
        old_only=sorted(old-eligible), new_only=sorted(eligible-old),
        input_sha256={str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
        output_sha256=hashlib.sha256(result).hexdigest())
    (out / 'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k: manifest[k] for k in ('table_entries','forms','old_flags','new_flags','output_sha256')}))


if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--source',choices=['spacy','masc','wiktionary','productive'],default='spacy')
    prepare(parser.parse_args().source)
