# Human input precision testing

This is a test/tooling expansion against MinIME 0.5.4 (baseline 7816734), with no
production changes. The app APK SHA-256 remains
4c77868b50f4f201fe55b429e4610ea06ff7f4ea5a115f8b94fe95ae8667f0d2.

## Repeatable coverage

| Profile | Required cases | Contract |
| --- | ---: | --- |
| Off-center taps and small drift | 1,664 | All letters, eight start/drift patterns, no lost or substituted letters |
| Thumb overlap | 10,816 | All 676 ordered letter pairs, both release orders, nonzero/reordered pointer IDs |
| Cancellation | 208 | No text from a cancelled touch, even if an UP follows |
| Intentional upward slides | 208 | Capital shortcut still works; tolerance must not swallow intentional slides |
| Outside-key initial DOWN | 208 exploratory | Record disagreement with intended key, excluded from acceptance gates |

The eight configurations cover Pinyin/English, portrait/landscape layout budgets
(360/740 dp measured widths), and font scales 1.0/1.3. Interior starts range from
12% to 88% of each key; edge starts use 2%/98% with release drift up to 2 dp.
Pair starts use 25%–75% offsets and 1 dp movement. The fixed seed is 20260908.
The Android views are attached to a fresh synthetic activity per configuration.
Virtual event timestamps exercise pointer order/geometry, not Android timers.

A separate installed-IME replay uses 20 frozen inputs: English and full,
initial-only, left-mixed and right-mixed Pinyin, with ten disjoint development and
ten holdout inputs selected by hash from the attributed conversation corpus.
It uses real waits, 15%–85% key offsets, drift, planned 35–169 ms holds, uneven
inter-key pauses, cancelled touches followed by retries, and injected typos
followed by a Backspace touch. Every intermediate spelling and composing span
is checked; an exact-input selection ends each case. No expected Chinese ranking
or special phrase is supplied to the runtime.

## Results and limits

All 12,896 required matrix cases pass. The 20 installed-IME cases pass all 185
spelling/composition checkpoints across 165 OS-injected touch sequences. Candidate
stability, double-tap caps, intentional slides/cancellation and held-delete remain
adjacent passing controls. The final machine-readable summary and timing values
are in `evidence/summary.json`; raw reports, layouts, failing pointer streams and
run identities are preserved in compressed JSON evidence.

The final matrix/candidate-control batch passes eight instrumentation tests in
20.817 seconds; development passes in 38.531 seconds; holdout plus caps, slide and
held-delete controls passes four tests in 51.989 seconds. Observed touch-hold
intervals have p50 163 ms, p95 228 ms and maximum 285 ms. Build, fixture packaging,
metric self-checks, and archived-report regeneration pass.

All 208 outside-key probes disagree with intended input: they activate a neighbor,
another control, or nothing. That is retained evidence, not counted as success
or diagnosed as a recoverable keyboard bug. Initial DOWN beyond a key boundary
does not uniquely encode the person's intention. Edit counts involving control
command names are command-string comparisons, not a human character error rate.

The earlier combined run is retained in `rejected/`: its landscape activity
detached after 1,638 cases. It is incomplete and cannot pass the report gate.
Fixing the activity ownership did not change test profiles or weaken assertions.

These are synthetic distributions, not participant measurements. They establish
behavior inside a declared envelope on one SM-G781B ARM64 phone, not a human
accuracy rate, digitizer tolerance, Google equivalence, or ranking improvement.
Landscape is a measured Android layout configuration, not a second physical
device orientation study. Actual touch-hold timings include event injection and
scheduling overhead; they are not decoder or end-to-end input latency.

## Running and reviewing

From the repository root, build `:app:assembleDebugAndroidTest`, then run:

```powershell
.\tools\test-human-input.ps1
# Bounded phases, with fresh output directories:
.\tools\test-human-input.ps1 -Phase Matrix
.\tools\test-human-input.ps1 -Phase Development
.\tools\test-human-input.ps1 -Phase Holdout
python -X utf8 tools/make_human_input_corpus.py --check
python -X utf8 tools/summarize_human_input.py --self-test
```

The runner verifies the APK and packaged fixture, records artifact/corpus hashes,
assigns a fresh ID to each phase, and rejects stale, incomplete or missing reports.
Metric controls detect injected insertion/deletion/substitution/reordering errors
and prove that exploratory errors cannot hide required failures. Test fixtures
are explicitly excluded from the app APK by `verify_apk.py`.

Only phone RFCR91GWXLX is permitted. Each phase restores the previous IME and
preferences, turns off the display, and verifies its sleep state. The final
preference read-back and device state are recorded in `phone-restoration.json`.
