# Taiwanese provider lookup profile

The phone's Taiwanese tail is inside dictionary lookup. A desktop profile of
the unchanged packaged POJ dictionary identifies repeated candidate trimming
as a useful target for a separate experiment. No runtime change is admitted here.

The [frozen plan](PROVIDER-LOOKUP-PROFILE-PLAN.md) uses all 1,960 prefixes of
224 previously exposed Taiwanese timing queries and all 18,278 lowercase ASCII
strings of one to three letters. Each JVM initializes once and measures three
passes: 60,714 lookups per run. The plain and profiled runs preserve every
candidate's order and metadata across all 121,428 measured lookups. This is
behavioral equivalence, not language accuracy or a fresh holdout.

| Unprofiled pass | Corpus p95 | Corpus p99 | Structural p95 | Structural p99 |
| --- | ---: | ---: | ---: | ---: |
| 1 | 5.488 ms | 18.308 ms | 0.979 ms | 3.134 ms |
| 2 | 5.435 ms | 18.112 ms | 0.974 ms | 3.067 ms |
| 3 | 5.479 ms | 18.281 ms | 0.975 ms | 3.043 ms |

Of 1,305 CPU samples, 1,258 have lookup on their stack and 47 are outside it.
619 of the lookup samples (49.2%) include `ReadingUnitIndex.trim`. That is a
sampling share, not an exact elapsed-time fraction. Trimming sorts and removes
duplicate texts after each terminal state, allocating a new hash set each time.

Allocation samples associated with lookup have 12.39 GB of sampled weight;
the trim body and its duplicate-removal lambda account for 6.80 GB (54.9%).
Hash-map nodes and backing arrays dominate the sampled classes. These weights
estimate allocation traffic over the run; they are **not retained memory**.
The 20 top-level GC pauses total 38.45 ms. Collection and pause events describe
the same pauses and are not added together. Harness fingerprinting and output
also allocate, so whole-run GC cannot be attributed exclusively to lookup.

The profiler perturbs timing: corpus p99 rises to 21.34–21.71 ms. Its timings
are diagnostic only. No events were lost and no stacks were truncated; one
allocation sample lacks a stack and remains explicitly unassigned. The first
JFR attempt failed before measurement because the default temporary directory
was inaccessible. The separate successful run uses workspace-owned scratch
directories and preserves the failed attempt in the ledger.

[Aggregate evidence](provider-lookup-profile/summary.json) retains all strata,
event counters, input and raw-output hashes, and limitations. Raw traces and
query outputs stay local. [The next experiment](REUSED-DEDUP-PLAN.md) will reuse
invocation-local deduplication storage while requiring exact selection identity
and paired unprofiled latency improvements. HotSpot profiling cannot establish
Android latency, memory safety under load, or release readiness.
