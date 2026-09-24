# Locate the remaining candidate tail after removing popup relayout

Frozen September 25, 2026, before preparing or running this diagnostic. The
accepted runtime is local `e8bc0cf` / publication `2558304`. No production
algorithm, delay, dictionary, rank or acceptance change is proposed here.

The ongoing unhooked language replay already identifies two remaining aggregate
misses: Chinese ordinary-cadence candidate p99 is 81.29 ms against 80 ms, and
Taiwanese ordinary-cadence p95 is 52.17 ms against 50 ms. The complete eight-run
comparison must still be reported, including unfavorable results. These
observations justify diagnosis; they do not identify the remaining causal stage.

Reuse the existing bounded QueueTrace observer and CandidateTimingProbe against
the current single-window runtime. Keep the same 8 ms delayed single worker,
ordinary main Handler, cancellation guards, provider order and candidate display
filter. Generate an isolated source overlay from current sources. The only new
test adaptation enables the existing stage observer inside each queue replay,
allowing queue boundaries, font checks, callback application and frame callbacks
to be examined in the same session. Preserve the original observers and their
contract tests. Instrumentation perturbs timing: do not compare its latencies
with unhooked runs as a speedup or release acceptance.

Run complete frozen shard 0 for Chinese, Taiwanese and Japanese at both 150/60 ms
cadences, in that order. No query selection by latency, phrase, glyph or expected
result. English is an immediate-path control in the unhooked comparison; these
three modes exercise the asynchronous provider path. Validate the exact action
inventory and all raw/Space observations before expanding to another session.
Keep cancelled, stale, unmatched, ambiguous and missing observations visible.

Report stage counts and mean/median/p95/p99/maximum by mode, source/genre and
input condition. Correlate requests with their actual key-release windows using
the existing strict analyzers; never assign ambiguous requests arbitrarily or
add percentiles from different stages. Investigate all observations above 100 ms
and missing candidate frames using same-session chronology. Report any stage
that cannot be attributed as unresolved. This is a full-workload diagnostic, not
a new holdout, language-quality benchmark or large-sample latency certificate.

Only after the measurements identify a remaining cause may a separate
one-variable runtime experiment be frozen. Earlier failed zero-delay, font
warming, paging and asynchronous-delivery experiments remain rejected; removing
one bottleneck does not automatically admit those changes.

Before building, check the unchanged core/native evidence and observer contracts.
Record active/overlay source hashes, exact app/test APK identities, identical 24
language assets, the frozen timing corpus and the non-debuggable fixture manifest.
No observer, test editor or telemetry belongs in an ordinary release artifact.

This requires a separate explicit phone acknowledgement: the current reservation
covers only eight unhooked comparison sessions. Hold the shared mutex, require
cooled starts and at least 17 minutes of acknowledged headroom before each run.
Restore original APK, preferences, learning and IME after every session; verify
display OFF and explicitly release ownership. No hosted CI or Play action.
