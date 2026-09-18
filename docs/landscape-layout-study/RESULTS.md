# Landscape layout assessment

2026-09-18. Recommendation: **retain compact and trial a taller split as an
option**. A same-height split helps reach but does not solve short targets.
Reject full-height side overlays as the default because they obscure the app.
No production default changed in this study.

Follow-up: [native phone assessment](DEVICE-RESULTS.md) and screenshots now
cover all four layouts on RFCR91GWXLX. Split layouts remain debug-only prototypes.

Open `index.html` through a local server to try every layout, screen size, capital
slide, symbol slide and punctuation mapping. These are working browser prototypes,
not Android screenshots or an installed IME. Geometry comes from the same JSON
rectangles used by the evaluator. The full-height design deliberately overlays
the original app instead of inventing an app that reflows into the middle gap.

## Comparison at 760 × 360 dp

| Design | Letter target | Clear editor height above | Mean inward reach | Correct letters, 6 dp scatter | Correct letters, 10 dp scatter |
|---|---:|---:|---:|---:|---:|
| Compact joined | 95 × 25.5 dp | 176 dp | 233.0 dp | 680,852 / 704,640 = 96.62% | 562,017 / 704,640 = 79.76% |
| Split, same height | 60 × 25.5 dp | 176 dp | 147.1 dp | 680,851 / 704,640 = 96.62% | 560,526 / 704,640 = 79.55% |
| Split, taller keys | 60 × 44 dp | 96 dp | 147.1 dp | 704,459 / 704,640 = 99.97% | 683,013 / 704,640 = 96.93% |
| Full-height sides | 48 × 68 dp | 0 dp | 117.7 dp | 704,591 / 704,640 = 99.99% | 692,697 / 704,640 = 98.31% |

**Percentage definition:** intended letter taps landing on the correct letter /
all intended letter taps, before correction. The denominators combine three
matched random seeds, not independent people. Scatter is assumed Gaussian noise
with the stated standard deviation in dp on both axes. These numbers are **not
measured human hit rates**. Perfect center contacts pass for all layouts.

Inward reach is corpus-weighted horizontal distance from the nearer screen edge.
It is not a thumb-range measurement. The taller split reduces that distance by
85.8 dp relative to joined KALQ, while mean within-thumb geometric movement is
93.1 dp versus 116.7 dp. The same-height split has 79.2 dp movement; full-height
sides increase it to 103.8 dp because rows become much taller. None of these
quantities establishes speed, comfort, learning time or fatigue.

At 10 dp scatter, the taller split's 21,627 wrong letter actions consist of
14,154 other-letter substitutions, 2,841 accidental Space actions, 4,125 control
activations and 507 misses. Errors are not snapped back to the nearest letter.
Separately, Space succeeds in 55,340 / 57,348 attempts for taller split versus
47,181 / 57,348 for compact. Space uses a declared idealized nearest-target policy;
real users may choose different internal/footer Space keys.

## Across sizes and text genres

The ranking is stable over the frozen sizes under 10 dp scatter:

| Viewport | Compact | Same-height split | Taller split | Full-height sides | Taller split clear editor height |
|---|---:|---:|---:|---:|---:|
| 640 × 320 dp | 79.75% | 79.55% | 96.93% | 98.00% | 56 dp |
| 760 × 360 dp | 79.76% | 79.55% | 96.93% | 98.31% | 96 dp |
| 880 × 400 dp | 79.76% | 79.55% | 96.93% | 98.36% | 136 dp |

Each percentage uses 704,640 intended letter attempts. The fixed split block
width explains why bottom-split results remain constant across screen widths.
The smaller screen exposes a serious cost: 56 dp above the tall board is barely
enough for app chrome and one text line. Retaining a compact option is necessary.

Separate genre results at 760 × 360 dp / 10 dp scatter:

| Genre | Intended letter attempts | Compact | Same-height split | Taller split | Full-height sides |
|---|---:|---:|---:|---:|---:|
| GUM conversation | 16,155 | 79.82% | 79.62% | 97.23% | 98.33% |
| GUM essay | 14,955 | 79.32% | 79.05% | 96.80% | 98.00% |
| Taiwan.md essay, full Pinyin | 149,373 | 79.87% | 79.64% | 96.92% | 98.27% |
| Authored Chinese conversation spelling | 594 | 82.83% | 82.83% | 97.14% | 98.32% |

All remaining genres, partial spellings, profiles and per-seed counts are retained
in `results.json.gz`. For the main 760 dp comparison, per-seed 10 dp hit ranges
are 79.68–79.84% compact, 79.46–79.63% short split, 96.90–96.96% taller split,
and 98.29–98.32% sides. These are seed ranges, not human confidence intervals.

## What was actually tested

- PASS: 12 disjoint key geometries and exact-center identity for letters/Space.
- PASS: frozen source/code/plan hashes; 3,780 report records have complete error
  partitions. Metrics include dead space and controls.
- PASS: browser input of all 26 letters, Space, Shift and a symbol in each of
  the four prototypes. Observed output `abcdefghijklmnopqrstuvwxyz A@` each time.
- PASS: actual browser pointer drags on the taller split produce `1` when sliding
  down on M, then `M` when sliding up.
- PASS: browser label containment at 24 layout/viewport/punctuation combinations.
  Initial checking caught line boxes overflowing the short compact rows; setting
  the prototype's key line height to one fixed the cause, after which all passed.
- Visually inspected taller split and full-height overlays. The latter visibly
  hides Notes/Done controls and the beginning of the text lines.
- Not measured in this initial offline study: phone touch accuracy, multitouch,
  gesture false activation, physical latency, Android insets/rotation, app
  compatibility and real thumb reach. The separate native follow-up documents
  which Android checks have since passed and which human measures remain unknown.

The frozen corpus has 12,529 text/spelling rows, 234,880 letter contacts and
19,116 Space contacts per seed. It reuses all GUM test genres and existing Chinese
essay/conversation recovery data. Unsupported punctuation and nonletters are
counted as exclusions in `manifest.json`; no symbol-gesture accuracy is inferred
from the tap simulation. No dictionary or autocorrection is used. Corpus exposure,
assumed independent scatter and idealized thumb/Space choices prevent a claim of
real-world typing improvement. Browser checks do not replace Android tests.

## Decisions

1. **Compact:** retain for small landscape windows and tasks needing editor space.
2. **Same-height split:** optional reach variant only; reject it as a remedy for
   vertical imprecision. It slightly worsens wide-scatter accuracy as keys narrow.
3. **Taller split:** strongest candidate for an opt-in phone trial. It preserves
   familiar letter/symbol assignments and improves the geometric tolerance while
   keeping the app above the IME. The height tradeoff must be user-controlled.
4. **Full-height sides:** do not ship as a default. Good key geometry cannot offset
   covering the app's cursor, text and controls. A transparent center is not a
   guarantee of a usable editor. It would require separate cross-app validation
   and a reliable way to dismiss or move the panels.

Next acceptance work: build an isolated optional Android split layout; test
candidate access, rotation with active composition, both-thumb overlap, symbol
slides and cleanup on the authorized phone. Compare real input errors and visible
latency before considering a default change.

## Reproduction

Run `python tools/assess_landscape_layouts.py` from the repository root with NumPy.
The script consumes the pinned inputs and produces summary, document records and
preview geometry. Serve the repository locally, then open
`docs/landscape-layout-study/index.html`. Keep the frozen plan and negative results;
do not describe synthetic contact totals as human-study sample size.
