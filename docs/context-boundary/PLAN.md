# Development loop: reuse shared context-score work

Baseline: `5b704a4`, restored after the rejected backoff trial. Performance change
only. `PhoneticDictionary` computes a full character score with context, computes
the full score again without context, then subtracts. After two characters the
two preceding-character states are identical, so their remaining increments are
identical. Computing those increments twice wastes lookup/string/log work.

Evaluate each shared increment once, but accumulate both running sums in their
original order. Do not truncate to the first two characters: changing floating
point cancellation can reorder ties. Require bitwise-equal score differences
against the existing two-call implementation and identical candidate text/span/
order on all 42,891 frozen conditional probes. Preserve source data, model
estimator, defaults, modes, vocabulary, search budgets and binary format.

Runtime uses only existing context/text/counts. Test labels remain post-run only.
Use the corpus frozen for the preceding backoff experiment; no holdout claim.
Run alternating baseline/optimized microbenchmarks on the same candidate workload
with warm-up and multiple rounds. Report context-score cost separately from full
lookup and phone input latency; no phone claim. Land only with repeatable scoring
improvement, no observed lookup regression, exact output parity and core/native
gates passing. No new retained cache or per-candidate fields.
