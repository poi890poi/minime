# Exact completion existence: retain the narrower query

September 24, 2026. **Land the exact short-circuit predicate.** It avoids
constructing/ranking a list for a boolean decision, preserves source behavior,
and shows modest Chinese timing gains in the first phone pair. Larger gains in
the second pair coincide with a slower baseline session; do not attribute all of
them to this change. No general p95 or release-readiness claim.
[Predeclared experiment](EXISTENCE-PLAN.md).

## Problem, cause and change

CompositionEngine.conversionInput asks whether an English completion exists. Its
old implementation generates up to 24 ranked candidates, scanning all matching
dictionary entries and maintaining a heap. The new hasEnglishCompletion stops
at the first eligible entry. It applies the same input/case/context/source-case
rules and excludes an exact word as its own completion. The original full-list
implementation is unchanged and serves as the independent oracle.

No dictionary bytes, scores, candidate limits/order, Space policy, learning,
privacy, UI, settings or scheduling changes. The immutable dictionary owns the
lookup; no cross-query cache or word-specific exception is added.

## Exact behavior and isolated work

Every distinct source prefix is exercised in lower/title/upper/mixed casing,
with deterministic extensions and malformed/apostrophe/Unicode boundaries, in
both language-context policies. **880,100 decisions agree exactly** with the
existing full-list method (440,050 inputs × two policies); 16 empty-dictionary
checks also pass. These are equivalence checks, not language accuracy.
The full core suite passes 1,768,296 assertions. Pinned desktop output remains
byte-identical over 13,014 inputs and 11,272 native queries.

A deterministic hash sample of 983 source-derived prefixes × two contexts runs
five measured rounds after two warm-ups, alternating method order. Across 9,830
JVM measurements, full-list mean/median/p95 are 2.880/1.900/3.700 microseconds;
existence mean/median/p95 are 1.483/1.400/1.801 microseconds. These are local JVM
lookup costs, not Android or physical-touch latency. Distinct-prefix sampling
does not model natural typing frequencies. [Raw microbenchmark](existence-micro/summary.json).

## Paired phone result

Run order A1, B1, B2, A2. Identical test APK, assets, eight frozen development
queries and 150/60 ms release spacing. Only classes.dex differs outside signing
metadata. Each replay has 400 letters plus 64 Space actions, all raw/Space frames
observed. Four replays pass: **1,856 injected actions**. Unhooked frame observation
uses the existing corrected next-release boundary.

Cells are **candidate p95 / raw-text p95 in milliseconds**, then **candidate
submissions before the next release out of 50 letters**. p95 means 95% of observed
timings fall at or below it; unobserved candidate frames are excluded from that
latency percentile but remain in the deadline denominator and raw reports.

| Mode / spacing | A1 | B1 | A2 | B2 |
|---|---|---|---|---|
| Chinese / 150 ms | 95.78 / 25.83 (49/50) | 93.23 / 21.53 (49/50) | 103.72 / 28.05 (48/50) | 118.76 / 19.45 (50/50) |
| Chinese / 60 ms | 69.87 / 21.74 (36/50) | 70.16 / 21.67 (37/50) | 86.13 / 38.14 (24/50) | 75.62 / 21.35 (40/50) |
| English / 150 ms | 27.74 / 28.37 (50/50) | 28.91 / 30.43 (50/50) | 36.34 / 35.47 (50/50) | 27.57 / 29.15 (50/50) |
| English / 60 ms | 27.13 / 26.97 (50/50) | 28.51 / 27.62 (50/50) | 36.38 / 33.10 (50/50) | 28.31 / 27.95 (50/50) |
| Taiwanese + English / 150 ms | 67.48 / 22.90 (49/50) | 69.44 / 22.05 (49/50) | 86.02 / 24.17 (49/50) | 69.21 / 21.00 (49/50) |
| Taiwanese + English / 60 ms | 69.99 / 21.69 (44/50) | 64.73 / 22.58 (41/50) | 64.96 / 33.59 (35/50) | 67.99 / 22.59 (44/50) |
| Japanese + English / 150 ms | 68.31 / 21.77 (49/50) | 69.18 / 20.54 (49/50) | 87.33 / 24.08 (49/50) | 72.45 / 21.90 (49/50) |
| Japanese + English / 60 ms | 68.91 / 21.41 (46/50) | 67.73 / 20.84 (46/50) | 81.91 / 28.82 (36/50) | 75.63 / 21.63 (42/50) |

The first pair's Chinese raw p95 improves 25.83→21.53 ms at the slower pace;
fast raw p95 is nearly unchanged and timely candidates improve only 36→37/50.
The second pair has much larger gains, but English/control timings also improve,
so these are not isolated causal estimates. Chinese captured candidate frames
rise 44→45 and 29→48/50 at the fast pace; missing frames are not dictionary misses.

The apparently worse second slow Chinese p95 partly reflects newly observed slow
prefixes: B2 captures all 50 while A2 captures 48. Restricting to the same observed
prefixes yields Chinese mean 52.93→50.56 and 60.66→50.70 ms at 150 ms spacing;
shared-observation p95 is 95.78→79.80 and 103.72→94.45 ms. Fast shared means improve
46.28→44.69 and 51.43→41.28 ms. This restricted comparison remains conditional and
does not erase misses or individual regressions. One shared slow-prefix observation
also worsens substantially. All original timings remain available; no input is
removed to improve a headline. [Shared-observation analysis](existence-common-observations.json).

Other modes have mixed changes, especially Taiwanese in the first pair. These
small sessions do not prove statistical non-inferiority or general responsiveness.
The decision combines exact semantic equivalence, a bounded reduction in lookup
work and a modest repeated Chinese benefit; it does not claim that the larger
second-pair gain or every mode's variation is caused by the predicate.

## Limits and remaining gates

All four sessions restore original APK/preferences/IME and verify display OFF.
The reservation was explicitly released after all six separately reported
prefix-selection batches completed and verified cleanup. [Binary/session
identities](existence-binaries.json), [A1](existence-a1/summary.json),
[B1](existence-b1/summary.json), [B2](existence-b2/summary.json),
[A2](existence-a2/summary.json). Full distributions include actual input cadence,
missing observations, mean, median, p95, p99 and maximum.

Input is reused development material, not fresh conversation/essay holdout data.
OS injection bypasses the digitizer, and submission callbacks are not display
presentation or human hit rate. Thermal/CPU frequency was not captured; the
second-session variation has no proven thermal diagnosis. Candidate tails over
100 ms and the 50 ms p95 target remain unresolved. Complete larger, varied
release measurements with environment telemetry before performance acceptance.
No release payload, Play upload or GitHub CI is produced by this change.
