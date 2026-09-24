# Decoder scheduling: keep the 8 ms delay

September 24, 2026. **Reject the zero-delay production trial.** It helps fast
Japanese candidates, but does not provide a consistent general improvement.
Fast Chinese raw-text p95 is worse in both comparisons. Production remains at
b47bb2a with its existing 8 ms delay; no language-specific delay exception is
added. [Contract and prepared trial](SCHEDULING-PLAN.md).

## Problem, hypothesis and cause

Fresh candidates still arrive late while typing. AsyncDecoder intentionally
waits 8 ms before starting a query, allowing near-simultaneous requests to
collapse into the latest one. The trial removes only that wait. The single
worker, cooperative provider cancellation, stale-result guard, ranking,
learning, dictionaries and acceptance logic are unchanged.

Both diagnostic builds deliver their ordinary replay requests (301/301 baseline,
300/300 trial). Late frames therefore cannot be called missing dictionary
results. In the hooked baseline, Chinese request-to-delivery p95 is 37.12 ms and
the main callback p95 is 37.52 ms. These stages overlap with other event work and
their percentiles must not be added. Removing the wait does not remove the
remaining callback/font/render work. Immediate completion could interfere with
raw-frame preparation, but that explanation is a hypothesis, not an isolated
causal finding. [Baseline stages](schedule-stage-a/stages.json),
[trial stages](schedule-stage-b/stages.json).

## Repeated phone comparison

Same app assets, test APK, eight frozen development queries, four modes, and
150/60 ms requested finger-up spacing. Run order: A1, B1, B2, A2. A is the 8 ms
baseline; B is zero delay. No stage hooks are present in these four replays.
Mean actual fast spacing is 59.46–61.56 ms across cells.

Each cell below shows **candidate p95 / raw-text p95 in milliseconds**, followed
by **candidate submissions before the next key release out of 50 letters**.
p95 is the value at or below which 95% of observed timings fall. Candidate p95
is conditional on observing a ready matching frame; missing observations are
preserved in the linked reports and are not counted as fast or correct.

| Mode / spacing | A1 | B1 | A2 | B2 |
|---|---|---|---|---|
| Chinese / 150 ms | 102.79 / 20.33 (50/50) | 102.37 / 23.69 (50/50) | 112.58 / 29.60 (48/50) | 92.90 / 26.15 (48/50) |
| Chinese / 60 ms | 75.50 / 23.29 (32/50) | 76.29 / 30.40 (35/50) | 72.96 / 41.50 (26/50) | 82.09 / 44.91 (27/50) |
| English / 150 ms | 27.36 / 27.55 (50/50) | 28.12 / 30.50 (50/50) | 36.98 / 32.89 (50/50) | 36.30 / 35.46 (50/50) |
| English / 60 ms | 28.20 / 28.23 (50/50) | 28.46 / 30.30 (50/50) | 31.16 / 34.02 (50/50) | 31.73 / 33.41 (50/50) |
| Taiwanese + English / 150 ms | 65.64 / 21.47 (49/50) | 76.25 / 20.82 (50/50) | 86.95 / 25.38 (49/50) | 88.90 / 23.27 (50/50) |
| Taiwanese + English / 60 ms | 67.29 / 20.78 (47/50) | 64.86 / 30.04 (44/50) | 69.70 / 34.70 (37/50) | 82.79 / 32.15 (41/50) |
| Japanese + English / 150 ms | 63.13 / 21.18 (49/50) | 63.67 / 24.23 (50/50) | 81.95 / 23.66 (49/50) | 56.47 / 28.28 (50/50) |
| Japanese + English / 60 ms | 73.89 / 21.61 (44/50) | 61.43 / 37.32 (47/50) | 75.83 / 27.25 (38/50) | 64.69 / 28.56 (46/50) |

Fast Chinese timely submissions improve only 58/100→62/100 across the two
pairs, while its raw-text p95 worsens 23.29→30.40 and 41.50→44.91 ms.
Fast Japanese candidate p95 improves about 11–12 ms in both pairs, but raw-text
p95 worsens, particularly in the first pair. Taiwanese results are mixed.
English also varies despite its synchronous path being unchanged, which warns
against attributing every millisecond to this one variable. These small sessions
do not establish statistical non-inferiority or release acceptance.

Each unhooked run captures all **400/400 raw-editor frames and 64/64 Space
frames**. This is OS-injected input to frame-submission callback, not physical
touch latency, display presentation or human hit rate. Candidate observed-frame
counts and strict deadline counts are separate; a frame prepared while a spelling
is current can have its submission callback arrive after the next release.

Raw data, mean/median/p95/p99/max, actual cadence and work counters:
[A1](schedule-a1/summary.json), [B1](schedule-b1/summary.json),
[B2](schedule-b2/summary.json), [A2](schedule-a2/summary.json).

## Extra-work audit: counts alone are insufficient

Ordinary runs start 300–301 base/add-on stages each. Recorded provider elapsed
work is approximately 1.20/1.60 seconds in A1/A2 and 1.28/1.50 seconds in B1/B2.
There is no reproducible ordinary-work increase in this small comparison.
One A2 callback is discarded by the stale-delivery guard; no stale callback is
accepted by the controlled cancellation tests.

The initial continuous 4 ms stress stream starts 44 base lookups instead of one
per 50 requests. Because this is not a plausible sustained human rate, a paired
condition was declared before execution: 4 ms within each pair, 150 ms between
pairs. Both builds then run the complete extended matrix with one warm-up and
two measured rounds. Main callbacks are deliberately withheld throughout each
burst, so these tests measure obsolete work and cancellation, not typing UX.

| Extended condition, 50 requests/round | Baseline base calls | Trial base calls | Baseline provider elapsed work | Trial provider elapsed work |
|---|---:|---:|---:|---:|
| Continuous 4 ms | 1, 1 | 41, 37 | 21.51, 17.22 ms | 183.16, 176.50 ms |
| Continuous 12 ms | 47, 47 | 47, 47 | 409.31, 376.51 ms | 386.24, 370.98 ms |
| Paired 4/150 ms | 25, 25 | 50, 50 | 549.59, 536.11 ms | 432.73, 476.60 ms |

The paired condition doubles base-call count **but reduces measured provider
elapsed work**. This is counterevidence to treating call counts as CPU or battery
cost. We do not claim a practical energy regression. Continuous 4 ms work does
increase, and queued cancellation remains useful, but the primary reason to
reject the change is the mixed ordinary latency and repeated Chinese raw-frame
slowdown. Every burst delivers only its final callback, with zero obsolete
callbacks accepted. [Baseline matrix](schedule-paired-a/decoder-burst.tsv),
[trial matrix](schedule-paired-b/decoder-burst.tsv). Earlier diagnostic burst
results are preserved in the stage folders.

## Verification, cleanup and remaining work

- 888,180 shared-core contract assertions pass; 13,014 pinned desktop inputs and
  11,272 native queries pass with byte-identical desktop output. These counts
  do not measure language accuracy.
- Local builds pass. Eight phone sessions pass **14 instrumentation tests**:
  six timing replays (2,784 injected actions including Space), real-provider
  burst tests, stale-delivery cancellation and close behavior. Runtime Rime is
  off in this timing configuration; no native-Rime latency claim is made.
- All phone sessions restore the original APK, preferences and prior IME and
  verify display OFF. Final session b1677ca8-2b21-485a-b3d5-3ccd5e165465 is followed
  by explicit release to SHINE; no clients remain queued.
- [APK identities and session map](schedule-binaries.json). Only classes3.dex
  differs outside signing metadata. The archived one-line trial remains a
  reproduction artifact; no zero-delay runtime edit remains in the checkout.
- No dictionary, ranking, settings default, release package, Play upload or
  GitHub CI change. Input data is already-used development material, not a fresh
  conversation/essay holdout. No heap, battery, physical-touch or quality gain
  is claimed. Existing release gates remain open.

Next target: isolate the remaining main-thread candidate application and frame
preparation cost while preserving early raw-text feedback. Do not globally
remove useful coalescing merely to shorten the request timer.
