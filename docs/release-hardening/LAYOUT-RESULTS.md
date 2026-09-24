# Unchanged board layout: reject the runtime trial

September 24, 2026. **No production change.** Avoiding identical letter-board
layout assignments removes a demonstrated redundant Android layout request,
but does not establish a reliable typing improvement. Fast Taiwanese raw-text
p95 worsens in both pairs. Runtime remains b47bb2a, including the 8 ms lookup
delay. [Predeclared contract](LAYOUT-PLAN.md).

## Problem, cause and experiment

KeyboardView.render assigns new layout parameters to the letter-board container
on every candidate refresh, before returning for an unchanged layout. A settled
board therefore becomes layout-requested even though neither its dimensions nor
keys changed. The one-variable trial compares current width and pixel height
before assigning parameters. It changes no candidate, dictionary, ranking,
learning, acceptance, scheduling, styling or hit rectangle.

The new Android test reproduces the unnecessary request on baseline in both
portrait and landscape configurations. Both tests fail at the intended assertion.
The trial passes them, including pending typing, retained touch-target identity,
symbol height and numeric/main transitions. Candidate stability and paging tests
also pass: **12 integration tests total**. This proves removal of the unnecessary
request, not the size of its performance cost. Configuration-specific views are
tested without rotating the physical phone.

## Repeated typing comparison

Same phone and test APK; identical app assets. Only classes3.dex differs outside
signature metadata. Run order A1, B1, B2, A2; A is baseline, B is the dimension
guard. Eight frozen development queries, four modes, 150/60 ms release spacing.
Actual mean fast intervals range from 59.74 to 61.18 ms. No stage hooks are used.

Each cell is **candidate p95 / raw-text p95 in milliseconds**, then **candidate
submissions before the next key release out of 50 letters**. p95 is the value at
or below which 95% of observed latencies fall. Candidate p95 excludes unobserved
frames; those remain missing in the reports and denominator, never fast successes.

| Mode / spacing | A1 | B1 | A2 | B2 |
|---|---|---|---|---|
| Chinese / 150 ms | 93.30 / 20.44 (50/50) | 103.09 / 20.04 (50/50) | 110.89 / 36.42 (47/50) | 106.57 / 30.87 (48/50) |
| Chinese / 60 ms | 71.68 / 20.96 (34/50) | 69.34 / 24.44 (36/50) | 77.55 / 39.52 (21/50) | 76.90 / 40.32 (23/50) |
| English / 150 ms | 27.62 / 27.96 (50/50) | 27.98 / 28.98 (50/50) | 33.94 / 35.15 (50/50) | 32.45 / 34.26 (50/50) |
| English / 60 ms | 29.04 / 29.07 (50/50) | 28.54 / 28.57 (50/50) | 35.77 / 33.74 (50/50) | 35.37 / 33.40 (50/50) |
| Taiwanese + English / 150 ms | 78.14 / 23.75 (49/50) | 78.45 / 21.54 (50/50) | 92.58 / 25.57 (49/50) | 87.52 / 24.79 (50/50) |
| Taiwanese + English / 60 ms | 74.45 / 22.19 (42/50) | 71.88 / 27.34 (40/50) | 78.06 / 25.64 (37/50) | 75.98 / 35.22 (34/50) |
| Japanese + English / 150 ms | 65.92 / 19.94 (49/50) | 75.77 / 22.36 (50/50) | 81.74 / 23.10 (49/50) | 88.23 / 24.88 (50/50) |
| Japanese + English / 60 ms | 59.36 / 20.20 (47/50) | 70.04 / 24.75 (40/50) | 83.57 / 24.80 (39/50) | 79.31 / 26.02 (38/50) |

Fast Chinese timely submissions improve 55/100 to 59/100 over the two pairs;
Taiwanese fall 79/100 to 74/100 and Japanese fall 86/100 to 78/100. The Chinese
candidate p95 gain is only 2.34 and 0.65 ms. Fast Taiwanese raw-text p95 worsens
22.19 to 27.34 and 25.64 to 35.22 ms. Japanese candidate timing is mixed.

Restricting comparisons to exactly the same prefixes observed in both builds
also does not establish a general benefit: fast Taiwanese candidate mean is
1.09/0.29 ms worse, Japanese 6.98/0.11 ms worse, and Chinese 1.94 ms better then
0.27 ms worse. These conditional subsets exclude missing frames and cannot
certify non-inferiority. [Common-observation analysis](layout-common-observations.json).

The second pair is generally slower on both builds. Session variability prevents
attributing every timing difference to this guard; there is no thermal, CPU
frequency or battery diagnosis. The decision is that evidence fails the declared
benefit gate, not that the guard is proven intrinsically slow. A deterministic
reduction in requests is insufficient to override mixed visible timing.

All four replays pass, each observing **400/400 raw-editor frames and 64/64 Space
frames**: 1,856 OS-injected actions total. Submission callbacks are not physical
digitizer latency, display presentation or human touch hit rate. Missing candidate
observations can include unchanged presentation, lateness or supersession; these
unhooked runs do not distinguish them. No claim of lost dictionary coverage.

Raw samples, actual intervals, observation counts and mean/median/p95/p99/max:
[A1](layout-a1/summary.json), [B1](layout-b1/summary.json),
[B2](layout-b2/summary.json), [A2](layout-a2/summary.json).
Provider elapsed work is about 1.22/1.55 seconds baseline and 1.21/1.53 seconds
trial. These elapsed counters are not CPU/battery measurements. The final baseline
discards one stale delivery; the second trial cancels one request. No new
cancellation policy is introduced.

## Verification and disposition

- Shared core: 888,180 contract assertions pass. Pinned desktop: 13,014 inputs,
  11,272 native queries; output SHA-256 identical to the accepted runtime. These
  counts are not prediction accuracy. Production/core files are restored exactly.
- Local APK builds pass. Two expected baseline failures establish test sensitivity;
  12 trial integration checks and four timing replays pass. Original failures are
  preserved in [baseline](layout-regression-a/instrumentation.txt) and trial
  outcomes in [trial](layout-regression-b/instrumentation.txt).
- [Binary/input identities and sessions](layout-binaries.json). All six experiment
  sessions restore original APK/preferences/IME and verify display OFF. A separate
  accepted-runtime diagnostic profile follows before the final explicit release.
- [Archived trial and regression test](layout-experiment/README.md). Neither the
  rejected guard nor its intentionally failing baseline assertion enters active
  production/test source. No unrelated work is reverted.
- Inputs are reused development material, not fresh conversation/essay holdouts.
  No new release package, Play upload or GitHub CI. Existing latency, language
  quality, physical touch and source/platform release gates remain open.

The next diagnostic profiles accepted candidate application after the sort-key
improvement, rather than extrapolating from the older pre-improvement profile.
