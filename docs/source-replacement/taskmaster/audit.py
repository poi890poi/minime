"""Offline structural audit of the pinned Taskmaster pilot; never emits model data."""
import collections
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CACHE = ROOT / 'artifacts/source-audit/taskmaster'
OUT = Path(__file__).resolve().parent
PIN = 'd92cb6af3005f1dc09c39e75e7daf4a04905e00b'
BASE = f'https://raw.githubusercontent.com/google-research-datasets/Taskmaster/{PIN}/TM-1-2019/'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    paths = {'self-dialogs.json': CACHE/'self-dialogs.json'}
    paths.update({f'train-dev-test/{s}.csv': CACHE/f'{s}.csv' for s in ('train', 'dev', 'test')})
    paths['README.md'] = OUT/'UPSTREAM-README.md'
    hashes = {name: digest(path) for name, path in paths.items()}
    manifest = OUT/'manifest.json'
    if manifest.exists():
        assert json.loads(manifest.read_text(encoding='utf-8'))['sha256'] == hashes, 'Pinned audit bytes changed'
    data = json.loads(paths['self-dialogs.json'].read_text(encoding='utf-8'))
    by_id = {d['conversation_id']: d for d in data}
    assert len(by_id) == len(data), 'Duplicate conversation IDs'
    splits = {}
    for split in ('train', 'dev', 'test'):
        with (CACHE/f'{split}.csv').open(encoding='utf-8') as source:
            ids = [row[0] for row in csv.reader(source) if row and row[0]]
        assert len(ids) == len(set(ids)), f'Duplicate IDs in {split}'
        splits[split] = set(ids)
        assert splits[split] <= by_id.keys(), f'Unknown IDs in {split}'
    assert not any(splits[a] & splits[b] for a,b in [('train','dev'),('train','test'),('dev','test')]), 'Split overlap'
    assert set.union(*splits.values()) == by_id.keys(), 'Split inventory does not cover file'
    stats = {}
    signatures = {}
    for split, ids in splits.items():
        domains = collections.Counter(); speakers = collections.Counter(); utterances = 0; apostrophes = 0; empty = 0
        signatures[split] = set()
        for identity in sorted(ids):
            dialog = by_id[identity]; turns = dialog['utterances']
            domains[dialog['instruction_id']] += 1
            assert turns, f'Empty conversation {identity}'
            assert [t['index'] for t in turns] == list(range(len(turns))), f'Unexpected turn indexes {identity}'
            signature = []
            for turn in turns:
                text = turn['text']; assert isinstance(text, str)
                speakers[turn['speaker']] += 1; utterances += 1
                empty += not text.strip(); apostrophes += ("'" in text or '’' in text)
                signature.append((turn['speaker'], ' '.join(text.lower().split())))
            signatures[split].add(hashlib.sha256(json.dumps(signature,ensure_ascii=False).encode()).hexdigest())
        stats[split] = dict(conversations=len(ids),utterances=utterances,empty_utterances=empty,
                           utterances_with_apostrophe=apostrophes,speakers=dict(speakers),instruction_counts=dict(sorted(domains.items())),
                           distinct_dialogue_texts=len(signatures[split]))
    overlaps = {a+'/'+b:len(signatures[a]&signatures[b]) for a,b in [('train','dev'),('train','test'),('dev','test')]}
    result = dict(revision=PIN,urls={n:BASE+n for n in paths},sha256=hashes,split_stats=stats,
                  exact_dialogue_text_overlap=overlaps,production_changed=False,
                  exposure='Structural inventory only; no model queries or example selection. Official conversation splits retained.',
                  limitations=['Prompted self-dialogues, not spontaneous two-person conversations',
                               'Task domains and repeated instructions are not broad English frequency evidence',
                               'Identical short turns may recur across otherwise distinct dialogues'])
    manifest.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({s:{k:v for k,v in data.items() if k!='instruction_counts'} for s,data in stats.items()},indent=2))
    print('Exact dialogue text overlap:', overlaps)


if __name__ == '__main__':
    main()
