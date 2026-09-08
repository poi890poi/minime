"""Build deterministic offline assets from pinned, vendored upstream data (Python 3.7+)."""
import gzip
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from sources import require_sources
require_sources('mcbopomofo','aosp')

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / 'third_party' / 'mcbopomofo'
OUT = ROOT / 'app/src/main/assets'
OUT.mkdir(parents=True, exist_ok=True)
tones = str.maketrans('', '', 'ˊˇˋ˙ˉ')
def marked(s):
    return s if s[-1] in 'ˊˇˋ˙ˉ' else s + 'ˉ'
readings = {}
entries = {}
freq = {}
for line in (SRC / 'phrase.occ').read_text(encoding='utf-8').splitlines():
    parts = line.split()
    if len(parts) == 2:
        freq[parts[0]] = float(parts[1])
for line in (SRC / 'BPMFBase.txt').read_text(encoding='utf-8').splitlines():
    p = line.split()
    if len(p) < 3 or not re.fullmatch('[\u3105-\u3129ˊˇˋ˙ˉ]+', p[1]):
        continue
    py = re.sub('[1-5]', '', p[2]).lower().replace('ü', 'v').replace('u:', 'v')
    if not re.fullmatch('[a-zv]+', py):
        continue
    readings[p[1].translate(tones)] = py
    if re.fullmatch('[\u3400-\u9fff\U00020000-\U0002ffff]+', p[0]):
        entries[(p[0], p[1])] = (py, freq.get(p[0], 0.0), marked(p[1]))
skipped = 0
for line in (SRC / 'BPMFMappings.txt').read_text(encoding='utf-8').splitlines():
    p = line.split()
    if len(p) < 2 or not re.fullmatch('[\u3400-\u9fff\U00020000-\U0002ffff]+', p[0]):
        continue
    try:
        py = "'".join(readings[s.translate(tones)] for s in p[1:])
    except KeyError:
        skipped += 1
        continue
    entries[(p[0], ''.join(p[1:]))] = (py, freq.get(p[0], 0.0), ''.join(marked(s) for s in p[1:]))
with (OUT / 'zh_tw.tsv').open('w', encoding='utf-8', newline='\n') as f:
    for (word, bpmf), (py, count, explicit) in sorted(entries.items()):
        f.write('{}\t{}\t{}\t{}\t{}\n'.format(py, bpmf, word, count, explicit))
with (OUT / 'syllables.tsv').open('w', encoding='utf-8', newline='\n') as f:
    for bpmf, py in sorted(readings.items()):
        f.write(py + '\t' + bpmf + '\n')
english = {}
raw = gzip.decompress((ROOT / 'third_party/aosp/en_US_wordlist.combined.gz').read_bytes())
for line in raw.decode('utf-8').splitlines():
    m = re.match(r' word=([^,]+),f=(\d+)', line)
    if m and int(m[2]) >= 70 and re.fullmatch("[A-Za-z]+(?:'[A-Za-z]+)*", m[1]):
        english[m[1]] = int(m[2])
with (OUT / 'en_us.tsv').open('w', encoding='utf-8', newline='\n') as f:
    for word, count in sorted(english.items()):
        f.write('{}\t{}\n'.format(word, count))
subprocess.run([sys.executable, str(ROOT / 'tools/compile_english_spelling.py')], check=True)
report = {'chinese_readings': len(entries), 'chinese_labels': len({e[0] for e in entries}),
          'syllables': len(readings), 'skipped_unmapped_readings': skipped,
          'english_words': len(english), 'aosp_decompressed_sha256': hashlib.sha256(raw).hexdigest(),
          'assets': {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in OUT.glob('*.tsv')}}
(ROOT / 'docs/dictionary-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
