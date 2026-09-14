# Layouts under simulated touch imprecision

2026-09-14. This is a letter-only geometry/decoder sensitivity experiment, not a human typing-speed benchmark.

20,096 eligible occurrences across 15 genres and 26 documents; 42,025 dictionary words; 738 corpus occurrences absent from that dictionary (3.67%). Excluded 305 whole non-ASCII/apostrophe/over-length tokens from the already normalized corpus.

Four layouts × two widths × five contact profiles × three matched seeds = 120 conditions. The corpus and its labels were already used in earlier evaluations; no fresh-holdout claim. All 7200 genre/decoder/seed rows are retained in `summary.json.gz` and per-document counts in `documents.jsonl.gz`.

## Conversations and essays

60 mm usable width, 1.5 mm scatter. Percent of words incorrect, averaged across three seeds. “Valid error” means a wrong literal spelling that exists in the frozen dictionary, including abbreviations and rare entries; it does not certify ordinary English usage. The forced decoders always choose a dictionary word and are diagnostic, not shipping policies.

### Conversation — 1367 words, 2 document(s)

| Layout | Literal error | Valid error | Spatial only | Spatial + frequency | Conservative |
| --- | ---: | ---: | ---: | ---: | ---: |
| qwerty | 14.14% | 1.07% | 3.80% | 3.27% | 5.07% |
| colemak | 13.70% | 1.83% | 4.56% | 3.80% | 5.51% |
| dvorak | 14.02% | 1.95% | 4.80% | 3.78% | 6.14% |
| split-qwerty | 22.68% | 1.73% | 4.83% | 3.76% | 8.83% |

### Essay — 1067 words, 1 document(s)

| Layout | Literal error | Valid error | Spatial only | Spatial + frequency | Conservative |
| --- | ---: | ---: | ---: | ---: | ---: |
| qwerty | 17.49% | 1.44% | 4.65% | 3.78% | 7.75% |
| colemak | 16.99% | 1.37% | 4.62% | 3.91% | 7.75% |
| dvorak | 16.93% | 1.97% | 5.25% | 4.28% | 7.84% |
| split-qwerty | 27.99% | 2.09% | 5.69% | 4.40% | 12.28% |

## Wider scatter, directional bias, and occasional slips

Forced spatial + frequency word-error percentage at 60 mm. Conversations / essays are shown separately.

| Layout | 1.5 mm scatter | 2.5 mm scatter | Thumb bias | Occasional slips |
| --- | ---: | ---: | ---: | ---: |
| qwerty | 3.27% / 3.78% | 9.83% / 10.50% | 4.49% / 5.19% | 5.97% / 6.59% |
| colemak | 3.80% / 3.91% | 12.48% / 11.06% | 5.24% / 4.75% | 7.32% / 7.06% |
| dvorak | 3.78% / 4.28% | 14.26% / 12.65% | 5.32% / 5.37% | 7.95% / 7.90% |
| split-qwerty | 3.76% / 4.40% | 11.24% / 11.97% | 5.22% / 5.97% | 6.56% / 7.31% |

## Width sensitivity

At 1.5 mm scatter, forced spatial + frequency errors. A wider simulated board changes geometry; it does not reproduce a different physical grip.

| Layout | Conversation 60 → 70 mm | Essay 60 → 70 mm |
| --- | ---: | ---: |
| qwerty | 3.27% → 3.02% | 3.78% → 3.41% |
| colemak | 3.80% → 3.22% | 3.91% → 3.59% |
| dvorak | 3.78% → 3.15% | 4.28% → 3.62% |
| split-qwerty | 3.76% → 3.07% | 4.40% → 3.66% |

## Correction harm and uncertainty

Damage below counts previously correct literal words changed to wrong words, per 1,000 total word occurrences. Recovered valid errors counts cases where the literal was a wrong dictionary word but the forced decoder recovered the intended word. The conservative policy retains all valid literal words, so it cannot recover those errors.

| Genre / layout | Forced damage per 1,000 | Conservative damage per 1,000 | Forced valid errors recovered | Forced error across seeds |
| --- | ---: | ---: | ---: | ---: |
| conversation / qwerty | 23.17 | 0.24 | 22/44 | 3.15–3.37% |
| conversation / colemak | 23.90 | 0.00 | 33/75 | 3.66–3.88% |
| conversation / dvorak | 22.92 | 0.49 | 37/80 | 3.51–4.17% |
| conversation / split-qwerty | 21.46 | 0.24 | 38/71 | 3.44–3.95% |
| essay / qwerty | 23.43 | 0.62 | 23/46 | 3.66–4.03% |
| essay / colemak | 23.74 | 0.00 | 19/44 | 3.84–3.94% |
| essay / dvorak | 24.99 | 0.31 | 30/63 | 3.84–4.87% |
| essay / split-qwerty | 20.31 | 0.62 | 37/67 | 4.12–4.59% |

Seed ranges describe Monte Carlo variation only. Conversation and essay each contain very few source documents; they cannot support a dependable per-genre population confidence interval. The paired document bootstrap below is an exploratory corpus-mixture comparison, not a general English or human-performance claim.

| Alternative minus QWERTY, 60 mm / 1.5 mm | Word-error difference (percentage points) | Exploratory 95% document bootstrap |
| --- | ---: | ---: |
| colemak | +0.134 | +0.052 to +0.216 |
| dvorak | +0.368 | +0.270 to +0.463 |
| split-qwerty | +0.348 | +0.291 to +0.410 |

## Movement proxies

60 mm conversation tokens. Distance is per movement between one thumb’s consecutive letters within a word. Thumb is assigned by screen half. There is no Space, cross-word travel, or learned human thumb assignment.

| Layout | Distance per same-thumb movement | Alternating-thumb fraction |
| --- | ---: | ---: |
| qwerty | 13.40 mm | 49.97% |
| colemak | 13.01 mm | 48.37% |
| dvorak | 12.11 mm | 68.51% |
| split-qwerty | 12.49 mm | 49.97% |

## Most frequent valid-word confusions

Automatically selected by occurrence count, then lexical order, across all 15 genres at 60 mm / 1.5 mm scatter / three seeds. These illustrate errors only; they supply no production exception or layout weight.

| Layout | Intended → literal (occurrences across three noise replicates) |
| --- | --- |
| qwerty | `of` → `if` (48); `to` → `yo` (30); `in` → `on` (23); `for` → `foe` (16); `is` → `us` (15) |
| colemak | `the` → `she` (81); `of` → `if` (48); `and` → `ant` (35); `in` → `ih` (32); `to` → `do` (32) |
| dvorak | `the` → `tho` (66); `of` → `ef` (55); `of` → `af` (48); `and` → `asd` (37); `to` → `ta` (37) |
| split-qwerty | `of` → `if` (80); `in` → `on` (39); `for` → `foe` (26); `is` → `us` (23); `is` → `id` (21) |

## Interpretation limits

- Letter-only, centered-row adaptations use nearest-letter Voronoi regions. Punctuation positions are removed; empty margins cannot register non-letter keys. These results do not reproduce a shipping keyboard’s full hit map.
- Scatter, bias and slips are assumed distributions. They exclude motor learning, reach-dependent variance, occlusion, correlated trajectories, gesture collisions, missed/extra contacts and physical latency.
- Word boundaries and length are given. Exhaustive same-length dictionary search is an offline experiment, not the current MinIME decoder and not a measured mobile implementation.
- Fixed source-score scaling and conservative margins are not calibrated probabilities. Context is absent. A forced choice can damage unknown words; clean-center errors expose this rather than hiding it.
- Previously evaluated corpus; 26 document clusters and a specific genre mixture. No fresh holdout or user study. Do not promote a layout based solely on these results.

## Reproduce

```powershell
python tools/simulate_english_layouts.py --self-test
python tools/simulate_english_layouts.py --out artifacts/layout-imprecision-new
python tools/report_english_layouts.py artifacts/layout-imprecision-new --out artifacts/layout-report-new
```

Full simulation desktop runtime: 189.0 seconds. This is experiment execution time, not typing or touch latency.
