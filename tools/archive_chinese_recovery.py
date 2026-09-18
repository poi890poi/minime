"""Keep compact review evidence, with full-output hashes for local reproduction."""
import argparse, csv, gzip, hashlib, io, json
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('inputs', nargs='+', type=Path)
p.add_argument('--output', type=Path, default=Path('docs/chinese-recovery/evidence'))
a = p.parse_args()
a.output.mkdir(parents=True, exist_ok=True)
manifest = {}
for path in a.inputs:
    buf = io.StringIO(newline='')
    writer = None
    count = 0
    with path.open(encoding='utf-8', newline='') as source:
        for row in csv.DictReader(source, delimiter='\t'):
            outputs = row.pop('outputs')
            values = [item.rsplit(':', 1) for item in outputs.split('|') if item]
            row['target_first_glyph_rank'] = next((i for i, (text, end) in enumerate(values, 1)
                if row['target'] and text == row['target'][0] and (int(end)>0 or len(row['target'])==1)), 0)
            row['first8'] = '|'.join(outputs.split('|')[:8])
            row['full_outputs_sha256'] = hashlib.sha256(outputs.encode('utf-8')).hexdigest()
            if writer is None:
                writer = csv.DictWriter(buf, fieldnames=list(row), delimiter='\t', lineterminator='\n')
                writer.writeheader()
            writer.writerow(row); count += 1
    data = gzip.compress(buf.getvalue().encode('utf-8'), mtime=0)
    name = path.name+'.gz'
    (a.output / name).write_bytes(data)
    manifest[name] = dict(rows=count, raw_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
        archive_sha256=hashlib.sha256(data).hexdigest(), bytes=len(data))
(a.output / 'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
print(json.dumps(dict(archives=len(manifest), bytes=sum(m['bytes'] for m in manifest.values()))))
