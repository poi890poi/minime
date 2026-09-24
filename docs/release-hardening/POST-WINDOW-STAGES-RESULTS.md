# Remaining Chinese and Taiwanese delays need different fixes

September 25, 2026. The [declared diagnostic](POST-WINDOW-STAGES-PLAN.md) completes
three phone sessions on the admitted single-window runtime. **No new production
behavior or speedup is claimed.** Combined observers locate remaining work;
their overhead prevents release acceptance or direct speed comparisons with
unhooked runs.

All 2,354 injected raw/Space updates are observed: Chinese 728, Taiwanese 1,006,
Japanese 620. Exact frozen action inventories pass. Original APK, preferences,
learning and IME are restored, display OFF is verified, both owning runners have
exited, and the extended reservation is explicitly released at 22:13 UTC.
No rotation/AOD settings changed. With the eight unhooked runs, this loop accounts
for 7,738 raw/Space actions; that is not a touch-accuracy score.

## Stage evidence

Each cell is **p95 / p99 milliseconds**, the thresholds covering 95% / 99% of
observed stage durations. Do not add percentiles. Provider counts include
cancelled work; queue/callback counts include stale deliveries. Those populations
differ in Taiwanese and remain explicit below.

| Mode | Provider work | Main-queue wait | Callback application |
|---|---:|---:|---:|
| Chinese | 5.31 / 8.51 | 5.96 / 6.49 | 12.79 / 43.72 |
| Taiwanese | 16.94 / 46.22 | 5.82 / 6.59 | 6.20 / 8.22 |
| Japanese | 1.91 / 3.32 | 6.43 / 7.75 | 6.63 / 9.05 |

Chinese accounts for all 665 started/posted/delivered requests. Taiwanese starts
894, cancels three during the pipeline, posts/enters 891 and discards three as
stale, accepting 888. Japanese starts/posts/accepts all 508. One Chinese letter
window has two callback requests and is excluded from the joined timeline as
ambiguous; it is not assigned an arbitrary callback. Request IDs remain complete.

The intentional 8 ms scheduling delay stays unchanged. Mean deadline overshoot
is 0.15–0.17 ms; mean main-queue wait is 1.97–3.22 ms. Japanese has a 30.76 ms
maximum queue wait, retained in its report. These measurements do not support
another blanket Handler or thread-priority change. Rejected scheduling trials
remain rejected.

## Chinese: font filtering dominates the worst callback

Ordinary-cadence font-filter p95/p99 is 9.08/45.87 ms, with a 62.01 ms maximum.
The general follow-up of **every** prior >100 ms observation finds one case:
the earlier essay/transposition action takes 93.48 ms in this hooked replay,
including 62.01 ms in font filtering inside a 69.93 ms callback. Provider work
is 1.48 ms and main-queue wait 1.06 ms; the result contains 2,201 candidates.
Its older unhooked observations were 101.34 and 98.07 ms. Different hooks/runs
prevent calling 101 → 93 a shipping improvement or a resolved stall.

Two fast Chinese candidate frames are unobserved in the diagnostic. Their
callbacks finish after the next key release, take 40.93 and 46.89 ms, and spend
32.68 and 36.72 ms filtering fonts over 1,928 and 2,201 candidates. Raw updates
are observed. The unhooked new-build run had no missing Chinese candidate frames;
hooked counts must not replace that result.

This locates the expensive boundary, not its internal cause. Cache lookup,
actual font queries, allocation/GC and repeated traversal need separation before
implementation. Removing glyph validation, blacklisting Unicode blocks, reducing
candidate coverage or promoting individual entries is not an acceptable shortcut.
Prior worker warming and paging results remain adverse evidence, not approved fixes.

## Taiwanese: lookup work dominates

Provider p99 is 46.22 ms and maximum 52.10 ms. Provider counters record 4,358.75 ms
in the add-on stage versus 4.44 ms in the base stage across 894 calls; Rime has
no calls. In this mode Japanese native merge returns its fallback unchanged,
pointing to add-on lookup. The expensive reading-index operation is still unknown.

All ten unobserved fast candidate frames occur after two typed letters. Three
requests are cancelled during provider work and three become stale on delivery;
their provider work is 45–52 ms. Four others finish application before the next
release but produce no observed matching frame; provider work is 40–45 ms.
They remain “changed presentation without observed frame,” not invented successes.
Slow-cadence font p99 is only 1.80 ms; fast-cadence font p99 is 0.19 ms. A common
font fix cannot explain this provider tail.

Profile the shared lookup locally before changing it. Preserve ordered candidates,
scores, evidence, paired forms and acceptance behavior. Traversal, repeated
trimming/sorting and allocation are hypotheses from code inspection, not proven
causes. Use broad frozen inputs and exhaustive short spellings, not a cache or
exception chosen for the observed two-letter cases.

Japanese is the control: all 508 candidate frames are observed, with low provider
and font costs. Source/genre and frame-stage distributions remain available.
No raw/Space/candidate observation exceeds 100 ms in these diagnostic runs.
That small hooked sample does not clear unhooked tails or large-sample gates.

## Evidence and checks

[Chinese](post-window-stages/chinese.json), [Taiwanese](post-window-stages/taiwanese.json)
and [Japanese](post-window-stages/japanese.json) retain counts, mean/median/p95/p99/max,
source/genre/condition strata, missing-frame details, reporter/raw hashes and
cleanup receipts. [Prior-outlier follow-up](post-window-stages/outlier-followup.json)
matches every earlier outlier by action identity. Raw text and telemetry stay local.
Starts have thermal status 0 and battery temperatures 33.8, 33.7 and 33.9 °C.

The package audit confirms unchanged active Java sources and all 24 language
assets. Non-signature differences from the unhooked fixture are diagnostic DEX
code and Git revision metadata. Ordinary build output is restored and contains
no test editor or observer. Test-key fixtures are not releases.

The stage-summary helper omitted p99. An independent reporting fix adds it using
the same nearest-rank convention as the other reporters, with empty-sample and
long-tail fixtures. Ten stage-report and five strict timeline checks pass;
Chinese aggregates were regenerated from unchanged raw evidence. Six queue-report
checks and the observer's chronology, terminal-state and overflow contract pass.
No phone replay was discarded.

Next: reduce the identified work through separately frozen, behavior-equivalent
experiments, then repeat unhooked cross-language and acceptance checks. Dictionary,
ranking and production scheduling remain unchanged in this diagnostic loop.
