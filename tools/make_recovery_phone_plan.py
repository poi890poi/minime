"""Freeze a hash-selected reference comparison before any phone observation."""
import csv, gzip, hashlib, io, json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root / 'docs/chinese-recovery/corpus.tsv.gz'
rows = list(csv.DictReader(io.StringIO(gzip.decompress(source.read_bytes()).decode('utf-8')), delimiter='\t'))
seed = 'phone-recovery-20260918/'
selected = []
for genre, condition, count in [('authored-conversation-regression', None, 6),
                               ('essay-regression', 'full', 4),
                               ('essay-regression', 'initials', 4),
                               ('essay-regression', 'mixed', 4)]:
    choices = [r for r in rows if r['genre']==genre and (condition is None or r['condition']==condition)
               and 3<=len(r['target'])<=5 and len(r['raw'])<=22]
    choices.sort(key=lambda r: hashlib.sha256((seed+r['id']+r['raw']).encode()).digest())
    if len(choices)<count:
        raise SystemExit(f'Not enough rows for {genre}/{condition}: {len(choices)}')
    selected.extend(choices[:count])
plans = [dict(id=f'recovery-{i:02}', mode='pinyin', source=row,
    actions=[dict(type=row['raw'], label='typed', capture=True),
             dict(choose=row['target'][0], label='first-glyph', capture=True),
             dict(key='SPACE', label='remainder-accepted', capture=True)]) for i,row in enumerate(selected)]
out = root / 'docs/chinese-recovery/phone-plans'
out.mkdir(exist_ok=True)
for i in range(0, len(plans), 9):
    (out/f'batch-{i//9+1:02}.json').write_text(json.dumps(plans[i:i+9], ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
(out/'manifest.json').write_text(json.dumps(dict(seed=seed, corpus_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
    episodes=len(plans), status='Frozen, not executed; candidate unavailable is a recorded failure, not a passing comparison.'), indent=2)+'\n', encoding='utf-8')
print(len(plans))
