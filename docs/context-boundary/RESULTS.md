# Development loop: shared context-score work — accepted

## Problem, cause and change

Each candidate's boundary adjustment scored its full text twice: once after the
accepted context, once after an empty context. After two candidate characters,
both passes have the same preceding-character state. Repeating their remaining
hash lookups, string work and logarithms adds cost without additional evidence.

The new `chineseBoundary` method evaluates shared increments once while retaining
both original running sums. It does not truncate the sums: that could change
floating-point cancellation and reorder ties. When identical states also have
identical sums, the difference is exactly zero and work can stop. The estimator,
source counts, weights, vocabulary, candidate budgets and binary format are
unchanged. No retained cache or per-candidate field was added.

## Improvement

The scoring workload contains the first eight alternatives from 1,024 hash-selected
unique baseline queries: 8,192 context/candidate pairs, predominantly one or two
characters. Labels did not select the workload. After warm-up, ten rounds alternate
which implementation runs first; each round measures 163,840 score differences.

| Measurement | Two passes | Shared work |
|---|---:|---:|
| Median round-average time per score difference | 0.954 µs | 0.808 µs |
| Individual paired-round time reductions | — | 9.1% to 20.9% |

The median cost falls **15.3%**: `(0.954 - 0.808) / 0.954`. This is elapsed time for
context scoring alone, not total decoder latency, CPU time, or phone touch latency.
All ten paired rounds improved. Raw rounds are in `scoring.tsv`.

The single broad full-lookup comparison has p50/p95 0.530/2.043 ms before and
0.505/2.003 ms after, across 29,345 unique lookups. Mean was 0.722 → 0.700 ms;
maximum 26.849 → 27.008 ms. Do not infer a full-pipeline speedup from those single
runs. The repeated isolated scoring result is the supported performance claim.

## Quality and verification

- All **42,891** conditional probes have identical candidate text, order and
  consumed spans, separately covering essays, encyclopedia text and the small
  authored conversation set. No coverage or suggestion-quality gain is claimed.
- Bitwise score-difference parity is checked against an independent frozen copy
  of the original estimator across **171,708 source reading rows**, reversed
  context/text pairs, empty context, Unicode and serialized-count paths.
- Core suite passes **887,616 contract assertions**. This is not language accuracy.
  Host key-processing p50/p95/max: 0.02 / 0.54 / 48.59 ms.
- Pinned desktop Rime completes **13,014 inputs**, 11,272 uncached native queries;
  native IPC plus decoder p50/p95/max: 2.416 / 5.053 / 88.004 ms. This native
  boundary differs from the isolated Java scoring benchmark.
- No Android build, install or phone latency measurement in this loop.

Run `tools/test-core.ps1`, `tools/test-desktop.ps1`, `ContextBoundaryBenchmark`
with `workload.tsv`, and `ContextRankingEvaluation` with the preceding loop's
frozen `docs/context-backoff/inputs.tsv.gz`. Use `tools/report_context_backoff.py`
to compare complete outputs. Evidence hashes and workload lineage are retained
here. These reused data do not constitute a fresh holdout.
