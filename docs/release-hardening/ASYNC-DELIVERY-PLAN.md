# Decoder-result delivery experiment

September 25, 2026, frozen before implementation or trial replay. Based on
[three-mode queue measurements](QUEUE-STAGES-RESULTS.md).

Hypothesis: synchronization barriers explain a removable part of the 8–11 ms
mean post-to-main wait. Mark only the Message carrying AsyncDecoder's existing
result Runnable asynchronous. Keep its Handler, Looper, 8 ms scheduled delay,
single worker, provider order, dictionaries, cancellation and closed guards.
Do not change lifecycle/loading handlers or input priority. This is an experiment,
not an assumed improvement. Revert the production change if the evidence fails.

First test a capturing Handler to prove the message flag and main-thread delivery,
then existing burst/cancel/close contracts. Add coverage for closing while a
result is already enqueued. These checks must catch stale callbacks regardless
of their ordering against ordinary main-thread messages. Core and pinned native
output must be unchanged before Android packaging; compare packaged assets.

Use one normal instrumentation APK for both unhooked accepted/trial fixture APKs.
Screen with the existing fixed four-mode touch workload in A-B-B-A order, under
the same cool-start checks, cadence, preferences and cleanup. Report every mode
and cadence independently, all denominators, candidate frame counts and strict
deadline observations. Reject if any mode/cadence repeatedly worsens raw or Space
p95/p99 across both paired comparisons, or repeated candidate losses result.
Candidate improvement must repeat rather than rely on pooled percentile changes.
Do not change thresholds or choose spellings after observing results.

If the screen survives, repeat the language-specific frozen shard and extend
across prescribed shards/sessions; short replay alone cannot certify release.
Use an isolated queue-instrumented trial only as a separate causal diagnostic;
never mix hooked and unhooked timings to claim speedup. A smaller post-to-main
interval would support the synchronization-barrier hypothesis; unchanged delay
would reject it. Same-process test-editor timing needs subsequent Chrome/Keep
integration checks. No hidden Android APIs or synthetic barriers are needed.

Risks: moving candidate application ahead of ordinary messages may compete with
raw/Space rendering or lifecycle work. Keep first-frame and cancellation gates;
do not accept average gains that mask repeated tail regressions. No language
quality or licensing issue is resolved by this delivery change.

Get a new explicit SHINE reservation and hold the shared phone mutex for every
device operation and cleanup. Bound the runner to the acknowledged window;
restore APK/settings/learning/IME and verify display OFF, then explicitly release.
No GitHub CI, final distribution or Play action is part of this experiment.
