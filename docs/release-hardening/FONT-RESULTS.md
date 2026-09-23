# Font warming trial — rejected; measurement fixes retained

## Problem, cause and trial

Chinese candidates still miss the 50 ms p95 target. Profiling separates worker
delivery, font checks, learning reads, remaining candidate application and render
callbacks. Cold font checks occupy as much as 88 ms on the main thread. This is
a real blocking cost, but identifying it does not prove that moving it will make
the final candidates arrive sooner.

The trial warmed the same bounded font-capability cache on the decoder worker,
using separate Paint copies and cancellation between candidates. Core filtering
stayed in its original position; dictionaries, ranking, candidate spans and Space
acceptance were unchanged. The cache bound remained 8,192 code points. No font
checks were removed, and no word-specific exceptions were introduced.

**Decision: reject and revert the production trial.** It reduces long main-thread
callbacks, but the visible candidate improvement does not repeat. It adds worker
work and synchronization; the final Japanese comparison also regresses. The
evidence does not justify adding that mechanism. The patch and its concurrency
test are archived, not compiled into the app.

## Results

p95 means 95% of observed measurements are at or below that delay. Each stage
column has 100 delivered Chinese callbacks. The candidate-frame row has 50
letter events at a 150 ms interval, all observed. Do not sum stage percentiles.

| Chinese measurement | Baseline 1 | Trial 1 | Final baseline | Final trial |
|---|---:|---:|---:|---:|
| Main callback p95 | 41.31 ms | 36.82 ms | 41.76 ms | 39.33 ms |
| Main callback maximum | 99.69 ms | 47.20 ms | 96.19 ms | 44.90 ms |
| Main font checks p95 | 8.35 ms | 1.96 ms | 8.33 ms | 2.91 ms |
| Request → worker/main delivery p95 | 40.25 ms | 51.10 ms | 39.39 ms | 46.42 ms |
| Key-up → candidate frame p95 | 112.57 ms | 107.65 ms | 118.96 ms | 119.94 ms |

The gain is in the main-thread tail, not an established end-to-end speedup. Final
Japanese candidate p95 rises from 70.31 to 80.60 ms at 150 ms typing and from
69.00 to 80.57 ms at 60 ms typing. The first trial did not reproduce this slowdown;
do not claim a proven universal regression. It is still adverse evidence against
landing a change without a repeated end-to-end benefit.

Final corrected key-up → editor-frame p95 (50 letters per cell):

| Mode / key interval | Baseline | Trial |
|---|---:|---:|
| Chinese / 150 ms | 22.83 ms | 22.02 ms |
| Chinese / 60 ms | 20.80 ms | 21.65 ms |
| English / 150 ms | 28.41 ms | 27.98 ms |
| English / 60 ms | 29.71 ms | 28.58 ms |
| Taiwanese + English / 150 ms | 21.43 ms | 21.29 ms |
| Taiwanese + English / 60 ms | 20.69 ms | 21.59 ms |
| Japanese + English / 150 ms | 21.16 ms | 23.33 ms |
| Japanese + English / 60 ms | 21.83 ms | 24.72 ms |

These short runs cannot establish cross-mode non-inferiority or satisfy the
large-sample acceptance contract. Injection bypasses the physical digitizer;
frame submission is not display presentation. Queries are the same eight frozen
development inputs, not representative Taiwanese/Japanese conversations. Six
replays total 2,784 actions including Space; none is a fresh coverage holdout.
No heap or battery improvement is claimed.

## Testing improvement that is retained

The old probe stopped observing candidates when the next finger went down,
although that spelling remained current until finger-up. Correcting this captures
all 50 fast Chinese baseline frames, including 20 during the following press.
Taiwanese/Japanese each have 49 observed frames plus one unchanged presentation
per interval. This corrects a false inference about missing suggestions; it does
not make the app faster. Eight reporter contract tests cover the distinction.

The final trial delivers all 300 asynchronous typing requests. Its fast Chinese
frame count is 48/50: one result finishes beyond the observation window and one
changed result has no observed frame. Do not hide those behind its conditional
75.82 ms p95, or call them proof of missing decoder output. Final baseline is
50/50 at 75.89 ms. [Measurement contract and diagnosis](CANDIDATE-STAGES.md).

## Verification and artifacts

- Shared core passes 887,604 assertions; pinned desktop passes 13,014 inputs and
  11,272 native queries. These are verification counts, not language accuracy.
- Final trial passes **19 phone tests**, including the replay, source-derived
  font parity under concurrent warming, cancellation, deep paging/selection,
  nine stability checks, mixed/initial Pinyin, English correction/spacing,
  Taiwanese paired output, Japanese joined input and lifecycle/mode switching.
- Every session restores the original APK, preferences and Samsung IME and
  verifies display OFF. The window is explicitly released to SHINE. Final
  cleanup session: `24a30acb-dcb3-403a-8dae-94461147dcd1`.
- No production change from this trial remains. No new release package or Play
  upload is produced; no GitHub CI is used. Existing release gates remain open.

Raw compressed samples, stage reports and instrumentation:
[baseline 1](stages-votes/stages.json), [trial 1](stages-font-trial/stages.json),
[intermediate baseline](stages-font-baseline-repeat/stages.json),
[corrected final baseline](stages-window-baseline/stages.json),
[corrected final trial](stages-font-final/stages.json).
[APK identities](font-experiment-binaries.json) record the exact builds; only
classes3.dex differs between production APKs outside signature metadata.
[Rejected patch](rejected-font-warming.patch) and
[archived trial test](rejected-font-warming-test.java.txt) preserve reproducibility.

Next isolate the remaining candidate-application work before trying another
optimization. In the final baseline, learning reads take 7.09 ms p95 and the
residual application stage 21.47 ms p95. Font work still matters, but merely moving
it has not shortened the full path. Expanded-grid allocation, broader language
quality, human touch, long prefix-suite completion and source-rights gates remain.
