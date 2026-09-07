"""Freeze a small disjoint touch-replay sample; never use candidate ranks as labels."""
import hashlib
import argparse
import json
import re
from pathlib import Path

source = Path('docs/conversation-ranking/corpus/inputs.tsv')
rows = [line.split('\t') for line in source.read_text(encoding='utf-8').splitlines()]
selected = []
for split in ('dev', 'test'):
    for group in ('en-' + split, 'zh-' + split + '-full', 'zh-' + split + '-initial',
                  'zh-' + split + '-mixed-left', 'zh-' + split + '-mixed-right'):
        pool = []
        for row in rows:
            if row[0] != group:
                continue
            words = row[2].split() if group.startswith('en-') else [row[2]]
            for word in words:
                if re.fullmatch('[a-z]{3,12}', word):
                    identity = group + ':' + row[1] + ':' + word
                    pool.append((hashlib.sha256(('human-input-v1:' + identity).encode()).hexdigest(), identity, word))
        used = {r['text'] for r in selected}
        count = 0
        for digest, identity, word in sorted(pool):
            if word in used:
                continue
            selected.append({'id': identity, 'split': split, 'mode': 'english' if group.startswith('en-') else 'pinyin', 'text': word})
            used.add(word)
            count += 1
            if count == 2:
                break
        assert count == 2, group
result = {'version': 1, 'seed': 20260908, 'source': str(source),
          'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest(), 'cases': selected}
target = Path('app/src/androidTest/assets/human-input.json')
serialized = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
parser = argparse.ArgumentParser()
parser.add_argument('--check', action='store_true')
if parser.parse_args().check:
    assert target.read_text(encoding='utf-8') == serialized, 'Frozen fixture differs from deterministic source selection'
else:
    target.write_text(serialized, encoding='utf-8')
print(str(target), len(selected), hashlib.sha256(target.read_bytes()).hexdigest())
