"""Compare frozen mode queries. Targets score saved outputs, never guide runtime."""
from pathlib import Path
from collections import defaultdict, Counter
import gzip, hashlib, json, statistics, math

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'docs/mode-priority'
RUN = ROOT / 'artifacts/mode-priority'

def records(name):
    with gzip.open(str(RUN / (name + '.jsonl.gz')), 'rt', encoding='utf8') as f:
        return {(x['row'], x['mode']): x for x in map(json.loads, f)}

def scores(record, target):
    matches = [i for i, c in enumerate(record['candidates']) if c[0] == target and c[2] == 0]
    return dict(n=1, optional3=int(any(1 <= i <= 3 for i in matches)),
                optional8=int(any(1 <= i <= 8 for i in matches)),
                any=int(bool(matches)), highlight=int(record['preferred'] in matches),
                raw=int(0 in matches))

def main():
    before, after = records('baseline'), records('candidate')
    assert before.keys() == after.keys()
    corpus = ROOT / 'docs/input-modes/coverage-inputs.tsv'
    inputs = [line.split('\t') for line in corpus.read_text(encoding='utf8').splitlines()]
    groups = defaultdict(lambda: {'before': Counter(), 'after': Counter()})
    unchanged = Counter(); changed = Counter(); inventories = Counter(); preferences = Counter()
    for key, b in after.items():
        a = before[key]; group, doc, identity, condition, raw, target, context = inputs[key[0]]
        mode = key[1]
        (unchanged if a == b else changed)[mode] += 1
        assert sorted(c[0] for c in a['candidates']) == sorted(c[0] for c in b['candidates']), 'Candidate inventory changed'
        inventories[mode] += 1
        preferences[mode] += int(a['candidates'][a['preferred']][0] != b['candidates'][b['preferred']][0])
        if mode in ('chinese', 'english'): assert a == b, 'Unaffected mode changed'
        g = groups[(group, condition, mode)]
        g['before'].update(scores(a, target)); g['after'].update(scores(b, target))
    report = dict(baseline='7fb0ae1', corpus_sha256=hashlib.sha256(corpus.read_bytes()).hexdigest(),
                  queries=len(inputs), mode_queries=len(after), unchanged=dict(unchanged),
                  changed=dict(changed), identical_candidate_inventories=dict(inventories),
                  changed_highlights=dict(preferences),
                  definition='Exact whole-input target in optional slots 1-3 / 1-8, excluding raw slot zero; raw availability and highlight reported separately. Ordinal positions are not physical row/page visibility. Exposed regression/source retrieval, not language accuracy.',
                  coverage=[dict(group=k[0], condition=k[1], mode=k[2], **{s:dict(v) for s,v in values.items()}) for k,values in sorted(groups.items())])
    timings = {}
    for stage in ('baseline', 'candidate'):
        path = RUN / (stage + '-ranking.tsv')
        if not path.exists(): continue
        by = defaultdict(list)
        for line in path.read_text(encoding='utf8').splitlines()[1:]:
            p = line.split('\t'); by[p[2]].append(int(p[-1]) / 1000)
        timings[stage] = {mode:dict(n=len(v), mean_us=statistics.mean(v), p50_us=statistics.median(v),
                                  p95_us=sorted(v)[math.ceil(len(v)*.95)-1], max_us=max(v)) for mode,v in sorted(by.items())}
    report['cached_ranking_timings'] = timings
    OUT.mkdir(exist_ok=True)
    (OUT / 'results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    for stage in ('baseline', 'candidate'):
        (OUT / (stage+'.jsonl.gz')).write_bytes((RUN / (stage+'.jsonl.gz')).read_bytes())
    print(json.dumps({k: report[k] for k in ('unchanged','changed','changed_highlights','cached_ranking_timings')}, indent=2))
    for row in report['coverage']:
        if ((row['group']=='poj-retrieval' and row['mode']=='taiwanese') or
            (row['group']=='japanese-retrieval' and row['mode']=='japanese') or
            (row['group'].startswith('en-') and row['mode'] in ('taiwanese','japanese') and row['condition']=='half') or
            (row['group']=='zh-essay' and row['mode'] in ('taiwanese','japanese') and row['condition']=='full')):
            print(row['group'], row['condition'], row['mode'], row['before'], row['after'])

if __name__ == '__main__': main()
