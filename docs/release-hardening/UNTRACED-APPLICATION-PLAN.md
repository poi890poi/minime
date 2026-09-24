# Check candidate-application cost without intrusive attribution

September 24, 2026. Diagnostic test-only change on accepted runtime 1ea175a.
The sampled profile and per-vote stage hooks identify possible work but add
overhead to hundreds of candidate operations. Several plausible micro-optimizations
have failed repeated end-to-end comparisons. Measure the same application body
without those hooks before selecting another algorithm or UI change.

Expose a second entry point on ApplicationProfileTest, retaining its exact frozen
prefix selection, providers, real LocalLearning, reset learning/preferences,
one warm-up and three recorded applications per prefix. Only disable ART method
tracing, and write a separate application-untraced.tsv. Provider lookup happens
before the measured application callback. The engine has no UI or device-font
predicate, so this isolates application/ranking and cannot certify presentation.
The separate English completion timer remains after the application, unchanged.

Record per-query durations and candidate counts, median/p95/max and request size.
This is a small diagnostic, not a natural-language accuracy or release-latency
sample. Do not compare its elapsed time to a profiler-attributed share as a
production speedup. Preserve the old traced entry point for reproducibility.
No dictionary, runtime, scheduling, scoring or release-package changes.

Build only the instrumentation after existing unchanged-core/desktop verification.
Run within a separately acknowledged phone window and shared mutex, with before/
after thermal/battery state and full original app/preferences/IME restoration and
verified display OFF. Decide the next experiment from this result and current
frame telemetry; do not automatically pursue further key-allocation changes.
