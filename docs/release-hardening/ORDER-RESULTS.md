# Candidate application: query-local sort keys

September 24, 2026. Performance experiment against runtime 995b535.

Decision: **land the query-local sort keys for a modest, repeated improvement
in fast Chinese candidate availability**. Do not claim a general p95 speedup or
release readiness. Keep both measurement corrections and the negative results.

## Corrected phone result

At the actual approximately 60 ms input pace, Chinese captured candidate frames
rise from **26/50 to 31/50 in both comparison pairs** (A3/B3 and A4/B4). Across
the two sessions that is 52% to 62% of typed prefixes, a gain of 10 percentage
points. These are repeated timing observations, not independent vocabulary
examples, prediction accuracy, or human touch hits.

The observer matches the spelling when preparing a frame; its submission
callback can arrive after the next finger-up. With the stricter requirement
that submission also precede the next finger-up, the counts are **18→20** in
the first pair and **20→25** in the second, out of 50 each (38/100→45/100 total).
This distinction is included in the reporter and its independent fixture test.

Final pair below: A4 baseline versus B4 trial, run in B4/A4 order. Each cell
contains 50 letters; candidate p95 is conditional on the observed frame count.
All 400 raw-editor frames are observed in each final run.

| Mode / requested interval | Candidate frames A→B | Candidate p95 A→B | Raw-editor p95 A→B |
|---|---:|---:|---:|
| Chinese / 150 ms | 47→47 | 104.57→97.45 ms | 25.51→26.98 ms |
| Chinese / 60 ms | 26→31 | 69.71→74.62 ms | 43.39→36.46 ms |
| English / 150 ms | 50→50 | 36.17→36.51 ms | 34.27→34.47 ms |
| English / 60 ms | 50→50 | 36.14→35.80 ms | 32.86→33.10 ms |
| Taiwanese + English / 150 ms | 49→49 | 87.39→88.16 ms | 24.80→24.56 ms |
| Taiwanese + English / 60 ms | 42→44 | 64.52→76.35 ms | 36.63→33.54 ms |
| Japanese + English / 150 ms | 49→49 | 86.54→84.79 ms | 26.53→24.61 ms |
| Japanese + English / 60 ms | 48→49 | 80.64→89.85 ms | 26.77→25.83 ms |

The tail result is mixed: the slower Chinese p95 reduction does not reproduce
in A3/B3 (103.59→104.38 ms). Taiwanese/Japanese final fast p95 is worse, while
more frames are captured. Comparing only inputs observed in both members of
each pair, Taiwanese mean differences are +0.48/−0.33 ms and Japanese are
−0.25/+0.97 ms; this restricted comparison is also conditional and cannot
certify non-inferiority. No broad latency or cross-language improvement is claimed.

Chinese actual mean intervals in A4/B4 are recorded in their JSON reports;
the trial is 61.04 ms at the 60 ms setting, rather than the old 72–75 ms.
The existing 50 ms candidate p95 and physical-touch acceptance gates remain open.

[First corrected baseline](order-a3/summary.json),
[first corrected trial](order-b3/summary.json),
[final baseline](order-a4/summary.json), [final trial](order-b4/summary.json).

## Problem and cause

The old comparator recomputes the partial-match predicate, context key and
learning-store lookup while comparing candidates. A controlled ART sample
attributes 55.3% of sampled application time to sorting. This is diagnostic
attribution under a profiler, not a percentage of real typing latency.
[Trace and parser output](application-profile/profile.json).

The trial computes these keys once per list, then applies the same stable order:
whole-token choices before partial choices, learned votes descending, score
descending, original source order for ties. No ranking policy, dictionary,
default acceptance or stored data changes. Keys last for one application only.
Temporary key objects replace repeated extraction; no heap or battery gain is
claimed. [Predeclared contract and measurement corrections](APPLICATION-PLAN.md).

## Measurement findings

Initial A1/B1/B2/A2 replays seemed to improve Chinese candidate p95 at the slower
pace from 117/112 ms to 96/96 ms. At the fast setting, means improved but tail
latency worsened and B2 missed one observation. This contradiction exposed a
test defect: every injected key first waited for the main thread to locate the
view. The busier baseline received input at 74.4/75.2 ms mean intervals; the trial
received it at 72.8/72.1 ms, despite both requesting 60 ms. These are not equal
loads, so the initial speedup is not the acceptance result.

The retained test correction resolves stable key geometry before each query.
A3/B3 use the same corrected test APK and achieve approximately 60 ms intervals.
They also exposed the raw-editor observer ending at the next finger-down;
it now observes through the next finger-up, just like the candidate observer.
Final B4/A4 reverse run order and use this final observer on both app APKs.
No app binary changes between any of these pairs.

Candidate timing is injected finger-up to the frame-submission callback for
matching, ready composition, conditional on observing it before supersession.
Missing observations remain in the denominator of observation counts. They can mean late completion,
supersession or unchanged presentation; unhooked runs do not distinguish these.
They are not dictionary coverage or human touch hit rate. Mean/median/p95/max and
all raw samples are preserved in each report. p95 means 95% of the observed
latencies are at or below that value; it does not count unobserved frames as fast.

## Behavior verification and limits

- Core: 888,180 assertions pass, including 576 new ordering checks on 96
  generated lists, exact ties, numeric edges and refreshed learning votes.
  These are contract checks, not language-model accuracy.
- Pinned desktop: 13,014 inputs and 11,272 native queries pass. Baseline and
  trial output JSONL are byte-identical. This replay and the phone inputs are
  already-used development data, not fresh holdouts.
- Initial phone integration: 20 of 21 checks pass. The first-glyph assertion
  fails identically on both baseline and trial because it requires 金 despite
  valid 進/今 choices being visible. The source-derived replacement passes all
  eight selections across both decoders. The other checks were not rerun merely
  to replace the failure log; original failures are retained.
  [Cause, screenshots and corrected test](first-glyph-assertion/README.md).
- Stage diagnostic: all 301 requests delivered. Chinese learning reads drop
  from 68,432 in the earlier 100-request diagnostic to 31,564 in this 101-request
  diagnostic. Different sessions and instrumentation prevent a causal latency
  claim from these totals. [Trial stages and failing test log](order-stage-trial/stages.json).
- Assets and dictionaries inside the compared APKs are identical. Only
  classes.dex and classes3.dex differ outside signature metadata.
  [Binary identities and evaluator hashes](order-binaries.json).

Eight frozen queries produce 50 letters and eight Space actions per mode/pace.
All four modes run at nominal 150 and 60 ms intervals: 464 actions per replay.
These are controlled development prefixes, not representative Taiwanese or
Japanese conversations. OS injection bypasses the physical digitizer and frame
submission is not screen presentation. No broad language-quality, power, heap,
cross-device or release-readiness claim follows from this experiment.

Existing release thresholds are unchanged. No release package or Play upload
is produced, and no GitHub CI is used.

Every phone session restores the original APK, settings, learning and Samsung
IME and verifies display OFF. The final short window was explicitly released
after session f165c655-17bb-4c61-8e60-630fb6735771. Nine timing replays total
4,176 injected actions, including Space; the profiled diagnostic is separate.

Next: use the corrected cadence to separate worker delivery from frame
preparation under rapid input. Even with this improvement, most Chinese updates
are not submitted before the following release at the fastest pace. Do not
mistake reducing comparator work for solving the whole path. Broader language
quality, human touch and existing source/platform release gates also remain.
