"""Archive reproducible prefix evidence without committing full candidate dumps."""
import csv
import gzip
import hashlib
import io
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
art = root / 'artifacts/prefix-access'
out = root / 'docs/prefix-access/evidence'
out.mkdir(exist_ok=True)
manifest = {}

for name in ('development-before', 'development-retention', 'development-preview',
             'validation-before', 'validation-after'):
    source = art / (name + '.tsv')
    data = source.read_bytes()
    target = out / (name + '.tsv.gz')
    target.write_bytes(gzip.compress(data, mtime=0))
    manifest[name] = dict(full_sha256=hashlib.sha256(data).hexdigest(),
                          archive_sha256=hashlib.sha256(target.read_bytes()).hexdigest())

source = art / 'composition-after.tsv'
compact = io.StringIO(newline='')
with source.open(encoding='utf-8') as stream:
    rows = csv.DictReader(stream, delimiter='\t')
    fields = [key for key in rows.fieldnames if key != 'outputs'] + ['target_first_glyph_rank', 'phrase_prefixes']
    writer = csv.DictWriter(compact, fields, delimiter='\t', lineterminator='\n')
    writer.writeheader()
    count = 0
    for row in rows:
        values = [(text, int(end)) for item in row.pop('outputs').split('|') if item
                  for text, end in [item.rsplit(':', 1)]]
        row['target_first_glyph_rank'] = next((i for i, (text, end) in enumerate(values, 1)
            if row['target'] and text == row['target'][0] and (end > 0 or len(row['target']) == 1)), 0)
        row['phrase_prefixes'] = '|'.join(f'{text}:{end}' for text, end in values if len(text) > 1 and 0 < end < len(row['raw']))
        writer.writerow(row)
        count += 1
target = out / 'composition-after-compact.tsv.gz'
target.write_bytes(gzip.compress(compact.getvalue().encode(), mtime=0))
manifest['composition-after'] = dict(rows=count, full_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                                    archive_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),
                                    baseline='docs/suggestion-coverage/evidence/pipeline-after-compact.tsv.gz')
(out / 'manifest.json').write_bytes((json.dumps(manifest, indent=2) + '\n').encode())
for name in ('core.log', 'desktop.log', 'retention-before-failure.log', 'development-retention.log', 'validation-after.log'):
    (out / name.replace('.log', '.txt')).write_bytes((art / name).read_bytes())
print(json.dumps(manifest))
