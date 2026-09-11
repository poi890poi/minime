# Deterministic conversion and bounded prediction

Baseline: 09b3257. Bug fix boundary: the experimental converter's A* equal-cost
tie ownership. The shipped app does not use this converter.

Prior evidence: identical posting payloads but 199 differing candidate-list rows;
StateLess compares state addresses after cost, position and length. Replace only
that final comparison with a per-search insertion sequence. Keep costs, traversal,
beam size, dictionary bytes and top-eight budget fixed. A comparator regression
must fail under reversed allocation order before the fix and pass afterward.

Build both unindexed and indexed converters with the same deterministic tie rule.
Compare all 18,730 prior primary regression records exactly, then three isolated
timing passes. These are parity regressions, not fresh quality holdouts. Separately
report changes from the old address-based ordering, including target outcomes;
do not claim the bug fix preserves undefined equal-cost order.

Next, test bounded lexical work as an independent mechanism. A query must either
finish or report unavailable; never expose an arbitrary truncated enumeration as
a ranked top-eight result. Keep the existing provider available as fallback.
Freeze the work budget and input corpus before evaluation, retain deadline misses
and missing coverage, and do not promote without evidence. All code stays outside
Android production until the shared-core/provider and device gates are met.
