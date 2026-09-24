# Candidate font pages: useful Chinese signal, hold pending broader evidence

September 24, 2026. **Hold outside production.** The trial consistently reduces
the slower-paced Chinese candidate tail, but other modes and acceptance timings
are mixed. The eight-query shared-spelling replay is too narrow to decide the
four-language tradeoff. Accepted runtime remains `1ea175a`. No release package
or upload is produced by this experiment. [Frozen plan](CANDIDATE-PAGES-PLAN.md).

## Problem, cause and change

Font checks traverse the entire ranked result before the first strip appears.
The trial resolves the Space default immediately, then validates later entries
when a viewport requests them. It preserves an ordered source snapshot so the
old result remains pageable while a new spelling is pending. Full-list callers
and expansion still receive every readable candidate. No source, score, limit,
default-selection policy, dictionary byte, font rule or decoder changes.

The trial adds a small thread-confined page owner and a bounded strip request.
This retains the ranked source references longer and moves font work to expansion;
it is not a free reduction of all work. The complete trial and its regressions
are preserved in `held-candidate-pages.patch` for a larger evaluation.

## Correctness and work moved

Shared core passes 2,008,883 contract assertions, including 364 generated page
cases against the old full filter. The 13,014-input pinned desktop output is
byte-identical. Phone integration passes 12 paging/stability/selection tests.
[Integration evidence and earlier viewport-test failures](candidate-pages-integration/README.md).
These are contract checks, not language accuracy or physical touch certification.

An isolated rendering diagnostic replays 46 frozen prefixes twice, with real
font capability and local learning, precomputed providers, and offscreen
measure/layout/bitmap drawing. All **92 complete candidate/default identities**
match; all deep-choice output assertions pass. Per pass, baseline validates
14,146 strings before the first strip. Trial validates 1,084 before expansion
and the remaining 13,062 during expansion. No candidate was deleted to save work.

Costs below are **p95 milliseconds for detached work**, not touch latency:

| Stage | Baseline first / second pass | Trial first / second pass |
|---|---:|---:|
| Apply plus initial strip | 128.40 / 63.29 | 69.17 / 59.81 |
| Expand and draw complete grid | 529.95 / 526.41 | 541.73 / 530.83 |
| Select final candidate | 1.80 / 1.86 | 3.31 / 3.24 |

Expansion maximum rises from 611.08 to 735.14 ms on the first pass. The current
full-grid allocation already dominates expansion cost; paging only font checks
does not solve that. Deep selection also adds roughly 1.4–1.5 ms at p95 in this
isolated diagnostic. [A](candidate-pages-cost-a/summary.json),
[B](candidate-pages-cost-b/summary.json). Do not hide these costs behind typing gains.

## Phone timing

Six unhooked replays use the identical timing APK and frozen eight spellings,
each with 400 letters and 64 Space actions: **2,784 injected actions** in total.
Every raw-editor and Space frame is observed. Candidate timing excludes missing
observations; their counts and next-release deadlines remain in the raw reports.
This observes submitted frames, not the physical digitizer or displayed photons.

Chinese at 150 ms key-release spacing has candidate p95 **112.81→77.40**,
**96.15→76.73**, and **96.39→84.85 ms** in the three A/B comparisons. p95 is the
95th percentile of observed fresh-result timings. Baseline/trial observations
are respectively 50/50, 48/50 and 48/50 out of 50 letters per run.

The original A/B/B/A series warms from thermal status 0 to 1. A subsequent B/A
pair begins each run at status 0 and 33.8 C battery temperature; both finish at
status 0, 34.7/34.8 C. These snapshots reduce one confound without establishing
constant CPU speed or explaining earlier sessions.

The cooled pair below lists **candidate / raw / Space p95 in milliseconds**, then
**observed candidate submissions / letters**. Candidate and raw have 50 letters;
Space has only eight samples per cell, so its p95 is the observed maximum.

| Mode / spacing | Baseline A3 | Trial B3 |
|---|---|---|
| Chinese / 150 | 96.39 / 24.11 / 31.97; 48/50 | 84.85 / 22.84 / 31.55; 50/50 |
| Chinese / 60 | 79.65 / 38.30 / 39.03; 31/50 | 89.36 / 32.21 / 30.34; 39/50 |
| English / 150 | 33.29 / 35.59 / 15.18; 50/50 | 31.93 / 33.75 / 18.69; 50/50 |
| English / 60 | 34.94 / 32.81 / 16.05; 50/50 | 36.25 / 32.09 / 16.67; 50/50 |
| Taiwanese / 150 | 87.08 / 25.31 / 29.72; 49/50 | 85.60 / 24.88 / 30.96; 49/50 |
| Taiwanese / 60 | 65.29 / 37.85 / 29.54; 41/50 | 65.73 / 34.72 / 26.72; 43/50 |
| Japanese / 150 | 86.94 / 23.76 / 28.87; 49/50 | 84.37 / 23.33 / 32.82; 49/50 |
| Japanese / 60 | 74.35 / 28.78 / 30.95; 46/50 | 83.98 / 27.25 / 30.59; 48/50 |

Fast Chinese submissions before the next release improve from 23 to 28 out of
50 in the cooled pair, while the conditional candidate p95 gets worse. Japanese
fast deadlines go from 38 to 37 out of 50; Japanese slower Space worsens in this
pair and the first pair. The smaller sample cannot settle those regressions.
Do not claim universal speedup, non-regression, or completed latency acceptance.

Next: freeze broader language-specific inputs and retain genre/reading/error
labels, then evaluate this same trial without tuning it to outputs. All inputs
here were reused development material. Full-grid allocation is a separate
future experiment. [Binaries, sessions and hashes](candidate-pages-binaries.json)
and the six replay directories retain mean/median/p95/p99/max, actual cadence,
missing observations, deadline counts and environment. `report-candidate-pages.py`
rebuilds these summaries from guarded session evidence. Every session restores
the original app/preferences/IME and verifies display OFF. The final reservation
was explicitly released after all clients, including cooldown polling, finished.
