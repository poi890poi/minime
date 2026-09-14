# Compact layouts: frozen experiment plan

2026-09-14. Classification: experimental tooling and design evaluation. No app,
dictionary, settings, or device changes. User requests compact continuous layouts,
not fewer keys. No production layout is selected by this experiment.

Use the prior pinned corpus, dictionary, decoder, noise profiles, seeds, and
60/70 mm widths. All letter regions are 30 mm high. Reproduce QWERTY and split
KALQ 30 mm. Reuse Colemak/Dvorak evidence only after verifying original hashes.
This is previously evaluated regression data, not a fresh holdout. Freeze these
geometries before evaluation; do not optimize against corpus labels or results:

- qwerty: original centered 10/9/7 rows.
- kalq-30mm: earlier split reference, 8 mm gap, four rows at 7.5 mm pitch.
- kalq-joined: identical KALQ row/column assignment and blank positions, zero
  central gap, horizontal pitch width/8. Four rows still occupy 30 mm.
- kalq-reflow: concatenate original KALQ rows across both blocks, remove blank
  cells, then wrap this sequence into 9/8/9 centered rows. This is an experimental
  adaptation, not the published KALQ layout. It changes row assignments as well
  as packing, so compare as a whole design, not a pure gap intervention.
- compact-aligned: concatenate QWERTY letter rows and wrap 9/8/9, left aligned,
  horizontal pitch width/9 and vertical pitch 10 mm.
- compact-staggered: same 9/8/9 letter assignment; middle row offset half a cell.
  This yields a hexagonal nearest-center tessellation in the interior. It is
  stretched to the fixed envelope, not regular hexagons or a Typewise replica.

Decoder receives contact coordinates, geometry and production word frequencies;
never expected words, genre, noise labels or future text. Expected text is used
only for generating intended taps and scoring. Keep all decoder weights fixed.
Report conversations and essays separately, plus all genres in raw evidence.
Primary condition 60 mm width, Gaussian standard deviation 1.5 mm per axis.
Stress: 2.5 mm scatter, inward bias, occasional slips; 70 mm width sensitivity.
Report wrong whole words / word attempts, valid-word confusions, harmful
corrections, same-thumb travel. These are synthetic sensitivity measures, not
human speed, touch latency, or population accuracy.

Separate control-boundary audit: add a common 10 mm bottom control row to every
layout (total envelope 40 mm). Five equal-width cells are Shift, comma, Space,
period, Delete. KALQ blank internal Space cells additionally remain active.
Nearest-center selection among letters and controls; this is an explicit
abstract geometry, not Android hitboxes. Replay corpus letter taps plus one
intended bottom-Space tap per word with matched noise. Count letter-to-control
and Space-to-letter hits. Do not feed these results to the frozen word decoder:
it assumes correct word boundaries. This audit identifies a limitation, not a
complete omission/insertion/segmentation model or corrected word accuracy.

Acceptance: reproduce baseline exactly; clean taps identify all letters and
controls; document totals reconcile; preserve negative results. Retain candidates
for human prototypes only if improvement survives stress and both genres.
No shipping decision without actual controls, unknown words, fresh conversations,
learning and timed two-thumb trials. Do not translate modeled travel into WPM.
