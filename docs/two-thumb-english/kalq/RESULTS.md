# KALQ under simulated touch imprecision

**Every error percentage below is incorrect whole words divided by word attempts, multiplied by 100.** One or more wrong letters makes a word incorrect. This is not an individual touch-miss rate or a speed measurement. After-correction errors include errors the decoder introduces. Counts combine three matched noise seeds.

The original KALQ paper reported 37 WPM after training six participants on a 7-inch tablet. That is separate from this phone-width simulation. [Authors’ paper, Figure 1](https://www.pokristensson.com/pubs/OulasvirtaEtAlCHI2013.pdf).

## Geometry and test scope

- QWERTY: original pilot, 30 mm letter-region height and 10 mm vertical pitch.
- KALQ 30 mm: original block/letter positions, four left rows compressed to 7.5 mm vertical pitch.
- KALQ 40 mm: 10 mm vertical pitch, requiring 10 mm more height than the baseline.
- Both KALQ versions have four columns per block and an 8 mm central gap. Space-key holes remain, but space activation is not simulated.
- Same 20,096 eligible word occurrences, 42,025-word dictionary, decoder, widths, error profiles, and seeds. The QWERTY rerun reproduces all earlier per-genre counts exactly.

## Primary comparison: 60 mm width, 1.5 mm scatter

Each axis has 1.5 mm Gaussian standard deviation. These are assumed contact errors, not measured human behavior. “Forced spatial + frequency” always selects a dictionary word. “Conservative” can retain the literal input and never replaces a recognized dictionary word.

### Conversation (2 source document(s))

| Layout | Before correction | Forced spatial + frequency | Conservative |
| --- | ---: | ---: | ---: |
| qwerty | 580/4,101 (14.14%) | 134/4,101 (3.27%) | 208/4,101 (5.07%) |
| kalq-30mm | 426/4,101 (10.39%) | 134/4,101 (3.27%) | 166/4,101 (4.05%) |
| kalq-40mm | 334/4,101 (8.14%) | 127/4,101 (3.10%) | 137/4,101 (3.34%) |

### Essay (1 source document(s))

| Layout | Before correction | Forced spatial + frequency | Conservative |
| --- | ---: | ---: | ---: |
| qwerty | 560/3,201 (17.49%) | 121/3,201 (3.78%) | 248/3,201 (7.75%) |
| kalq-30mm | 403/3,201 (12.59%) | 118/3,201 (3.69%) | 163/3,201 (5.09%) |
| kalq-40mm | 310/3,201 (9.68%) | 112/3,201 (3.50%) | 127/3,201 (3.97%) |

## Contact-profile sensitivity

Forced spatial + frequency word errors at 60 mm width. Each cell shows conversation / essay percentages. Denominators remain 4,101 conversation attempts and 3,201 essay attempts per cell across three seeds.

| Layout | 1.5 mm scatter | 2.5 mm scatter | Directional bias | Occasional slips |
| --- | ---: | ---: | ---: | ---: |
| qwerty | 3.27% / 3.78% | 9.83% / 10.50% | 4.49% / 5.19% | 5.97% / 6.59% |
| kalq-30mm | 3.27% / 3.69% | 11.75% / 11.18% | 4.27% / 4.03% | 6.88% / 6.40% |
| kalq-40mm | 3.10% / 3.50% | 9.07% / 8.53% | 4.02% / 3.84% | 5.56% / 5.62% |

## 70 mm width

1.5 mm scatter, forced spatial + frequency. Counts are incorrect words / attempts.

| Layout | Conversations | Essays |
| --- | ---: | ---: |
| qwerty | 124/4,101 (3.02%) | 109/3,201 (3.41%) |
| kalq-30mm | 121/4,101 (2.95%) | 107/3,201 (3.34%) |
| kalq-40mm | 115/4,101 (2.80%) | 100/3,201 (3.12%) |

## Correction harm

At 60 mm / 1.5 mm, counts of previously correct literal words changed into wrong words by correction. These errors are already included in the after-correction word-error totals.

| Genre / layout | Forced damage | Conservative damage |
| --- | ---: | ---: |
| conversation / qwerty | 95 in 4,101 total attempts | 1 in 4,101 total attempts |
| conversation / kalq-30mm | 99 in 4,101 total attempts | 2 in 4,101 total attempts |
| conversation / kalq-40mm | 102 in 4,101 total attempts | 3 in 4,101 total attempts |
| essay / qwerty | 75 in 3,201 total attempts | 2 in 3,201 total attempts |
| essay / kalq-30mm | 85 in 3,201 total attempts | 0 in 3,201 total attempts |
| essay / kalq-40mm | 88 in 3,201 total attempts | 0 in 3,201 total attempts |

## Limits and validation

- Only letter positions and confusion are tested. Accidental internal-space activation, true segmentation, extra/missing taps, gestures, learned grips and timing are absent.
- Nearest-letter regions can absorb space holes. Consequently this is not a full KALQ-versus-QWERTY usability comparison.
- One essay document and two conversation documents; corpus already evaluated. Three noise seeds are not three independent populations. No fresh-holdout or human-speed claim.
- The 40 mm variant spends extra height; any advantage must be considered with that cost. The 30 mm variant is the equal-height comparison.
- All 90 conditions completed. 5,400 aggregate rows reconcile with per-document counts; clean literal controls and error conservation pass; all original QWERTY counts match.
- An initial wrapper preflight failed before simulation because the reference QWERTY row definition was missing. The corrected wrapper keeps QWERTY and reruns it; no result or threshold was changed in response to KALQ outcomes.

```powershell
python tools/simulate_kalq_layout.py --out artifacts/kalq-new
python tools/report_kalq_layout.py artifacts/kalq-new --out artifacts/kalq-report-new
```

Reproduction also requires the referenced paper at `artifacts/kalq-reference/paper.pdf` for its source hash. No paper pages are redistributed in this evidence package.

Desktop experiment execution took 135.0 seconds. That is batch runtime, not touch latency.
