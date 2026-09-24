# Smaller increments: latency and scroll effort must both pass

September 24, 2026. **Neither geometry trial is admitted.** All experimental
KeyboardView changes and paging-only integration fixtures have been removed from
the active tree; the patches preserve them. Accepted runtime remains `1ea175a`.
The baseline-compatible frame harness stays available for later work.

## One row per append

[Frozen plan](ROW-PLAN.md). This changes only increment quantity from the previous
append-only prototype. Initial allocation and prefetch position stay unchanged.
All 20 integration checks pass. The same frame APK completes 48 episodes with
unchanged full candidate/default identities and correct visible-tail selection.

| Orientation | Scroll p95 / p99 ms | Scroll steps | Accepted reference steps |
|---|---:|---:|---:|
| Portrait | 24.19 / 25.88 | 728 | 344 |
| Landscape | 31.48 / 33.73 | 342 | 254 |

The timing gate passes, but the small increments cannot supply enough new content
for a three-quarter-viewport scroll. The temporary end clamps movement, forcing
more actions. This is a user-effort regression, so the change is rejected without
an admission repeat. No physical fling or human touch claim is made.

## One actual viewport per append

[Frozen plan](VIEWPORT-APPEND-PLAN.md). Derive the increment from the actual scroll
height and the existing minimum touch rectangle. Keep all other behavior and the
test APK unchanged. A new cooled baseline/trial pair measures:

| Orientation / action | Baseline p95 ms | Trial p95 ms | Trial p99 ms |
|---|---:|---:|---:|
| Portrait / open | 448.90 | 52.27 | 56.85 |
| Portrait / scroll | 19.96 | 36.20 | 40.13 |
| Portrait / select | 67.20 | 70.00 | 75.78 |
| Landscape / open | 424.75 | 45.91 | 54.98 |
| Landscape / scroll | 18.22 | 38.32 | 40.56 |
| Landscape / select | 69.98 | 72.12 | 76.12 |

The full lists/defaults match, all final selections succeed, and scroll counts
and offsets return to the exact baseline sequence in every episode: 344 portrait,
254 landscape. However, the fixed 33 ms p95 scrolling budget still fails in both
orientations. Do not relax that budget to admit a preferred design. The second
repeat, paging integration and broad typing replay are therefore unrun for this
last implementation; the frame tests alone are not a general integration PASS.

Every number here is a sequential programmatic action to submitted GPU frame,
not physical contact or displayed pixels. Endpoint conditions are recorded per
session; they do not eliminate frequency drift. Each session restores the original
APK/preferences/IME, verifies display OFF and explicitly releases phone ownership.

The experiments establish a tradeoff, not a production improvement. A subsequent
redesign would need to reduce view creation/layout cost while preserving scrolling
and accessibility; stacking scheduling heuristics onto these rejected prototypes
is not supported. Keep the null production change and move to independently
valuable source/core release work.
