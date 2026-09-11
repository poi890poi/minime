# Bounded Japanese lexical prediction: coverage failure

Cooperative cancellation makes the isolated predictor return promptly, but it is
**unsuitable as a replacement for MinIME prediction**. In each of three passes,
192 of 384 queries abort, 102 complete with no candidates, and only 90 complete
with candidates. The exact target appears in 37 queries, against 196 for the
existing MinIME provider on the same frozen development screen.

Keep the cancellation experiment and its negative results outside Android. The
supported sentence-conversion optimization is evaluated separately in
[deterministic conversion](../japanese-determinism/README.md).

## Fixed contract and implementation

The [pre-run plan](PLAN.md) fixes an 8 ms cooperative deadline and 8,192 work
checkpoints. The baseline is the previously audited posting-index predictor,
using the entire typed prefix. Cancellation checks cover predictive trie
enumeration, reading expansion, token expansion and ranking comparisons. Any
exhaustion discards the entire captured ranking; no arbitrary subtree fragment
is exposed as a top-eight result. The next request starts with a fresh budget.

Dictionary bytes, surfaces, word costs, deduplication and lexical tie rules are
unchanged. Only typed input reaches the process; reference targets are scored
afterward. No frequency tuning, handpicked vocabulary or threshold sweep occurs.
This patch is independent of the A* tie fix: lexical prediction does not run A*.

The deadline is soft. Individual upstream lookups and OS scheduling can overrun
the next checkpoint. The measurements include unwinding and discarding aborted
work; they exclude Python romaji-to-kana conversion, IPC and UI rendering. Wall
timing includes the wrapper and IPC. There is no Android touch-latency claim.

## Latency, availability and memory

| Metric, all 384 queries in each pass | Pass 1 | Pass 2 | Pass 3 |
|---|---:|---:|---:|
| Mean engine latency | 3.714 ms | 3.705 ms | 3.694 ms |
| Engine p50 | 1.297 ms | 1.303 ms | 1.288 ms |
| Engine p95 | 9.023 ms | 9.018 ms | 9.011 ms |
| Engine p99 | 9.701 ms | 9.675 ms | 9.729 ms |
| Maximum engine latency | 10.645 ms | 10.535 ms | 10.557 ms |
| Wall p95 | 9.143 ms | 9.110 ms | 9.111 ms |
| Maximum wall latency | 10.796 ms | 10.676 ms | 10.696 ms |
| Unavailable queries | 192 | 192 | 192 |

The desktop latency screen (p95 <= 10 ms, maximum <= 16 ms) passes. Each pass has
123 deadline cancellations and 69 work-cap cancellations. Availability and target
hits agree across passes. A completed empty result is explicitly distinguished
from cancellation and from a usable ranking. No held results are produced.

The lexical-only process loads in 178.8–181.6 ms at 45.18–45.20 MiB RSS and ends
at 45.79–47.01 MiB RSS. It omits the sentence connection matrix, so this is not a
memory saving relative to the sentence converter's different responsibilities.
No model payload size changes. Per-query and load evidence is retained alongside
`summary.json` and `coverage.json`.

## Coverage by source and input condition

Each cell has 32 frozen probes. “Available” includes empty completed results.
Hits mean the exact independent target is among eight whole-buffer candidates;
they are not physical first-page measurements or human conversation accuracy.
The MinIME column reuses the prior run on identical IDs, typed inputs and targets.

| Conversation source / unit | Input | Available | Bounded hits | Existing MinIME hits |
|---|---|---:|---:|---:|
| RealPersonaChat / clause | Full | 28 | 4 | 21 |
| RealPersonaChat / clause | Half | 18 | 0 | 7 |
| RealPersonaChat / clause | Three-quarter | 28 | 1 | 10 |
| ASDC / clause | Full | 26 | 10 | 18 |
| ASDC / clause | Half | 15 | 2 | 11 |
| ASDC / clause | Three-quarter | 25 | 9 | 14 |
| RealPersonaChat / word | Full | 4 | 4 | 28 |
| RealPersonaChat / word | Half | 10 | 0 | 13 |
| RealPersonaChat / word | Three-quarter | 13 | 1 | 14 |
| ASDC / word | Full | 5 | 5 | 31 |
| ASDC / word | Half | 8 | 0 | 11 |
| ASDC / word | Three-quarter | 12 | 1 | 18 |

Partial target coverage is only 14/256 (5.5%), against 98/256 (38.3%) in the
existing provider. Full-input coverage is 23/128 (18.0%), against 98/128 (76.6%).
Broad short prefixes exhaust the full-subtree search; long clauses often are not
single lexical entries. Thus cancellation alone solves neither retrieval nor
sentence composition. The uncapped baseline previously exceeded five seconds on
some inputs; increasing this budget is not a supported typing-time solution.

These 384 probes were frozen before this experiment and were already evaluated.
RealPersonaChat is casual conversation; ASDC is task-oriented accommodation
dialogue. Readings are machine annotations with the documented source/OOV limits.
This small development screen supports rejection, not population accuracy or a
positive quality claim. The sentence tie experiment separately retains the
reviewed encyclopedia regression. No new essay generalization is claimed here.

## Verification and next integration boundary

Every completed ranking, including empty ones, agrees exactly with the uncapped
upstream lexical API: 138 unique completed inputs, zero ordered differences.
All three passes expose zero partial rankings. Three contract tests cover forced
zero-budget cancellation, prefix completion and repeated requests, and recovery
after a large-query cancellation. These synthetic tests are not accuracy scores.

```powershell
# Use the pinned modern Python/dependencies from japanese-engine-benchmark.
python docs/japanese-bounded-prediction/prepare.py
./docs/japanese-bounded-prediction/build.ps1
# Recreates the small upstream-built fixture if it is absent:
python docs/japanese-predictive-review/check_fixture.py
python docs/japanese-bounded-prediction/test_budget.py
python docs/japanese-bounded-prediction/evaluate.py
python docs/japanese-bounded-prediction/report.py
```

Reject this lexical implementation as the main predictor. Retain the existing
focused-language partial provider and use the supported indexed converter only
for a separately gated whole-sequence conversion path. The next implementation
step is a shared-core provider adapter with candidate/commit consistency,
stale-result cancellation and fallback contracts, before Android integration.
Any new ranking decision needs a fresh document-disjoint holdout. No app or
production dictionary changed, and no phone was operated. See [NOTICE.md](NOTICE.md).
