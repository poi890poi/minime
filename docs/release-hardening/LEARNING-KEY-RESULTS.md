# Reject learning-key prefix binding

September 24, 2026. **Reject and revert the runtime trial.** Isolated lookup work
decreases, but Chinese raw-frame p95 worsens in both matched phone pairs. Preserve
accepted runtime 1ea175a; the archive contains the entire trial and its checks.
[Predeclared plan](LEARNING-KEY-PLAN.md), [rejected patch](rejected-learning-key.patch).

## Problem, change and mechanism result

LocalLearning reconstructs context/tab/reading/tab for each candidate. The trial
binds that invariant prefix once for the existing candidate sort, then reads
the same live SharedPreferences count for each choice. No snapshot, cache, data,
identity, score, persistence format or private-field change. The shared default
binding delegates to the old count method; Android supplies prefix reuse.

The trial passes 1,768,542 core assertions, including live update/clear checks
and a private-input store that throws on any read or binding. Pinned desktop
output is unchanged across 13,014 inputs and 11,272 native queries, SHA-256
72ed6f69cf0a838ec9c03a4d8de2a3e94e5991531b344ae1f173d95bff2d6bfe.
The phone mechanism test passes empty/sparse/full stores, Unicode/context
isolation and live count changes after creation, clearing and restoration.

The mechanism benchmark alternates old/new order over 20 measured rounds after
five warm-ups. Each batch performs 512 lookups, including binding cost.

| Stored vote entries | Old batch median | Bound batch median |
|---|---:|---:|
| 0 | 1.649 ms | 1.407 ms |
| 20 | 2.908 ms | 2.675 ms |
| 2,000 | 1.441 ms | 1.073 ms |

These synthetic batches are not language accuracy or end-to-end typing latency.
The difference between store sizes is not a storage-size causal estimate; JIT
and run order differ. Raw rounds and means/tails are retained in
[mechanism evidence](learning-key-mechanism/summary.json).

## Four phone typing replays

Run order A1, B1, B2, A2. Identical timing-test APK and frozen inputs, no stage
hooks. Only classes.dex/classes3.dex differ outside signing metadata. All four
tests pass: each observes all 400 raw-letter frames and 64 Space frames, or
1,856 injected actions total. No physical digitizer or panel presentation claim.

Cells show **candidate p95 / raw-editor p95 in milliseconds; timely candidate
submissions out of 50 letters**. p95 includes observed frames only; missing
candidate observations stay in the timely denominator and raw reports.

| Mode / interval | A1 | B1 | A2 | B2 |
|---|---:|---:|---:|---:|
| Chinese / 150 ms | 105.22 / 20.19; 50 | 113.10 / 21.14; 50 | 103.15 / 26.03; 48 | 101.97 / 27.25; 48 |
| Chinese / 60 ms | 73.45 / 21.55; 37 | 64.05 / 23.88; 37 | 80.50 / 35.45; 23 | 66.96 / 41.81; 25 |
| English / 150 ms | 28.91 / 31.31; 50 | 27.87 / 30.58; 50 | 35.75 / 34.90; 50 | 35.54 / 35.55; 50 |
| English / 60 ms | 28.41 / 27.78; 50 | 28.46 / 29.30; 50 | 34.96 / 31.69; 50 | 36.60 / 33.63; 50 |
| Taiwanese / 150 ms | 72.81 / 23.00; 49 | 69.62 / 20.27; 49 | 87.57 / 25.58; 49 | 94.23 / 24.74; 49 |
| Taiwanese / 60 ms | 55.93 / 23.04; 43 | 58.73 / 29.81; 40 | 72.89 / 36.52; 36 | 67.93 / 30.90; 40 |
| Japanese / 150 ms | 70.62 / 21.67; 49 | 71.68 / 21.27; 49 | 82.34 / 24.32; 49 | 81.60 / 24.09; 49 |
| Japanese / 60 ms | 67.86 / 22.72; 44 | 74.47 / 23.22; 42 | 73.92 / 30.06; 40 | 86.18 / 25.39; 40 |

Fast Chinese candidate p95 improves in both pairs, but observed candidates fall
46→44 and 31→29 out of 50. That conditional tail gain cannot erase lost frame
observations or the repeated raw-tail regression. Timely candidates are 37→37
and 23→25. Other modes are mixed, with repeated English fast raw-tail worsening.
The later pair is globally slower; thermal/power telemetry was not collected
for these comparisons and no thermal explanation is claimed.

This fails the predeclared retention rule. Both method/API changes and trial
regressions are removed from active source; their full patch is archived. The
previous accepted binary remains the runtime baseline. No new release build.

Session/binary IDs are in [learning-key-binaries.json](learning-key-binaries.json).
Raw samples, deadline counts, actual cadence and p99/max remain in each replay
directory. All guarded sessions restore original app/preferences/IME and verify
display OFF. After the separately recorded accepted-baseline diagnostic and
final environment reads, display OFF was verified again and the reservation
explicitly released to SHINE. Larger varied-input latency,
physical human touch, platform, quality and source-rights gates remain open.
