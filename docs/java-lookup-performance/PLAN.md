# Java lookup performance contract

Type: performance improvement in shared `ReadingUnitIndex` lookup, preserving
observable candidates. Baseline revision: dce6ddb. No scheduling, search budget,
beam, ranking, source data, binary format, or native-provider activation changes.

Hypothesis before editing: single-entry lookup builds and repeatedly sorts results
at every visited input offset, but returns only the final offset. Sentence
conversion needs intermediate results; lookup does not. Removing unused result
materialization must preserve the queue, visited states and search budget.

Use consumed, attributed conversation and general contract corpora, plus the exact
prior phone input stream. Freeze deterministic SHA-256 samples (up to 256 per
source/condition), before comparing. These are performance/regression inputs, not
new accuracy holdouts. Runtime receives only raw input and active pack; labels and
references never enter lookup. Record every candidate field in ordered digests.

Acceptance: zero output digest changes on every input/pass; repeat-process lookup
p95 and allocation improvement on Japanese, no material repeated regression on
Taiwanese or Chinese. Report cold/first and warmed passes separately, with mean,
median, p95, p99 and max, genre/source/condition and empty-output counts. Separate
loading and fingerprinting from lookup. Desktop timings do not establish Android
or touch latency. Phone integration needs a fresh acknowledged device lease.

Risk: accidentally skipping intermediate results needed by Chinese sentence
conversion, or changing bounded traversal. Keep conversion on its existing path;
run shared-core regressions and pinned desktop Rime before Android. Stored data
and preferences remain compatible. Rollback is reverting the isolated code change.
