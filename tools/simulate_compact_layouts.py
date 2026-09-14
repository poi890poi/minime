"""Frozen compact geometry extensions; no production imports or behavior changes."""
import argparse
import collections
import json
from pathlib import Path
import numpy as np
import simulate_english_layouts as pilot
from simulate_kalq_layout import geometry as split_geometry, BLOCKS

ORIGINAL = pilot.geometry
KALQ_ORDER = ''.join(''.join(block[r] if r < len(block) else '' for block in BLOCKS)
                     for r in range(4)).replace(' ', '')
QWERTY_ORDER = ''.join(pilot.ROWS['qwerty'])
NAMES = ['qwerty', 'kalq-30mm', 'kalq-joined', 'kalq-reflow',
         'compact-aligned', 'compact-staggered']


def packed_rows(order):
    return [order[:9], order[9:17], order[17:]]


def geometry(name, width):
    if name == 'qwerty':
        return ORIGINAL(name, width)
    if name == 'kalq-30mm':
        return split_geometry(name, width)
    result = np.zeros((26, 2))
    if name == 'kalq-joined':
        for side, block in enumerate(BLOCKS):
            for r, row in enumerate(block):
                for c, letter in enumerate(row):
                    if letter != ' ':
                        result[ord(letter)-97] = [(side*4+c+.5)*width/8, (r+.5)*7.5]
    else:
        order = KALQ_ORDER if name == 'kalq-reflow' else QWERTY_ORDER
        assert name in ('kalq-reflow', 'compact-aligned', 'compact-staggered')
        for r, row in enumerate(packed_rows(order)):
            offset = 0 if name == 'compact-aligned' else (9-len(row))/2
            for c, letter in enumerate(row):
                result[ord(letter)-97] = [(c+.5+offset)*width/9, (r+.5)*10]
    return result


def controls(name, width):
    points = [[(i+.5)*width/5, 35] for i in range(5)]
    if name in ('kalq-joined', 'kalq-30mm'):
        gap = 8 if name == 'kalq-30mm' else 0
        pitch = (width-gap)/8
        for side, block in enumerate(BLOCKS):
            for r, row in enumerate(block):
                for c, letter in enumerate(row):
                    if letter == ' ':
                        points.append([side*(4*pitch+gap)+(c+.5)*pitch, (r+.5)*7.5])
    return np.array(points)


def boundary_audit(out):
    _, docs, items, _, _ = pilot.load_inputs()
    grouped = collections.defaultdict(list)
    for word, doc in items:
        grouped[len(word)].append((word, doc))
    rows = []
    for width in (60, 70):
        for name in NAMES:
            letters = geometry(name, width)
            targets = np.concatenate([letters, controls(name, width)])
            assert len(np.unique(targets, axis=0)) == len(targets)
            assert np.array_equal(((targets[:, None]-targets[None, :])**2).sum(2).argmin(1), np.arange(len(targets)))
            for profile in pilot.PROFILES:
                for seed in pilot.SEEDS:
                    totals = collections.defaultdict(lambda: [0, 0, 0, 0, 0])
                    for length, records in grouped.items():
                        ids = pilot.word_ids([w for w, _ in records])
                        taps = pilot.contacts(letters[ids], width, profile, seed+length*1000)
                        hit = ((taps[:, :, None]-targets[None, None, :])**2).sum(3).argmin(2)
                        space = np.broadcast_to(targets[28], (len(records), 1, 2))
                        staps = pilot.contacts(space, width, profile, seed+length*1000+500000)
                        shit = ((staps[:, :, None]-targets[None, None, :])**2).sum(3).argmin(2)[:, 0]
                        for i, (_, doc) in enumerate(records):
                            x = totals[docs[doc]['genre']]
                            for j, v in enumerate([length, int((hit[i]>=26).sum()), 1,
                                                   int(shit[i]<26), int(shit[i]!=28)]):
                                x[j] += v
                    for genre, v in totals.items():
                        if profile == 'center':
                            assert v[1] == v[3] == v[4] == 0
                        rows.append(dict(layout=name,width_mm=width,profile=profile,seed=seed,genre=genre,
                                         letter_attempts=v[0],letters_hit_controls=v[1],space_attempts=v[2],
                                         spaces_hit_letters=v[3],space_errors=v[4]))
    (out/'controls.json').write_text(json.dumps(rows, indent=2)+'\n', encoding='utf-8')


def run(out):
    original = json.loads((pilot.ROOT/'docs/two-thumb-english/simulation/manifest.json').read_text())
    assert pilot.sha(Path(pilot.__file__)) == original['code_sha256']
    for path, expected in original['input_sha256'].items():
        assert pilot.sha(pilot.ROOT/path) == expected, path
    assert sorted(KALQ_ORDER) == sorted(QWERTY_ORDER) == list('abcdefghijklmnopqrstuvwxyz')
    pilot.ROWS = {name: (pilot.ROWS['qwerty'] if name=='qwerty' else [KALQ_ORDER if name.startswith('kalq') else QWERTY_ORDER]) for name in NAMES}
    pilot.geometry = geometry
    pilot.run(out)
    boundary_audit(out)
    p = out/'manifest.json'; m = json.loads(p.read_text())
    m.update(extension_code_sha256=pilot.sha(Path(__file__)),
             extension_plan_sha256=pilot.sha(pilot.ROOT/'docs/two-thumb-english/compact/PLAN.md'),
             dependency_sha256={'tools/simulate_kalq_layout.py':pilot.sha(pilot.ROOT/'tools/simulate_kalq_layout.py')},
             kalq_order=KALQ_ORDER,letter_height_mm=30,control_audit_total_height_mm=40,
             centers_mm={str(w):{n:geometry(n,w).tolist() for n in NAMES} for w in (60,70)})
    p.write_text(json.dumps(m,indent=2)+'\n',encoding='utf-8')
    print('PASS control clean hits; completed separate boundary audit',flush=True)


if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True)
    run(p.parse_args().out)
