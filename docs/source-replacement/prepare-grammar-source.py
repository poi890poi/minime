"""Isolate licensed grammar evidence without changing words or context counts."""
import hashlib
import importlib.util
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
OUT = ROOT / 'artifacts/source-audit/grammar-source'


def prepare():
    spec = importlib.util.spec_from_file_location('spacy_audit', HERE / 'spacy-contraction-audit.py')
    audit = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(audit)  # repository-owned bounded reader, not upstream code
    source = audit.CACHE / 'tokenizer_exceptions.py'
    notice = audit.CACHE / 'LICENSE'
    assert hashlib.sha256(source.read_bytes()).hexdigest() == audit.SOURCE_SHA
    assert hashlib.sha256(notice.read_bytes()).hexdigest() == audit.LICENSE_SHA
    forms, excluded, total, steps = audit.extract(source.read_text(encoding='utf-8'))
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
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'en_spelling.tsv').write_bytes(result)
    inputs = [source, notice, assets / 'en_us.tsv', assets / 'context.tsv', assets / 'en_spelling.tsv']
    manifest = dict(
        scope='Only grammatical flags; accepted lexical/frequency eligibility and bare-word bytes fixed',
        runtime='6bffd17', table_entries=total, interpreter_steps=steps, excluded=excluded,
        forms=len(forms), old_flags=len(old), new_flags=len(eligible),
        old_only=sorted(old-eligible), new_only=sorted(eligible-old),
        input_sha256={str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
        output_sha256=hashlib.sha256(result).hexdigest())
    (OUT / 'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k: manifest[k] for k in ('table_entries','forms','old_flags','new_flags','output_sha256')}))


if __name__ == '__main__':
    prepare()
