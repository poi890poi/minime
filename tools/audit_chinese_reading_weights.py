"""Audit-only weight alternatives; never writes production assets.

Input: McBopomofo 3.1 Source/Data/data.txt built with its frequency_builder,
main_compiler and postprocess modules. See docs/chinese-recovery/README.md.
"""
import argparse, hashlib, json, math, re
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('upstream', type=Path)
p.add_argument('--output', type=Path, default=Path('artifacts/mcbopomofo-audit'))
a = p.parse_args()
source = Path('app/src/main/assets/zh_tw.tsv')
scores = {}
for line in a.upstream.read_text(encoding='utf-8').splitlines():
    fields = line.split()
    if len(fields) == 3 and not line.startswith('#'):
        scores[fields[0], fields[1]] = float(fields[2])
maxima = {}
for (reading, text), score in scores.items():
    maxima[text] = max(maxima.get(text, -math.inf), score)
full, priors = [], []
changed = missing = 0
for line in source.read_text(encoding='utf-8').splitlines():
    cells = line.split('\t')
    reading = re.sub(r'([ˉˊˇˋ˙])', r'\1-', cells[4]).rstrip('-').replace('ˉ', '')
    score = scores.get((reading, cells[2]))
    if score is None:
        missing += 1
        full.append(line); priors.append(line); continue
    weighted = cells.copy()
    weighted[3] = str(max(0.0, 10 ** (score + 9) - 1))
    full.append('\t'.join(weighted))
    delta = score - maxima[cells[2]]
    if len(cells[2]) == 1 and delta < 0:
        prior = float(cells[3])
        cells[3] = str(max(0, (prior+1) * 10 ** delta - 1))
        changed += float(cells[3]) != prior
    priors.append('\t'.join(cells))
if missing:
    raise SystemExit(f'Unmapped source rows: {missing}; do not silently change source coverage')
a.output.mkdir(parents=True, exist_ok=True)
manifest = dict(revision='e965b78296b1322d11ce672aaf626c5e65411881',
    role='Rejected audit variants, not production frequencies', rows=len(full),
    reading_prior_rows_changed=changed, missing=missing,
    source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
    upstream_sha256=hashlib.sha256(a.upstream.read_bytes()).hexdigest(), variants={})
for name, rows in [('upstream-weights', full), ('reading-priors', priors)]:
    data = ('\n'.join(rows)+'\n').encode('utf-8')
    (a.output / (name+'.tsv')).write_bytes(data)
    manifest['variants'][name] = hashlib.sha256(data).hexdigest()
(a.output / 'weights-manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
print(json.dumps(manifest))
