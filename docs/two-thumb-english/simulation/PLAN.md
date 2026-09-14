# Frozen layout-imprecision pilot

2026-09-14. Test/tooling experiment only; no Android or production-data changes.
Freeze this protocol before reading results. No phone operations are needed.

Compare letter-only QWERTY, Colemak, Dvorak, and split QWERTY. Preserve each layout's
letter row order, omit punctuation positions, center each row, and use the same
nominal ten-column pitch and 30 mm three-row height. Split QWERTY reserves an 8 mm
central gap within the same width, reducing horizontal pitch. These are explicit
geometric adaptations, not faithful emulations of the full keyboards or MinIME's
outer-key hit rectangles. QWERTY/Colemak/Dvorak orders are verified against
[XKB definitions](https://raw.githubusercontent.com/xkbcommon/libxkbcommon/master/test/data/symbols/us)
and [Colemak's design](https://colemak.com/Design).

Widths: 60 and 70 mm. Seeds: 20260914, 20260915, 20260916. Conditions:

- Exact center control.
- Independent Gaussian scatter: 1.5 mm standard deviation on both axes.
- Wider Gaussian scatter: 2.5 mm standard deviation on both axes.
- Directional bias: 1.5 mm scatter plus a 1 mm horizontal offset toward the center
  for each thumb and a 0.5 mm downward offset.
- Occasional slips: 1.5 mm scatter, with 10% of contacts instead using 4 mm scatter.

All conditions use matched random draws across layouts. These are sensitivity
assumptions, not measured human distributions. The decoder always assumes a fixed
2 mm isotropic uncertainty, independently of the generator's profile. No tuning
on the evaluation corpus and no layout optimization in this first comparison.

Use every eligible token in the existing GUM test TSV, with all 15 genres reported
separately. Keep conversation and essay results visible. Preserve document IDs.
This corpus has been evaluated previously: it is regression evidence, not a fresh
holdout. Whole lowercase ASCII words of length 1–20 are eligible; exclude entire
apostrophe-containing tokens, count exclusions, and do not silently split them.
The source TSV already excludes numeric/punctuation spans. No free-writing,
punctuation, spacing, omission, insertion, timing, or gesture accuracy claim.

Use the complete production English TSV's ASCII 1–20-letter vocabulary, folding
case and retaining each word's maximum source score. Expected corpus words must
not be added to this dictionary. Report out-of-vocabulary tokens explicitly.

Compare nearest-key literal input with three experimental word decoders:

1. Nearest dictionary word in spatial distance, with no word prior.
2. Spatial distance / (2 * 2 mm squared) minus source score / 64.
3. The same scored decoder, but replace only an unknown literal when the score
   margin over the second dictionary word is >= 2 and the candidate improves over
   the literal hypothesis (score zero prior) by >= 1.

All search is exhaustive within the observed tap count, without access to intended
text. Forced dictionary decoders are diagnostic upper-risk policies, not proposed
shipping autocorrection. The conservative condition illustrates the cost of a
valid-word guard; its thresholds are fixed hypotheses, not calibrated confidence.

Metrics: raw key and word error, raw valid-word substitution, corrected word error,
out-of-vocabulary coverage, damaged previously-correct words, error recovery, and
valid-word error recovery. Store per-document counts for paired comparisons.
Report across-seed ranges and bootstrap document-cluster intervals where compared.
Measure idealized alternating-thumb usage and within-word movement separately;
these are geometric proxies, never WPM or a measured fatigue score. Python desktop
experiment runtime is not Android touch latency.

Before trusting results, verify clean literal identity, decoder identity at exact
word centers without a prior, seed reproducibility, exhaustive-search correctness
against an independent small brute-force calculation, and dictionary exclusion of
unrecognized inputs. Preserve failed/negative results; make no automatic layout
replacement based on a simulated win.
