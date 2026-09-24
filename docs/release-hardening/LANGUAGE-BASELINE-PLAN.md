# Accepted-runtime language timing baseline

September 25, 2026, before replay. Verification only, no runtime/data changes.
Baseline is accepted runtime `6bffd17` / publication `e89a337`, after reverting
the rejected compiled-validator trial. Core restoration passes 1,770,165 checks;
accepted pinned-desktop evidence remains attached to the completeness change.

Use the existing frozen 720-query language-timing inventory and unchanged
TouchLatencyTest observer. Begin with shard 0 in Chinese, English, Taiwanese and
Japanese, in that order; run one test/report per session, both 150/60 ms cadences.
Include every frozen input in that shard without selecting by output or latency.
No new code, thresholds, dictionaries, warm-up policy or test labels. Corpus SHA
`5c316cebea4ed88352a946fafe01fcda4e922c49a47b12783996a53ec1995f49`.

Build the accepted sources as non-debuggable release-mode packages with the
unchanged test-only EditorTestActivity fixture. Inspect manifest and asset identity
against the accepted ordinary release. This is a measurement package, never a
store artifact. No validator experiment class remains in active sources.

Require a new explicitly acknowledged phone window and shared mutex. Cool each
start to thermal status 0 and battery below 34 C; retain environment snapshots.
Bound individual instrumentation to 300 seconds and cooldown to ten minutes;
reserve cleanup headroom before starting each run. Stop without starting another
run if the reservation cannot cover those bounds. Restore original APK/settings/
learning/IME and verify display OFF after every run, then explicitly release.

Validate every action against the frozen mode/shard inventory. Preserve raw
samples locally; publish aggregate counts and per-source/genre/condition metrics.
Report all raw, Space and candidate observed/missing/deadline denominators plus
mean, median, p95, p99 and maximum. Compare to fixed release targets (raw/Space
p95 33 ms, p99 50 ms; candidates p95 50 ms, p99 80 ms) only as a diagnostic screen.
Missing frames do not become zero latency. Separate short-stratum maxima from
stable tail estimates. These reused development inputs and injected events do
not certify fresh conversation quality, human touch, screen presentation, the
10,000-actions-per-mode/three-session gate, or Chrome/Keep compatibility.

Do not infer a speedup from older-build or cross-session comparisons. This loop
locates remaining failures on the accepted build; any follow-up change needs its
own causal experiment. Retain all failures and incomplete runs.
