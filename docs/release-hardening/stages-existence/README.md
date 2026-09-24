# Remaining callback work and measured environment

September 24, 2026. Diagnostic on accepted runtime 1ea175a after rejecting
learning-key binding. The same frozen eight-query replay runs with the existing
test-only stage hooks. This is not an unhooked latency comparison or a release
pass. Preserve [stage samples](stages.tsv.gz), [frame samples](samples.tsv.gz),
[summaries](stages.json) and [environment/binary identity](environment.json).

| Mode | Request-to-delivery p95 | Callback p95 | Font checks p95 | Render callback p95 | Vote reads p95 | Residual callback p95 |
|---|---:|---:|---:|---:|---:|---:|
| Chinese | 48.38 ms | 45.96 ms | 15.17 ms | 14.22 ms | 6.98 ms | 20.91 ms |
| Taiwanese + English | 42.99 ms | 14.26 ms | 1.02 ms | 11.59 ms | 0.34 ms | 2.11 ms |
| Japanese + English | 39.64 ms | 15.78 ms | 2.02 ms | 11.12 ms | 0.47 ms | 2.34 ms |

Each percentile belongs to its own distribution; **do not add percentiles**.
The delivery interval includes queue/debounce/worker and main-thread waiting,
not just dictionary lookup. Callback includes ranking, font checks and render
work plus test instrumentation. Vote timing calls run once per read and alter
the workload. English does not use this asynchronous callback in this replay.

301 requests include one Chinese setup request; 298 delivered. Fast Chinese has
31/50 observed candidate frames, 11 ready after the observation window, two
undelivered and six changed results with no observed frame. These are observation
classes, not lost physical taps or missing vocabulary. All raw/Space frames are
retained separately in summary.json. Provider aggregate work is 506.37 ms base
and 1,087.61 ms add-ons over 301 calls each; Rime is off. Those sums over the
entire replay must not be described as per-keystroke latency.

Thermal status is **1 before and after**; Android calls this LIGHT. Battery
temperature rises from 35.4 to 36.1 Celsius; USB is connected, battery is 100%,
and low-power mode reads zero. One CPU0 frequency snapshot differs before/after;
it does not establish a frequency trajectory or causal throttling. Status has
no override. These snapshots cannot explain the earlier paired runs, which had
no corresponding thermal measurements. [Android thermal-status reference](https://developer.android.com/reference/android/os/PowerManager#THERMAL_STATUS_LIGHT).

The next measurement should first isolate application cost **without** method
tracing or per-candidate timing hooks, using the same dictionary candidates and
real LocalLearning. The sampled profile exaggerated neither by assumption nor
by proof: its overhead prevents treating its operation shares as real typing
cost. Compare that result with end-to-end frames before adding another runtime
mechanism. Future frame runs must retain thermal/power observations and report
warm-device behavior separately; do not select only favorable cool runs.

All operations, including environment reads, were inside the shared mutex and
acknowledged window. Original APK/preferences/IME restored, display OFF verified
after final reads, and the reservation explicitly released. No production change.
