"""Keep compact row evidence in git; full candidate telemetry stays in artifacts."""
import csv
import gzip
import hashlib
import io
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
art = root / 'artifacts/suggestion-coverage'
out = root / 'docs/suggestion-coverage/evidence'
manifest = {}
for name in ('pipeline-before', 'pipeline-after', 'pipeline-unprotected'):
    source = art / (name+'.tsv')
    compact = io.StringIO(newline='')
    with source.open(encoding='utf-8') as f:
        rows = csv.DictReader(f, delimiter='\t')
        fields = [k for k in rows.fieldnames if k!='outputs']+['target_first_glyph_rank', 'phrase_prefixes']
        writer = csv.DictWriter(compact, fields, delimiter='\t', lineterminator='\n')
        writer.writeheader()
        count = 0
        for row in rows:
            values = [(text, int(end)) for item in row.pop('outputs').split('|') if item
                      for text, end in [item.rsplit(':', 1)]]
            row['target_first_glyph_rank'] = next((i for i, (text, end) in enumerate(values, 1)
                if row['target'] and text==row['target'][0] and (end>0 or len(row['target'])==1)), 0)
            row['phrase_prefixes'] = '|'.join(f'{text}:{end}' for text, end in values if len(text)>1 and 0<end<len(row['raw']))
            writer.writerow(row)
            count += 1
    target = out / (name+'-compact.tsv.gz')
    target.write_bytes(gzip.compress(compact.getvalue().encode(), mtime=0))
    manifest[name] = dict(rows=count, full_file=str(source.relative_to(root)),
                         full_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                         compact_sha256=hashlib.sha256(target.read_bytes()).hexdigest())
(out / 'pipeline-manifest.json').write_bytes((json.dumps(manifest, indent=2)+'\n').encode())
print(json.dumps({k:v['rows'] for k,v in manifest.items()}))
