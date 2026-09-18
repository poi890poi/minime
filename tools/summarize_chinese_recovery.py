"""Compare access, reference ranks and exact acceptance with explicit denominators."""
import argparse
import collections
import csv
import gzip
import hashlib
import json
from pathlib import Path


def load(path):
    return list(csv.DictReader(path.open(encoding='utf-8'), delimiter='\t'))


def inventory(row):
    return [(text, int(end)) for item in row['outputs'].split('|') if item for text, end in [item.rsplit(':', 1)]]


def stats(rows):
    labeled = [r for r in rows if r['target']]
    first = []
    for r in labeled:
        rank = next((i for i, (text, end) in enumerate(inventory(r), 1) if text == r['target'][0] and (end > 0 or len(r['target']) == 1)), 0)
        first.append(rank)
    return dict(episodes=len(rows), empty=sum(int(r['candidates']) == 0 for r in rows),
                with_prefix_glyph=sum(int(r['first_glyph_rank']) > 0 for r in rows),
                prefix_in_first8=sum(0 < int(r['first_glyph_rank']) <= 8 for r in rows),
                labeled_episodes=len(labeled), target_available=sum(int(r['target_rank']) > 0 for r in labeled),
                target_in_first8=sum(0 < int(r['target_rank']) <= 8 for r in labeled),
                target_on_space=sum(r['target'] == r['space'] for r in labeled),
                target_first_glyph_available=sum(rank > 0 for rank in first),
                target_first_glyph_in_first8=sum(0 < rank <= 8 for rank in first))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('before', type=Path)
    parser.add_argument('after', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    before, after = load(args.before), load(args.after)
    assert len(before) == len(after)
    groups = collections.defaultdict(lambda: ([], []))
    changed, lost = [], []
    for a, b in zip(before, after):
        assert all(a[k] == b[k] for k in ('genre', 'id', 'condition', 'raw', 'target'))
        length = int(a['glyphs'])
        bucket = str(length) if length <= 4 else '5+'
        for group in ('all', a['genre'], a['genre'] + '/' + a['condition'], 'length/' + bucket):
            groups[group][0].append(a); groups[group][1].append(b)
        if a['space'] != b['space']:
            changed.append(dict(raw=a['raw'], genre=a['genre'], before=a['space'], after=b['space'], target=a['target']))
        if int(a['target_rank']) > 0 and int(b['target_rank']) == 0:
            lost.append(dict(raw=a['raw'], genre=a['genre'], target=a['target']))
    report = dict(before_sha256=hashlib.sha256(args.before.read_bytes()).hexdigest(),
                  after_sha256=hashlib.sha256(args.after.read_bytes()).hexdigest(),
                  groups={g: {'before': stats(a), 'after': stats(b)} for g, (a, b) in sorted(groups.items())},
                  changed_space=changed, lost_targets=lost)
    for name, rows in [('before', before), ('after', after)]:
        timings = {r['raw']: int(r['lookup_ns']) / 1e6 for r in rows}
        times = sorted(timings.values())
        report[name + '_lookup_ms'] = dict(unique_queries=len(times), mean=sum(times)/len(times), p50=times[len(times)//2], p95=times[int(len(times)*.95)], maximum=times[-1])
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'all': report['groups']['all'], 'changed_space': len(changed), 'lost_targets': len(lost)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
