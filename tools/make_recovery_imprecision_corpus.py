"""Freeze existing imprecision queries as unlabeled behavioral regression data."""
import gzip, hashlib, json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root / 'docs/taiwan-quality/first-choice-inputs.tsv'
data = source.read_bytes()
rows = ['genre\tid\tcondition\traw\ttarget\tglyphs']
for i, line in enumerate(data.decode('utf-8').splitlines()):
    raw, conditions = line.split('\t')
    for condition in conditions.split(','):
        if condition in {'missing-letter', 'transposed-letters', 'adjacent-key'}:
            rows.append(f'imprecision-contract\t{i}\t{condition}\t{raw}\t\t0')
payload = ('\n'.join(rows)+'\n').encode('utf-8')
out = root / 'docs/chinese-recovery'
(out / 'imprecision.tsv.gz').write_bytes(gzip.compress(payload, mtime=0))
(out / 'imprecision-manifest.json').write_text(json.dumps(dict(
    source=str(source.relative_to(root)), source_sha256=hashlib.sha256(data).hexdigest(),
    sha256=hashlib.sha256(payload).hexdigest(), episodes=len(rows)-1,
    role='Previously evaluated queries. No targets: checks consumption and acceptance contracts, not correction accuracy.',
    limitations='Adjacent-key perturbations cover horizontal QWERTY neighbors only; not measured human touch distributions or joined KALQ.'
), indent=2)+'\n', encoding='utf-8')
print(len(rows)-1)
