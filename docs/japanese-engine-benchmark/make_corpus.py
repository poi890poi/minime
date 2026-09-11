"""Freeze source-derived actions before querying an IME. No production imports."""
from pathlib import Path
import collections, gzip, hashlib, json, re, sys, zipfile
from importlib.metadata import version

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
WORK = ROOT / 'artifacts/japanese-engine-benchmark'
OLD = ROOT / 'docs/language-contract-benchmark/conversations'
sys.path.insert(0, str(ROOT / 'artifacts/language-contract-benchmark/python-deps'))
import jaconv
from sudachipy import dictionary, tokenizer

def digest(value):
    return hashlib.sha256(value).hexdigest()

def order(value):
    return digest(('completion-v1:' + value).encode())

def dump(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')

def write_rows(path, rows):
    payload = ''.join(json.dumps(x, ensure_ascii=False) + '\n' for x in rows).encode()
    path.write_bytes(gzip.compress(payload, mtime=0))

def main():
    versions = {p: version(p) for p in ['sudachipy', 'sudachidict_core', 'jaconv']}
    assert versions == {'sudachipy': '0.6.10', 'sudachidict_core': '20250129', 'jaconv': '0.4.0'}
    tok = dictionary.Dictionary().create()
    old_plan = json.loads((OLD / 'split-plan.json').read_text(encoding='utf8'))
    old_text = {json.loads(s)['text'] for s in gzip.open(OLD / 'turns.jsonl.gz', 'rt', encoding='utf8')}
    plans, rows, archives, exclusions = {}, [], {}, collections.Counter()
    jp = r'[\u3041-\u3096\u30a1-\u30fa\u30fc\u3400-\u9fff々]+'
    for source in ['real-persona-chat', 'asdc']:
        archive = ROOT / 'artifacts/language-contract-benchmark/conversation-sources' / source / 'source.zip'
        archives[source] = digest(archive.read_bytes())
        expected = {'real-persona-chat': '2fb4fe5c16e6722a0840fe6924113965cffe0b6b9957e436fdb3886fae2743dc',
                    'asdc': '9dc1b4a7c8cd72b65b889f630f57a3b585ae6c963773b97720f6e2777b27171b'}
        assert archives[source] == expected[source], 'Source archive changed'
        z = zipfile.ZipFile(archive)
        part = '/real_persona_chat/dialogues/' if source == 'real-persona-chat' else '/data/main/dialog/json/'
        exposed = set(old_plan[source]['development'] + old_plan[source]['holdout'])
        names = [n for n in z.namelist() if part in n and n.endswith('.json')
                 and n not in exposed and Path(n).stem not in ['00001', '001']]
        names.sort(key=lambda n: order(source + ':' + Path(n).stem))
        assert len(names) >= 64
        plans[source] = {'development': names[:16], 'holdout': names[16:64]}
        for role, documents in plans[source].items():
            for name in documents:
                doc = source + ':' + Path(name).stem
                utterances = json.loads(z.read(name))['utterances']
                selected = sorted(range(len(utterances)), key=lambda i: order(doc + ':' + str(i)))[:8]
                for i in sorted(selected):
                    u = utterances[i]; text = u['text']; parts = []; cursor = 0
                    for m in re.finditer(jp, text):
                        if m.start() > cursor:
                            parts.append({'kind': 'literal', 'text': text[cursor:m.start()]})
                        tokens = tok.tokenize(m.group(), tokenizer.Tokenizer.SplitMode.C)
                        words = []
                        for t in tokens:
                            kana = jaconv.kata2hira(t.reading_form()); raw = jaconv.kana2alphabet(kana)
                            valid = not t.is_oov() and bool(re.fullmatch("[a-z'-]+", raw)) and jaconv.alphabet2kana(raw) == kana
                            if not valid:
                                exclusions[source + '/unreadable-token'] += 1
                            words.append({'text': t.surface(), 'kana': kana, 'raw': raw, 'valid': valid})
                        kana = ''.join(w['kana'] for w in words); raw = jaconv.kana2alphabet(kana)
                        valid = all(w['valid'] for w in words) and len(raw) <= 96 and jaconv.alphabet2kana(raw) == kana
                        parts.append({'kind': 'japanese', 'text': m.group(), 'kana': kana, 'raw': raw,
                                      'valid': valid, 'words': words,
                                      'reference': 'original-kana' if not re.search(r'[\u3400-\u9fff々]', m.group()) else 'sudachi-silver'})
                        cursor = m.end()
                    if cursor < len(text): parts.append({'kind': 'literal', 'text': text[cursor:]})
                    rows.append({'id': doc + ':' + str(i), 'source': source, 'role': role, 'document': doc,
                                 'source_member': name, 'source_document_sha256': digest(z.read(name)),
                                 'turn': i, 'speaker': u.get('interlocutor_id', u.get('name')),
                                 'text': text, 'previously_seen_text': text in old_text, 'parts': parts})
    # Identify exact repetitions across roles without removing common greetings.
    development_text = {r['text'] for r in rows if r['role'] == 'development'}
    for r in rows:
        r['cross_role_repeat'] = r['role'] == 'holdout' and r['text'] in development_text
    write_rows(HERE / 'utterances.jsonl.gz', rows)
    dump(HERE / 'split-plan.json', plans)
    aj_dir = next((WORK / 'ajimee').iterdir())
    aj_file = aj_dir / 'JWTD_v2/v1/evaluation_items.json'
    assert digest(aj_file.read_bytes()) == 'e9eb668fd6aa14b1e26436f429b5550108af0a1dfd443b8cea0bcb3ab3028fca'
    items = json.loads(aj_file.read_text(encoding='utf8'))
    write_rows(HERE / 'ajimee.jsonl.gz', items)
    (HERE / 'AJIMEE-README.md').write_bytes((aj_dir / 'README.md').read_bytes())
    manifest = {'versions': versions, 'sources': archives, 'utterances': len(rows),
                'counts': dict(collections.Counter(r['source'] + '/' + r['role'] for r in rows)),
                'previously_seen_text': sum(r['previously_seen_text'] for r in rows),
                'cross_role_repeat': sum(r['cross_role_repeat'] for r in rows),
                'exclusions': dict(exclusions), 'ajimee_items': len(items),
                'ajimee_sha256': digest(aj_file.read_bytes()),
                'files': {f: digest((HERE / f).read_bytes()) for f in ['utterances.jsonl.gz', 'ajimee.jsonl.gz', 'split-plan.json']}}
    dump(HERE / 'corpus-manifest.json', manifest)
    print(json.dumps(manifest, ensure_ascii=False))

if __name__ == '__main__': main()
