# Append-only refresh helps, but large batches still stall

September 24, 2026. **Reject as a release change.** The unchanged-view hypothesis
has supporting evidence, but this implementation misses its predeclared scroll
budget. Accepted production runtime remains `1ea175a`.
[Plan](APPEND-PLAN.md); [first prototype](RESULTS.md).

Change: keep the toolbar and existing measured candidate views when an identical
list gains another batch. Candidate data, ordering, batch size and trigger stay
unchanged. The same frozen frame APK and 12-prefix/two-pass workload run on the
phone. All 20 integration checks pass, and every complete list/default signature
matches the accepted reference. Reaching the tail takes the same 344 portrait
and 254 landscape scroll steps as both earlier implementations.

All timings below are milliseconds to the first submitted GPU frame after the
programmatic action. They exclude physical touch and panel presentation.

| Orientation | Accepted scroll p95 | Full-refresh trial p95 | Append-only p95 / p99 | Append-only open p95 |
|---|---:|---:|---:|---:|
| Portrait | 16.32 | 83.07 | 50.93 / 54.89 | 51.43 |
| Landscape | 18.79 | 85.08 | 53.49 / 58.69 | 52.61 |

The earlier full refresh spends 93–103 ms p95 on scroll steps that add views;
append-only reduces that to 55–59 ms. Work within the append action itself still
takes 30.63 / 35.52 ms p95. Non-append action work remains below 0.3 ms and its
frame submission p95 is 19.28 / 16.90 ms. This points to batch creation as the
next isolated experiment. No repeat is justified for admission when the first
run misses the 33/50 ms p95/p99 budgets.

Deep selection p95 is 73.69 / 73.73 ms, versus 63.46 / 67.99 ms in the earlier
accepted baseline. These are different sessions; the change does not touch the
selection path, but it cannot claim nonregression from these numbers. All phone
state was restored, final display OFF verified, and the window explicitly released.
Endpoint environment and raw samples are retained in `expanded-append-frames-b1`.
`rejected-large-append.patch` preserves the exact prototype. No typing replay or
production dictionary changes were made for this rejected candidate.
