# Remaining candidate latency: experiment contract

Baseline runtime: 995b535 (documentation HEAD d82362b). Performance investigation,
with test-only stage hooks before selecting a production change. The measured
Chinese fresh-candidate p95 was 170.9 ms at a 150 ms key interval; target is 50 ms.

Replay the same eight frozen queries and four modes as the previous touch study.
These are development regressions, not a fresh holdout or multilingual coverage
sample. Preserve dictionaries, candidate identities/order, font filtering,
consumption, Space acceptance, settings and scheduling during diagnosis.

Measure request-to-main-delivery (queue + provider work + main dispatch), delivery
callback duration, its font-check and render subsets, and residual application
work. Provider counters are aggregate, include superseded work and cannot be
assigned to individual queries. Missing callbacks remain missing. English uses
synchronous core completion and has no decoder-request rows; frame timing still
covers it. Hooks add overhead, so compare phone changes with the same hooks and
verify surviving changes with the ordinary integration tests. Frame submission
is not physical display presentation or human touch latency.

Only retain a performance change if its targeted stage improves repeatedly,
candidate behavior is preserved, and end-to-end frame results support the gain.
Report other-mode regressions and missing/superseded samples. The broad release
acceptance gates remain open; short repeated samples cannot certify them.

Risk: test-hook lifetime or timing attribution could mislead diagnosis. Hooks
restore original callbacks, isolate nested callback samples, and remain entirely
under androidTest. No production telemetry or user text collection is added.

## Trial selected after the baseline

The first profile delivers all 301 asynchronous requests. Chinese callback p95 is
44.25 ms; font checks reach 87.86 ms on a cold prefix (129 retained candidates).
Other large lists spend 37–63 ms checking fonts. Worker delivery p95 is 38.65 ms;
render callback p95 is 10.58 ms. Do not sum unrelated percentiles.

Trial: warm the existing bounded font-capability cache on the decoder worker,
before delivery, checking cancellation between candidates. Keep filtering in its
original core position, so unsupported candidates cannot affect ranking differently.
Use separate Paint copies on each thread and short synchronized cache operations;
never hold the cache lock during font lookup. Preserve the 8,192-code-point cache
bound. Main-thread custom/English output retains its ordinary fallback font check.

This targets UI blocking, not cheaper font lookup. Cold candidates may still wait
for the same font work; report that limitation rather than promising 50 ms. Risks
are cache races, duplicate font work, stale work delaying the next request, and
extra warm-cache iteration. Compare with the unchanged baseline using identical
test hooks, including a second baseline run. Require repeated reduction in main
font/callback cost, candidate parity, passing integration, and no material repeated
raw-frame regression before retaining it. No dictionary/ranking change is allowed.

Baseline evidence: [first profile](stages-baseline/stages.json), [second profile
with learning-count timings](stages-votes/stages.json). The second run attributes
7.09 ms p95 to learning reads, with 20.21 ms residual application work; neither
explains the cold font spikes alone. Every asynchronous request delivered in these
runs, but some did not produce an observed matching frame before the next input.
The frame probe does not yet distinguish unchanged output from supersession.

Current baseline Chinese candidate-frame p95 at 150 ms is 126.39/112.57 ms (50/50
observed in each run), lower than the previous day's 170.90 ms without a production
change. Do not attribute that cross-session difference to this trial. The second
baseline has additional test-hook overhead; use its exact test APK for comparison.

Both phone sessions passed and restored the original APK, preferences and Samsung
IME, then verified display OFF. Session IDs: `ea6e300b-f297-4a4c-9ef0-620ff35c5ddd`
and `f1f5a5f1-9e72-4a97-aad0-446076957c6f`. Shared-core 887,604 assertions and pinned
desktop 13,014 rows/11,272 native queries passed before building the test APK.
