# Candidate application profile on the current release-mode build

September 25, 2026. Runtime `6bffd17` / publication `e89a337`. The two unchanged
diagnostics pass on the accepted, non-debuggable release-mode package in phone
session `3bbe5578-dad5-4938-8362-9dd8b46dca28`. No editor fixture or runtime
instrumentation hooks are added. [Frozen plan](APPLICATION-REFRESH-PLAN.md).

Each diagnostic applies 46 distinct frozen prefixes three measured times after
its warm-up, using precomputed real candidates and production LocalLearning.
The untraced callback median is 1.035 ms and p95 is 3.691 ms over 138 callbacks.
These exclude provider computation, font filtering, keyboard rendering and
touch. They do not explain the entire candidate-update delay or certify it.

The refreshed sampled trace attributes 167,503 microseconds of thread time to
candidate application. Direct-child shares are sorting 32.10%, conversionInput
17.97% and stream anyMatch 11.34%. Native regex compilation alone accounts for
13.38% as an exclusive leaf. Those percentages divide sampled application time;
they are not shares of real typing latency. Sampling overhead, different process
state and intervening changes prevent a speedup claim against historical traces.

Source inspection confirms fixed expressions passed repeatedly to String.matches
in the conversion decision, English completion and apostrophe paths. The
[next frozen experiment](VALIDATION-PATTERN-PLAN.md) reuses compiled Patterns,
with a fresh Matcher per call and unchanged expressions. It does not introduce
a query cache, learning snapshot or new prediction rule. This addresses observed
repeated work; its effect on unhooked typing still needs a controlled comparison.

The largest remaining group is sorting, including learning-key preparation.
The trace's SharedPreferences awaitLoadedLocked frames do not prove disk waits:
the method is visited by ordinary already-loaded reads too. Do not infer a
storage bottleneck from its name or revive the rejected learning-key trial.

Aggregate attribution and cost summaries are in `application-refresh/`. Raw
trace/query telemetry stays local. Both phone clients terminated; original APK,
preferences, learning and Samsung IME were restored/read back; actual display
OFF was verified before the mutex and coordinating reservation were released.
