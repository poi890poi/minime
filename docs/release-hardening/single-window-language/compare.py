"""Audit all eight paired replays; export aggregates, never input text."""
import csv
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
HERE = ROOT / 'artifacts/single-window-language'
sys.path.insert(0, str(ROOT / 'tools'))
from report_touch_latency import measurements, summary

ORDER = ['chinese-old', 'chinese-new', 'taiwanese-new', 'taiwanese-old',
         'japanese-old', 'japanese-new', 'english-new', 'english-old']
LIMITS = {'editor_submission': (33, 50), 'space_submission': (33, 50),
          'candidate_submission': (50, 80)}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def identity(row):
    return tuple(row[k] for k in ['mode', 'interval_ms', 'query_id', 'source',
                                  'genre', 'condition', 'action', 'expected'])


def load(tag):
    result = json.loads((HERE / 'reports' / (tag + '.json')).read_text(encoding='utf-8'))
    for name, digest in result['inputs'].items():
        if sha(ROOT / name) != digest:
            raise ValueError('Changed evidence: ' + name)
    log = (HERE / (tag + '.log')).read_text(encoding='utf-8-sig')
    for receipt in ['OK (1 test)', 'Cleanup verified: preferences, previous IME, display OFF.',
                    'Final display OFF verified after environment collection']:
        if receipt not in log:
            raise ValueError('Missing cleanup/test receipt: ' + tag)
    raw = ROOT / 'artifacts/device-tests' / result['session'] / 'touch-latency.tsv'
    with raw.open(encoding='utf-8-sig') as stream:
        rows = list(csv.DictReader(stream, delimiter='\t'))
    env = ROOT / 'artifacts/release-hardening' / ('single-window-language-' + tag + '-environment')
    status = int(re.search(r'Thermal Status: (\d+)', (env / 'before-thermal.txt').read_text(encoding='utf-8-sig'))[1])
    temperature = int(re.search(r'temperature: (\d+)', (env / 'before-battery.txt').read_text(encoding='utf-8-sig'))[1]) / 10
    if status != 0 or temperature >= 34:
        raise ValueError('Not a cooled start: ' + tag)
    display = (env / 'final-display.txt').read_text(encoding='utf-8-sig')
    if not re.search(r'Display Id=0\s*\n\s*Display State=OFF\s*\n', display):
        raise ValueError('Built-in display OFF not recorded: ' + tag)
    receipt = dict(session=result['session'], actions=len(rows), thermal_status=status,
                   battery_c=temperature, restored=True, display_off=True,
                   evidence_hashes={str(p.relative_to(ROOT)): sha(p) for p in
                                    [env / 'before-time.txt', env / 'before-thermal.txt',
                                     env / 'before-battery.txt', env / 'final-display.txt']})
    return result, rows, receipt


def compare(old, new):
    if old.keys() != new.keys():
        raise ValueError('Different groups')
    comparisons = {}
    for key in old:
        item = {}
        for metric, (p95, p99) in LIMITS.items():
            a, b = old[key][metric], new[key][metric]
            if a['n'] != b['n']:
                raise ValueError('Different denominators')
            item[metric] = dict(old=a, new=b,
                delta_ms={p: b[p] - a[p] for p in ['mean_ms', 'p50_ms', 'p95_ms', 'p99_ms', 'max_ms']},
                new_observed_tail_within_budget=b['p95_ms'] <= p95 and b['p99_ms'] <= p99,
                limits_ms=dict(p95=p95, p99=p99))
        item['candidate_deadline'] = dict(old=old[key]['candidate_deadline'], new=new[key]['candidate_deadline'])
        comparisons[key] = item
    return comparisons


def by_genre(rows):
    adjacent = list(zip(rows, rows[1:]))
    keys = list(dict.fromkeys((r['mode'], r['source'], r['genre'], r['interval_ms']) for r in rows))
    return {'/'.join(key): measurements([r for r in rows if
            (r['mode'], r['source'], r['genre'], r['interval_ms']) == key], adjacent) for key in keys}


def paired_observations(old, new):
    result = {}
    for key in dict.fromkeys((r['mode'], r['interval_ms']) for r in old):
        pairs = [(a, b) for a, b in zip(old, new) if a['action'] == 'key' and
                 (a['mode'], a['interval_ms']) == key]
        both = [(a, b) for a, b in pairs if int(a['candidate_submit_ns']) and int(b['candidate_submit_ns'])]
        result['/'.join(key)] = dict(letters=len(pairs), both_observed=len(both),
            old_only=sum(bool(int(a['candidate_submit_ns'])) and not int(b['candidate_submit_ns']) for a, b in pairs),
            new_only=sum(bool(int(b['candidate_submit_ns'])) and not int(a['candidate_submit_ns']) for a, b in pairs),
            neither=sum(not int(a['candidate_submit_ns']) and not int(b['candidate_submit_ns']) for a, b in pairs),
            shared_old=summary([a for a, b in both], 'candidate_submit_ns'),
            shared_new=summary([b for a, b in both], 'candidate_submit_ns'))
    return result


def main():
    loaded = {tag: load(tag) for tag in ORDER}
    if len({loaded[tag][2]['session'] for tag in ORDER}) != len(ORDER):
        raise ValueError('A session was reused')
    output = dict(order=ORDER, sessions={tag: loaded[tag][2] for tag in ORDER}, pairs={}, outliers=[])
    for language in ['chinese', 'taiwanese', 'japanese', 'english']:
        a, ar, _ = loaded[language + '-old']
        b, br, _ = loaded[language + '-new']
        if [identity(r) for r in ar] != [identity(r) for r in br]:
            raise ValueError('Different paired action sequence: ' + language)
        if a['raw_space_missing'] or b['raw_space_missing']:
            raise ValueError('Missing raw/Space frame: ' + language)
        output['pairs'][language] = dict(groups=compare(a['groups'], b['groups']),
            strata=compare(a['strata'], b['strata']), genres=compare(by_genre(ar), by_genre(br)),
            paired_candidate_observations=paired_observations(ar, br))
        for tag, rows, counterpart in [(language + '-old', ar, br), (language + '-new', br, ar)]:
            for index, row in enumerate(rows):
                for metric, endpoint in [('raw_or_space', 'editor_submit_ns'), ('candidate', 'candidate_submit_ns')]:
                    value = int(row[endpoint])
                    delay = (value - int(row['up_ns'])) / 1e6
                    if value and delay > 100:
                        other = counterpart[index]
                        output['outliers'].append(dict(tag=tag, action_index=index,
                            query_id=row['query_id'], source=row['source'], genre=row['genre'], condition=row['condition'],
                            interval_ms=int(row['interval_ms']), action=row['action'], typed_length=len(row['expected']),
                            metric=metric, delay_ms=delay, counterpart_ms=
                            (int(other[endpoint]) - int(other['up_ns'])) / 1e6 if int(other[endpoint]) else None))
    output['actions'] = sum(x['actions'] for x in output['sessions'].values())
    output['limitations'] = ['One pair per language; not repeated per-stratum evidence.',
        'Previously exposed development corpus, not a fresh holdout.',
        'Injected key-release to callback timing, not physical touch or screen presentation.',
        'Candidate latency is conditional on a matching observed frame; all denominators retained.',
        'No stage hooks: outlier counterpart comparison cannot identify a causal stage.']
    (HERE / 'comparison.json').write_text(json.dumps(output, indent=2) + '\n', encoding='utf-8')
    print('Validated', len(loaded), 'sessions;', output['actions'], 'actions;', len(output['outliers']), 'outliers')
    for language, pair in output['pairs'].items():
        for key, metrics in pair['groups'].items():
            candidate = metrics['candidate_submission']
            print(key, 'candidate p95', round(candidate['old']['p95_ms'], 2), '->', round(candidate['new']['p95_ms'], 2),
                  'observed tail budgets', {m: metrics[m]['new_observed_tail_within_budget'] for m in LIMITS})


if __name__ == '__main__':
    main()
