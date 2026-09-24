"""Evaluate already frozen grammar proposals on all eligible new documents."""
import collections
import gzip
import hashlib
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
OUT = ROOT / 'artifacts/source-audit/grammar-holdout'


def load_module(name, file):
    spec = importlib.util.spec_from_file_location(name, HERE / file)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    freeze_path = OUT / 'document-freeze.json'
    freeze = json.loads(freeze_path.read_text(encoding='utf-8'))
    parser = load_module('reference', 'audit-grammar-reference.py')
    prep = load_module('holdout', 'prepare-grammar-holdout.py')
    paths = {'accepted': ROOT / 'app/src/main/assets/en_spelling.tsv',
             'category': ROOT / 'artifacts/source-audit/wiktionary-grammar-source/en_spelling.tsv',
             'productive': ROOT / 'artifacts/source-audit/productive-grammar-source/en_spelling.tsv'}
    flags = {key: {l.split('\t')[1] for l in path.read_text(encoding='utf-8').splitlines()
                   if l.startswith('contraction\t')} for key, path in paths.items()}
    eligible = {r['document']: r for r in freeze['ledger'] if r['status'] == 'new-document-holdout'}
    rows = []
    for split in ('train', 'dev', 'test'):
        entry = freeze['source_files'][f'en_gum-ud-{split}.conllu']
        raw = gzip.decompress((OUT / entry['file']).read_bytes())
        if hashlib.sha256(raw).hexdigest() != entry['sha256']:
            raise ValueError('Holdout bytes changed after freeze')
        text = raw.decode('utf-8')
        for doc, body in prep.documents(text).items():
            if doc in eligible and hashlib.sha256(body.encode()).hexdigest() != eligible[doc]['text_sha256']:
                raise ValueError('Holdout document changed after freeze')
        parsed, _ = parser.references(text, 'new-gum-documents')
        rows.extend(r for r in parsed if r['document'] in eligible)
    grouped = collections.defaultdict(list)
    for row in rows:
        grouped['all/' + row['category']].append(row)
        grouped[row['genre'] + '/' + row['category']].append(row)
    groups = {}
    for group, members in sorted(grouped.items()):
        words = {r['word'] for r in members}
        groups[group] = dict(occurrences=len(members), unique_surfaces=len(words),
                            included={key: sum(r['word'] in values for r in members) for key, values in flags.items()},
                            included_unique={key: len(words & values) for key, values in flags.items()},
                            productive_gains=sum(r['word'] in flags['productive'] - flags['accepted'] for r in members),
                            productive_losses=sum(r['word'] in flags['accepted'] - flags['productive'] for r in members))
    manifest_path = ROOT / 'artifacts/source-audit/productive-grammar-source/manifest.json'
    manifest = json.loads(manifest_path.read_text())
    if hashlib.sha256(paths['productive'].read_bytes()).hexdigest() != manifest['output_sha256']:
        raise ValueError('Productive proposal changed after generation')
    report = dict(scope='Grammar-flag surface inclusion only; no runtime or typing-intent claim',
                  document_freeze_sha256=hashlib.sha256(freeze_path.read_bytes()).hexdigest(),
                  proposal_manifest_sha256=hashlib.sha256(manifest_path.read_bytes()).hexdigest(),
                  table_sha256={k: hashlib.sha256(p.read_bytes()).hexdigest() for k,p in paths.items()},
                  status_counts=freeze['status_counts'], eligible_genres=freeze['eligible_genres'], groups=groups)
    (OUT / 'grammar-reference-rows.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (OUT / 'grammar-reference-summary.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))


if __name__ == '__main__':
    main()
