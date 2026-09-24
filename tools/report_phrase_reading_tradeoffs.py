"""Join all changed defaults/rank losses to external readings after lookup only."""
import collections
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'artifacts/phrase-reading-prior'


def table(path):
    with path.open(encoding='utf-8') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))


def main():
    refs_path = ROOT / 'artifacts/moe-reading-reference/references.json'
    refs = {k: set(v) for k, v in json.loads(refs_path.read_text(encoding='utf-8')).items()}
    known = set().union(*refs.values())
    detail, groups = [], collections.defaultdict(collections.Counter)
    for name in ('broad', 'chat'):
        before_path = ROOT / f'artifacts/reading-prior-followup/{name}-before.tsv'
        before, after = table(before_path), table(OUT / f'{name}.tsv')
        assert len(before) == len(after)
        for old, new in zip(before, after):
            assert all(old[k] == new[k] for k in ('id', 'genre', 'condition', 'raw', 'target'))
            target, raw = old['target'], old['raw']
            loss = 0 < int(old['target_rank']) <= 8 and not 0 < int(new['target_rank']) <= 8
            if loss:
                status = ('multi-glyph' if len(target) != 1 else 'glyph-uncovered' if target not in known
                          else 'complete-reading-supported' if target in refs.get(raw, ())
                          else 'partial-or-unsupported-complete')
                groups[name + '/first8-losses'][status] += 1
                groups[name + '/loss-conditions'][old['condition']] += 1
                detail.append(dict(kind='first8-loss', dataset=name, old=old, new=new,
                                   reference_status=status,
                                   target_reference_readings=sorted(k for k, v in refs.items() if target in v)))
    before = {r['raw']: r for r in table(ROOT / 'artifacts/moe-reading-reference/before.tsv')}
    after = {r['raw']: r for r in table(OUT / 'modern.tsv')}
    for raw in sorted(refs):
        old, new = before[raw], after[raw]
        supported_before, supported_after = old['space'] in refs[raw], new['space'] in refs[raw]
        if supported_before != supported_after:
            kind = 'modern-space-gain' if supported_after else 'modern-space-loss'
            detail.append(dict(kind=kind, raw=raw, before=old['space'], after=new['space']))
            groups['modern-space'][kind] += 1
    encoded = (json.dumps(detail, ensure_ascii=False, indent=2) + '\n').encode()
    (OUT / 'tradeoffs-local.json').write_bytes(encoded)
    report = dict(scope='Post-run pronunciation joins; incomplete input is not a complete-reading error. All losses retained locally. No label edits or estimator inputs.',
                  groups={k: dict(v) for k, v in groups.items()},
                  references_sha256=hashlib.sha256(refs_path.read_bytes()).hexdigest(),
                  local_detail_sha256=hashlib.sha256(encoded).hexdigest())
    (OUT / 'tradeoffs-summary.json').write_bytes((json.dumps(report, indent=2) + '\n').encode())
    print(json.dumps(report, indent=2))
    print(json.dumps([x for x in detail if x['kind'] == 'modern-space-loss'], ensure_ascii=False))


if __name__ == '__main__':
    main()
