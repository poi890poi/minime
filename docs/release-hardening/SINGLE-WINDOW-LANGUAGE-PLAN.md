# Broader language replay after the single-window change

Frozen September 25, 2026, before these runs. Accepted runtime: local `e8bc0cf`,
publication `2558304`. This is follow-up measurement, with no new runtime tuning.

The repeated eight-query screen improves candidate timing but leaves Chinese at
150 ms key intervals over budget. Those reused mixed queries cannot characterize
Taiwanese/Japanese conversation performance. Run the existing frozen language
corpus, shard 0, at both established cadences for every mode. Keep source, genre
and input-condition breakdowns; this previously exposed corpus is development
data, not a fresh holdout or a language-accuracy reference.

Use the same final tests4.apk for accepted pre-change fixture
`artifacts/language-baseline/accepted.apk` and admitted single-window fixture
`artifacts/single-window/trial.apk`; hashes are already pinned in prior evidence.
All 24 assets are equal. Use ordinary unhooked methods, not QueueTrace or system
tracing. The exact action sequence must pass the existing workload validator.

Order: Chinese old/new, Taiwanese new/old, Japanese old/new, English new/old.
One new pair per language supplements the earlier repeated screen; it does not
establish repeated effects within every source/condition subgroup. Preserve every
run, missing frame, cancellation/unknown observation, thermal condition and
outlier. Report mean, median, p95, p99, maximum and denominators for raw/Space and
candidate submission, along with deadline coverage. Do not add percentiles or
interpret observed candidate counts as correct suggestions.

Apply existing absolute release budgets: raw/Space p95 <=33 ms and p99 <=50 ms;
candidate p95 <=50 ms and p99 <=80 ms. Investigate every >100 ms observation.
Do not change thresholds or select favorable queries. A missed budget remains
open even when the paired result improves. Any missing raw/Space frame or
functional failure blocks expansion and requires diagnosis. Use the resulting
stage/source breakdown to choose the next isolated experiment, if needed.

This does not complete the >=10,000 actions/mode across >=3 sessions gate.
Successful shard-0 integrity permits extending all four prescribed shards and
sessions later; the larger sample must not silently reuse synthetic assertion
counts or the same eight-word screen as conversation coverage.

Obtain a new explicit phone reservation. The runner holds the shared mutex,
requires the established cool start, and starts no run without 17 minutes of
acknowledged headroom for cooling, test and cleanup. Restore original APK,
preferences, learning and IME after each session; verify display OFF and explicitly
release ownership. No GitHub CI or Play publication.
