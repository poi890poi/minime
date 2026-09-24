# Main-thread window work delays queued candidates

September 25, 2026. Diagnostic finding; no production change admitted.

The complete Chinese shard-0 capture contains all **665 queued requests and 665
callbacks**, with no trace health warnings and no unknown thread-state duration
inside those requests. The frozen workload validator passes. The checked exclusive
interval analyzer reproduces the exploratory totals exactly; 14 interval/report
contracts pass. Core passes 1,770,165 assertions and the pinned desktop evaluator
retains output SHA-256 `1d2aa3693ae0ddbaf1c259252fc4d9689fcd458dfb23a214f9ed29c476fcde5d`.
Those are contracts/equivalence checks, not language accuracy measurements.

Queued-result waiting averages **8.996 ms**, with p95 **18.673 ms** and p99
**24.600 ms**. During the summed waiting intervals, the main thread is running
28.11%, runnable but off-CPU 18.09%, sleeping 53.76%, and in other states 0.04%.
Frame wall time overlaps 89.13% of waiting. Frame overlap and thread states are
different measurements and must not be added together.

| Deepest traced main-thread activity | Share of summed request waiting | Mean overlap per request |
|---|---:|---:|
| Window relayout, initial and subsequent | 56.52% | 5.085 ms |
| `postAndWait` | 16.98% | 1.528 ms |
| Untraced gaps | 9.90% | 0.890 ms |
| Other traversal work | 9.20% | 0.827 ms |
| View layout | 0.96% | 0.087 ms |
| View measurement | 0.55% | 0.049 ms |

The table lists selected activities, not an exhaustive partition. Full exclusive
shares are in [work.json](system-queue/work.json). Each instant is attributed to
the deepest active trace slice; nested slices are not added twice. The denominator
is 5,982.166 ms summed over 665 request waits, not total app runtime or keystrokes.
Overlapping request waits count once per affected request. Sleeping does not prove
a synchronization barrier. The final open thread state begins after every measured
request and is excluded; an open span overlapping measurement fails analysis.

`KeyboardView.placeAnnotation()` resizes a separate composition PopupWindow when
its measured text width changes. This is a plausible owner of the relayout cost,
but these system slices alone do not identify the window. The next experiment
suppresses only that popup in a diagnostic overlay. Removal is not a proposed
product change: the raw-text affordance must be retained in any later design.

## Capture failures retained

- The first direct config-file invocation was denied by the device tracer. Passing
  the config through stdin resolved access without changing device permissions.
- A non-streamed discard capture retained only 81/665 markers and reported losses.
- Streaming discard buffers retained all markers but raised configuration warnings.
- Ring buffers with periodic flush had no warnings, but installing APKs consumed
  the fixed trace budget: only 495/665 markers remained.
- The first test-scoped start hook failed because its closure lacked the deadline.
  The existing restoration path still restored the original phone state.
- Explicit captured context and starting after installation produced the complete
  capture. The trace retained its 180-second and 1 GiB hard bounds.

Incomplete/warning captures are not used for attribution. The raw system trace
and CSV remain local. Published aggregates and hashes contain no typed corpus.
The original APK/preferences/learning/IME were restored, the owned trace was
stopped and removed from the phone, display OFF was verified, and the reservation
was explicitly released. [Evidence identities](system-queue/sessions.json).

This is an instrumented synthetic same-process editor session on one phone.
It cannot certify uninstrumented latency, physical touch, Chrome/Keep behavior,
or release readiness. Accepted runtime and all 24 language assets are unchanged.
