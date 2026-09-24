# Follow-up: bounded row-sized append work

September 24, 2026, before the row trial. The unchanged-view refresh trial passes
20 integration checks and reduces all-scroll p95 to 50.93 / 53.49 ms, but misses
the predeclared 33/50 ms p95/p99 budgets. Append work itself is still 30.63 /
35.52 ms p95, while non-append work is below 0.3 ms. Its initial batch size is
also reused as the append increment: two conservative viewports, regardless of
how much space the next scroll actually needs.

Change only append quantity to one conservative row: viewport width divided by
the existing minimum 48 dp touch width, rounded up. Initial allocation remains
two conservative viewports, and the trigger remains one viewport before the end.
No empirical word/score threshold. Keep append-only unchanged-view behavior.

Hypothesis: spreading allocation across successive scroll updates limits a
single UI action's work enough to meet the existing budgets. Risk: fast scrolling
may catch the temporary end sooner, or expose insufficient content. The full
list must remain reachable and correctly selectable in both orientations.
Record scroll operation count/distance as well as latency to reveal any extra
user work; fewer views alone is not a success. Keep the same frozen frame APK,
integration contracts, candidate/default hash comparisons and acceptance budgets.
Require repeated baseline/trial evidence and typing nonregression before landing.
Do not combine scheduling, candidate caps or layout caching with this trial.
