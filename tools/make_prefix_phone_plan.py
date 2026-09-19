"""Reuse all 18 frozen reference cases; choose a source-returned target prefix.

This is an integration plan, not a language-accuracy holdout. Selection targets
never enter production decoding. Cases without a stored target phrase prefix
retain their original first-glyph action and remain in the phone comparison.
"""
import csv
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
original = root / 'docs/chinese-recovery/phone-plans'
output = root / 'docs/suggestion-coverage/phone-plans'
output.mkdir(exist_ok=True)
plans = [json.loads((original / f'batch-{i:02}.json').read_text(encoding='utf-8')) for i in (1, 2)]
queries = {case['source']['raw'] for plan in plans for case in plan}
rows = {}
with (root / 'artifacts/suggestion-coverage/pipeline-after.tsv').open(encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        if row['raw'] in queries:
            rows[row['raw']] = row
prefixes = 0
for i, plan in enumerate(plans, 1):
    for case in plan:
        raw, target = case['source']['raw'], case['source']['target']
        candidates = []
        for item in rows[raw]['outputs'].split('|'):
            text, consumed = item.rsplit(':', 1)
            if len(text) > 1 and 0 < int(consumed) < len(raw) and target.startswith(text):
                candidates.append((text, int(consumed)))
        if candidates:
            text, consumed = max(candidates, key=lambda pair: (len(pair[0]), pair[1]))
            case['actions'][1] = dict(choose=text, label='stored-prefix', capture=True)
            case['expected_prefix'] = dict(text=text, remaining=raw[consumed:].lstrip("'"))
            prefixes += 1
    (output / f'batch-{i:02}.json').write_bytes((json.dumps(plan, ensure_ascii=False, indent=2)+'\n').encode())
manifest = dict(cases=sum(map(len, plans)), stored_phrase_selections=prefixes,
                other_cases='Retain original first-glyph selection; none discarded.',
                scope='Repeated reference cases; integration and visible access, not held-out accuracy.',
                files={p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(output.glob('batch-*.json'))})
(output / 'manifest.json').write_bytes((json.dumps(manifest, indent=2)+'\n').encode())
integration = [dict(raw=case['source']['raw'], **case['expected_prefix'])
               for plan in plans for case in plan if 'expected_prefix' in case]
asset = root / 'app/src/androidTest/assets/stored-prefix-cases.json'
asset.write_bytes((json.dumps(integration, ensure_ascii=False, indent=2)+'\n').encode())
print(json.dumps(manifest))
