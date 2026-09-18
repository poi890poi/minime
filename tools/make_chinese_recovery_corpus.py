"""Freeze decoder recovery probes separately from independently authored text targets."""
import gzip
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/chinese-recovery'
OUT.mkdir(exist_ok=True)
rows = []
sources = {}


def read(path):
    data = (ROOT / path).read_bytes()
    sources[path] = hashlib.sha256(data).hexdigest()
    return data


syllables = sorted({s.split('\t')[0] for s in read('app/src/main/assets/syllables.tsv').decode().splitlines()})
for i, first in enumerate(syllables):
    for length in (3, 4, 6, 10):
        units = [first] + [syllables[(i * 37 + j * 71) % len(syllables)] for j in range(1, length)]
        for condition, raw in [('full', ''.join(units)), ('initials', ''.join(s[0] for s in units)),
                               ('mixed', ''.join(s if j % 2 == 0 else s[0] for j, s in enumerate(units))),
                               ('separated', "'".join(units))]:
            rows.append(['mechanical', f'{i}-{length}', condition, raw, '', str(length)])
for raw in read('docs/glyph-ranking/inputs.txt').decode().splitlines():
    rows.append(['glyph-audit', raw, 'single', raw, '', '1'])
for line in read('docs/conversation-ranking/corpus/inputs.tsv').decode().splitlines():
    cells = line.split('\t')
    if cells[0].startswith('zh-'):
        rows.append(['encyclopedic-regression', cells[1], cells[0], cells[2], cells[3], str(len(cells[3]))])
for item in json.loads(gzip.decompress(read('docs/construction-confidence/inputs.json.gz'))):
    rows.append(['essay-regression', item['document'] + ':' + item['id'], item['condition'], item['raw'], item['target'], str(len(item['target']))])
for line in read('docs/pinyin-fresh-holdout.tsv').decode().splitlines():
    cells = line.split('\t')
    rows.append(['authored-conversation-regression', cells[0], 'authored', cells[2], cells[3], str(len(cells[3]))])
entries = []
for line in read('app/src/main/assets/zh_tw.tsv').decode().splitlines():
    p = line.split('\t')
    if len(p[2]) >= 3 and "'" in p[0]:
        entries.append((p[0], p[2]))
entries = sorted(set(entries), key=lambda p: hashlib.sha256(('recovery-20260918/' + repr(p)).encode()).digest())[:5000]
for reading, target in entries:
    rows.append(['source-retrieval', reading, 'full', reading.replace("'", ''), target, str(len(target))])
payload = ('genre\tid\tcondition\traw\ttarget\tglyphs\n' + '\n'.join('\t'.join(r) for r in rows) + '\n').encode()
(OUT / 'corpus.tsv.gz').write_bytes(gzip.compress(payload, mtime=0))
manifest = dict(seed='recovery-20260918', rows=len(rows), unique_inputs=len({r[3] for r in rows}),
                sources=sources, sha256=hashlib.sha256(payload).hexdigest(),
                roles={'mechanical': 'all syllables, source-derived arbitrary chains, recovery contracts only; no phrase correctness labels',
                       'source-retrieval': '5000 hash-selected stored phrases >=3 glyphs; seen-source retrieval, not generalization',
                       'glyph-audit': 'all prior 499 inputs; MOE/UD used only after decoding',
                       'essay-regression': 'previously evaluated Taiwan.md prose, not fresh holdout',
                       'encyclopedic-regression': 'previously evaluated Chinese GSD and other frozen rows, not natural conversation',
                       'authored-conversation-regression': 'previously inspected author-written conversation scenarios; not population accuracy'},
                gates=['No previously present full target lost', 'No whole-token Space replaced by partial output',
                       'Source-owned first-glyph candidates remain reachable for multi-syllable inputs',
                       'Explicit prefix tap consumes only its stated raw span', 'No generated Han sequences',
                       'Separate results by genre, spelling condition and glyph length',
                       'Record input availability, target ranks, lookup latency and remaining gaps; do not infer Google parity'])
(OUT / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({k: manifest[k] for k in ('rows', 'unique_inputs', 'sha256')}))
