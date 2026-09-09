# Skip unrelated initial branches

Performance change. ReadingUnitIndex previously scanned every child syllable at
branching nodes, even if its first character could not match the typed letter.
An auxiliary sorted child array for nodes with at least eight children selects
the matching initial range by binary search. Small nodes retain linear traversal.
All matching children still enter the same priority queue, with the same source
node IDs, scoring, bounds and work budget. The compiled model format is unchanged.

The index costs about 2.4 MB on the expanded all-pack desktop workload. Combined
with compact storage, retained heap is 92.8 MB (repeat 92.7 MB), below the old
inventory's 96.2 MB. All 23,532 mode output vectors match; core 32,328 assertions
pass. Phone loading/UI/timing passed on the balanced build.

Separate-process order reversal produced conflicting tail timing results. Preserve
those logs and make no reproducible tail-latency claim from them. The follow-up
BranchEfficiencyAudit instead alternates indexed/linear lookup for each identical
query in one JVM, swapping only branch arrays through benchmark-only reflection.
Two warm-up and four measured rounds, 372 queries per strategy per mode; all output
text/score/pack/matching metadata vectors agree. Taiwanese/English median:
1.707 -> 1.326 ms; p95: 14.877 -> 11.656 ms. Japanese/English gains are small:
median 0.083 -> 0.074 ms, p95 2.458 -> 2.385 ms. These are uncached dictionary
lookup timings, distinct from Android worker callback latency.
