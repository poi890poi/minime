# Floating composition window accounts for repeated relayout

September 25, 2026. Decision: pursue a single-window replacement; do not ship
the diagnostic removal. [Frozen plan](ANNOTATION-ABLATION-PLAN.md).

Only the composition popup's placement is suppressed. Fonts, text updates,
dictionary assets, decoder, 8 ms worker delay, ordinary Handler and frozen
Chinese shard are identical. The same instrumentation APK is used. Both traces
account for all 665 queue/callback pairs, with no health warnings or unknown
thread-state intervals. Both replays match all 728 frozen actions; all 664 raw
letter submissions and 64 Space submissions are observed per replay.

| Measurement | Popup baseline | Popup suppressed |
|---|---:|---:|
| Window relayouts during measured interval | 729 | 0 |
| Summed relayout wall time | 4,099.38 ms | 0 ms |
| Relayout overlap per queued request | 5.086 ms | 0 ms |
| Mean queued-result wait | 8.996 ms | 1.408 ms |
| Queue-wait p95 | 18.673 ms | 5.371 ms |
| Queue-wait p99 | 24.600 ms | 6.431 ms |

The result supports the popup as the cause of the repeated window relayout in
this workload. Its text-dependent width sends a window update every time the
spelling changes, competing with result delivery on the main thread. The
ablation eliminates those relayouts without changing prediction work.

This is one instrumented same-process editor pair, not an uninstrumented shipping
speed estimate or physical touch measurement. Removing the popup also removes
the visible raw-text control and is therefore unsuitable for release. The next
[single-window trial](SINGLE-WINDOW-PLAN.md) retains that control and exact touch
routing; it requires functional and repeated uninstrumented performance checks.

The phone's original APK, settings, learning and Samsung IME were restored;
display OFF was verified. The owned trace was stopped, pulled and removed from
the phone, and the shared reservation was explicitly released. Raw traces remain
local. [Aggregate comparison](annotation-ablation/comparison.json),
[package identities](annotation-ablation/binaries.json),
[cleanup evidence](annotation-ablation/session.json).
