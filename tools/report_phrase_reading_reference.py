"""Post-run Google/default comparison; no reference values enter the estimator."""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'artifacts/phrase-reading-prior'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('session', type=Path)
    parser.add_argument('--verified-google-inputs', action='store_true',
                        help='Only after inspecting every typed screenshot')
    args = parser.parse_args()
    args.session = args.session.resolve()
    plan_path, observations_path = OUT / 'reference-plan.json', args.session / 'parity-observations.json'
    plan = json.loads(plan_path.read_text(encoding='utf-8'))
    manifest = json.loads((OUT / 'reference-manifest.json').read_text())
    assert hashlib.sha256(plan_path.read_bytes()).hexdigest() == manifest['plan_sha256']
    observations = json.loads(observations_path.read_text(encoding='utf-8'))
    assert len(observations) == 2 * len(plan)
    observed = {(r['provider'], r['id']): r for r in observations}
    assert len(observed) == len(observations)
    assert all(r['status'] == 'observed' for r in observations)
    def table(path):
        with path.open(encoding='utf-8') as stream:
            return {r['raw']: r for r in csv.DictReader(stream, delimiter='\t')}
    before_path, after_path = ROOT / 'artifacts/moe-reading-reference/before.tsv', OUT / 'modern.tsv'
    before, after = table(before_path), table(after_path)
    groups, details = collections.defaultdict(collections.Counter), []
    for case in plan:
        raw = case['source']['raw']
        accepted = {}
        for provider in ('google', 'minime'):
            steps = {s['stage']: s for s in observed[provider, case['id']]['steps']}
            assert steps['ready']['text'] == ''
            assert steps['accepted']['composingStart'] == -1
            accepted[provider] = steps['accepted']['text'].rstrip(' ')
            if provider == 'minime':
                assert steps['typed']['text'] == raw
        assert accepted['minime'] == before[raw]['space'], 'Phone/core baseline differs'
        old, new, google = accepted['minime'], after[raw]['space'], accepted['google']
        for group in ('all', case['source']['stratum']):
            c = groups[group]
            c['cases'] += 1
            c['baseline_matches_google_space'] += old == google
            c['trial_matches_google_space'] += new == google
            c['gains'] += old != google and new == google
            c['losses'] += old == google and new != google
        details.append(dict(raw=raw, stratum=case['source']['stratum'], before=old, after=new, google=google))
    report = dict(scope='Stratified 28-query diagnostic. Actual Google and accepted MinIME phone outputs, trial evaluated only in core. Not representative accuracy or trial APK validation.',
                  session=args.session.name, observations=len(observations),
                  minime_inputs_and_core_space_verified=len(plan),
                  google_inputs_visually_verified=len(plan) if args.verified_google_inputs else 0,
                  groups={k: dict(v) for k, v in groups.items()},
                  hashes={p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                          for p in (plan_path, observations_path, before_path, after_path)})
    (OUT / 'reference-detail-local.json').write_bytes((json.dumps(details, ensure_ascii=False, indent=2) + '\n').encode())
    (OUT / 'reference-summary.json').write_bytes((json.dumps(report, indent=2) + '\n').encode())
    print(json.dumps(report, indent=2))
    print(json.dumps(details, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
