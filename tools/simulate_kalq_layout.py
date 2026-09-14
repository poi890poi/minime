"""KALQ geometry extension; reuse the frozen pilot's decoder, inputs and noise."""
import argparse
import json
from pathlib import Path
import numpy as np
import simulate_english_layouts as pilot

BLOCKS = [['mbwh', 'p xc', 'rysz', 'dnfv'], ['gtoj', 'ie u', 'kalq']]
VARIANTS = {'kalq-30mm': 30, 'kalq-40mm': 40}
ORIGINAL_GEOMETRY = pilot.geometry


def geometry(name, width):
    if name not in VARIANTS:
        return ORIGINAL_GEOMETRY(name, width)
    centers = np.zeros((26, 2))
    seen = []
    pitch = (width - 8) / 8
    for side, block in enumerate(BLOCKS):
        for row, text in enumerate(block):
            for col, letter in enumerate(text):
                if letter == ' ':
                    continue
                centers[ord(letter)-97] = [side*(4*pitch+8)+(col+.5)*pitch,
                                          (row+.5)*VARIANTS[name]/4]
                seen.append(letter)
    assert sorted(seen) == list('abcdefghijklmnopqrstuvwxyz')
    assert np.all(centers[: ,0] > 0) and np.all(centers[:, 0] < width)
    return centers


def run(out):
    original = json.loads((pilot.ROOT/'docs/two-thumb-english/simulation/manifest.json').read_text())
    assert pilot.sha(Path(pilot.__file__)) == original['code_sha256'], 'Original harness changed'
    for relative, expected in original['input_sha256'].items():
        assert pilot.sha(pilot.ROOT/relative) == expected, relative
    assert pilot.sha(pilot.ROOT/'docs/two-thumb-english/simulation/PLAN.md') == original['plan_sha256']
    for width in (60,70):
        assert np.array_equal(geometry('qwerty',width), ORIGINAL_GEOMETRY('qwerty',width))
    pilot.ROWS = {'qwerty': pilot.ROWS['qwerty'],
                  **{name: [''.join(''.join(b) for b in BLOCKS).replace(' ', '')] for name in VARIANTS}}
    pilot.geometry = geometry
    # The original control explicitly names QWERTY; do not change its definition.
    pilot.run(out)
    p = out/'manifest.json'; manifest=json.loads(p.read_text())
    manifest.update(extension_code_sha256=pilot.sha(Path(__file__)),
                    extension_plan_sha256=pilot.sha(pilot.ROOT/'docs/two-thumb-english/kalq/PLAN.md'),
                    blocks=BLOCKS, heights_mm=VARIANTS, gap_mm=8,
                    published_layout_source='https://www.pokristensson.com/pubs/OulasvirtaEtAlCHI2013.pdf',
                    source_figure=1, source_pdf_sha256=pilot.sha(pilot.ROOT/'artifacts/kalq-reference/paper.pdf'),
                    geometry_rule='Four columns each block, fixed 8 mm gap, top-aligned blocks; holes retained; no within-block row stagger.',
                    omitted_behavior='Internal space-key activation and word segmentation, motor learning, movement time and physical grip are not simulated.')
    p.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True)
    run(p.parse_args().out)
