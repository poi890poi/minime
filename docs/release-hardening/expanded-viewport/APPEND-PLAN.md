# Follow-up: configure only appended candidate views

September 24, 2026. Performance experiment; not yet admitted. Baseline accepted
runtime `1ea175a`; diagnostic comparison also retains rejected full-refresh APK.

Evidence: the first prototype opens quickly, but append-scroll submission p95 is
93–103 ms. Non-append scrolling is 18–19 ms. Its scroll callback changes a key
used by the entire strip and re-runs setters/binding on every grid view. This
rebuilds unchanged toolbar nodes and invalidates old TextView measurement.

Hypothesis: appending only new views while the candidate presentation is identical
reduces the demonstrated append stalls. Keep the same batch size, trigger point,
full engine snapshot and ordering. Separate allocation count from presentation
identity: a changed list still takes the full reconcile path; an unchanged list
only adds its tail. Keep existing gesture deferral, accessibility node identity,
composition/selection ownership and reset behavior. No data/core/font change.

Use the same frozen frame APK/workload first, with the new app only. Compare to
both measured accepted baseline and rejected prototype, then repeat accepted
baseline/new trial in reverse order if the first comparison survives. Reject if
the append change loses selection/reachability or fails to reduce the observed
stall. For admission, both orientations must have all-scroll p95 <=33 ms and
p99 <=50 ms, in addition to repeatable opening improvement and unchanged candidate
identities. These frame-submission budgets do not certify physical presentation.
Full typing-shard nonregression remains a separate requirement before landing.

This isolates unchanged-view refresh from append allocation; do not alter batch
size or scheduling at the same time. Preserve a negative result if allocation or
layout still dominates. Increasing the latency budget to admit the trial is not
an allowed outcome.
