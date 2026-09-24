"""Source-defined grammar evidence; never generates production vocabulary."""
import ast
import importlib.util
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent


def source_suffixes(source):
    spec = importlib.util.spec_from_file_location('spacy_literal', HERE / 'spacy-contraction-audit.py')
    audit = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(audit)
    reader = audit.LiteralTable()
    reader.statements(ast.parse(source).body)
    suffixes = set()
    for surface, parts in reader.env['_exc'].items():
        if len(parts) != 2:
            continue
        left, right = (part['ORTH'] for part in parts)
        if not re.fullmatch('[A-Za-z]+', left) or not re.fullmatch("'[A-Za-z]+", right):
            continue
        if left + right != surface:
            raise ValueError('Attachment components disagree with source surface')
        suffixes.add(right.lower())
    return suffixes


def source_pronouns(snapshot):
    pages = {item['pageid']: item for item in snapshot['members'] if item['type'] == 'page'}
    accepted = set()
    for item in pages.values():
        word = item['title'].lower()
        if item['ns'] == 0 and re.fullmatch('[a-z]+', word):
            accepted.add(word)
    return accepted, len(pages)


def evidence(contractions, pronouns, suffixes):
    return set(contractions) | {pronoun + suffix for pronoun in pronouns for suffix in suffixes}
