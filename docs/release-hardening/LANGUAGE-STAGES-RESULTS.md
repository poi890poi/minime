# Candidate delivery needs finer queue measurements

September 25, 2026. **Diagnostic result; no production speedup or release
acceptance.** Three bounded phone replays pass on the accepted release-mode app
with the existing stage observer. Runtime/data/scheduling are unchanged. Original
APK, preferences, learning and Samsung IME were restored after every session;
actual display OFF was verified and ownership explicitly released at 18:54 UTC.

## Problem and controlled measurement

The [unhooked baseline](LANGUAGE-BASELINE-RESULTS.md) finds candidate p95 above
50 ms in Chinese, Taiwanese and Japanese, while raw/Space tails meet their budgets
in that sample. It cannot identify the responsible stage. The
[declared diagnostic](LANGUAGE-STAGES-PLAN.md) adds only three test entry points
to the unchanged CandidateTimingProbe and reuses the complete shard-0 inventory.

The runs cover 144 queries, 2,066 letters and 288 Space actions: **2,354 injected
actions** at both 150/60 ms requested release intervals. All action sequences
match the frozen source/genre/condition inventory. All raw/Space submissions are
observed. These are synthetic events, not human touch or displayed pixels.

Correlation uses mode, spelling and the time window between key releases. One
Chinese letter has two matching requests and is explicitly excluded from the
joined timeline, rather than selecting an arbitrary callback. Repeated spellings
cannot be joined across episodes. Missing callbacks/frames retain their own
denominators. Thirteen report-contract fixtures pass; instrumentation builds
locally and all three device tests pass.

## Where time is spent

The table reports **p95 milliseconds**: 95% of observed durations are at or below
that value. Percentiles from separate stages must not be added. Delivery includes
the 8 ms schedule delay, worker queue/computation and main-thread queue; it is
not a worker-only duration. Callback application includes font checks and the
view update call, but not later frame submission.

| Mode / interval | Request to main delivery | Callback application | Font checks within callback | Candidate submission observed |
|---|---:|---:|---:|---:|
| Chinese / 150 ms | 27.98 | 15.31 | 7.26 | 332/332 |
| Chinese / 60 ms | 30.13 | 10.10 | 3.33 | 327/332 |
| Taiwanese / 150 ms | 34.80 | 6.78 | 1.20 | 446/447 |
| Taiwanese / 60 ms | 32.77 | 5.52 | 0.11 | 436/447 |
| Japanese / 150 ms | 31.12 | 7.07 | 1.22 | 254/254 |
| Japanese / 60 ms | 30.84 | 5.92 | 0.09 | 254/254 |

Across delivered requests, mean request-to-delivery is 18.92 ms Chinese,
20.98 ms Taiwanese and 19.44 ms Japanese. Aggregate provider elapsed work per
call averages approximately 1.81, 5.20 and 0.80 ms respectively (base plus add-on
stages). Rime has zero calls under the accepted default. These means use different
delivered/provider populations when work is cancelled, so subtracting them does
not produce an exact per-key queue measurement. They do establish that the
recorded delivery interval is substantially broader than dictionary work alone.

Chinese has a 61.32 ms maximum font-check episode; its tail still deserves
attention. Taiwanese/Japanese font checks average about 0.15 ms and callback
application averages 2.67/3.43 ms. A blanket font or sorting rewrite is therefore
not supported as the common explanation for all three languages. After-callback
frame preparation/submission adds further time and is preserved in the timelines.

All 665 Chinese and 508 Japanese requests are delivered. Taiwanese delivers
888/894: four are cancelled during the pipeline and two discarded as stale on
delivery. Among the 11 unobserved fast Taiwanese candidate frames, six correspond
to undelivered requests, three to late callback completion, one to unchanged
presentation and one to changed presentation without an observed frame. The
single unobserved slow Taiwanese frame has unchanged presentation. Chinese's
five unobserved fast frames include four late callbacks and one changed row
without an observed frame. None is labelled a missing dictionary entry.

## Decision and limits

Keep production unchanged and measure the queue boundaries next. The current
probe cannot distinguish delayed worker start from main-queue waiting, and
changing Handler behavior or removing debounce now would be speculative. The
earlier [zero-delay rejection](SCHEDULING-RESULTS.md) remains valid; this is not
permission to silently retry it or weaken raw/Space protection.

The next [queue-boundary diagnostic](QUEUE-STAGES-PLAN.md) is frozen separately.
It must preserve scheduling and stale-result behavior while measuring the exact
intervals before choosing a production intervention.

Hooks perturb timing. Different thermal starts (29.6/31.7/33.5 C, status 0) and
sessions prevent a speedup claim against the older unhooked baseline. One shard
per mode does not certify the large-sample, human-touch, Chrome/Keep, Android
16/16 KB, source-rights or Play-delivery gates. No final release package is made
from the fixture APK.

Aggregate evidence is in `language-stages/`: separate `*-timeline.json`,
`*-stages.json`, and `*-touch.json` files retain counts, source/genre/condition
strata and available mean/median/p95/p99/max statistics. `binaries.json` verifies
the unchanged app, test identity and all 24 production assets; `sessions.json`
records provider work and cleanup display hashes. Raw queries, preference
backups, timestamps and Android dumps remain local.
