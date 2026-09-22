# Candidate-strip allocation — accepted improvement, release gates still open

## Problem and cause

The collapsed strip eagerly creates, binds and measures a TextView/divider for
every candidate. The same frozen phone prefixes produce as many as 1,651 base
candidates. Existing view reuse also scans that pool linearly. Most of these views
are off-screen while the user types. This work runs on Android's main thread even
though dictionary decoding runs on a worker.

The controlled baseline → trial → baseline → final-trial sequence supports this
as a major cause of the Chinese input delay: switching back restores the large
delay. It does not establish that every remaining delay has the same cause.

## Change

Materialize at least 24 strip candidates, or enough for two viewports on a wider
display. Add another batch on forward scroll near the loaded end. A new input,
engine or mode resets the allocation. An active gesture still defers replacement.
Stale scroller callbacks cannot populate a detached/replaced strip.

The core candidate list, order, dictionary data, font checks, consumption spans,
Space choice and expanded grid are unchanged. This is a view-allocation boundary,
not a vocabulary cap. The paging test scrolls to the original final candidate,
verifies it is visibly inside the viewport, selects it, and checks that the next
input returns to a bounded allocation.

## Measured gain and cost

OS-injected key-up → editor-frame submission p95 on RFCR91GWXLX (Android 13).
p95 is the delay at or below which 95% of observed samples fall. Each cell has
50 letter events; the initial Chinese/60 baseline has 49 observed raw frames.
The same eight frozen queries are replayed at 150 ms or 60 ms between key presses.

| Mode / key interval | Initial baseline | First trial | Repeated baseline | Final trial |
|---|---:|---:|---:|---:|
| Chinese / 150 ms | 158.5 ms | 27.5 ms | 148.8 ms | 26.8 ms |
| Chinese / 60 ms | 544.6 ms | 29.6 ms | 559.3 ms | 33.2 ms |
| English / 150 ms | 30.9 ms | 33.9 ms | 31.7 ms | 33.6 ms |
| English / 60 ms | 28.5 ms | 33.7 ms | 30.1 ms | 31.9 ms |
| Taiwanese + English / 150 ms | 21.9 ms | 25.4 ms | 22.4 ms | 25.4 ms |
| Taiwanese + English / 60 ms | 19.9 ms | 24.8 ms | 20.6 ms | 24.9 ms |
| Japanese + English / 150 ms | 21.5 ms | 24.5 ms | 18.6 ms | 26.0 ms |
| Japanese + English / 60 ms | 20.9 ms | 25.5 ms | 20.6 ms | 24.5 ms |

Chinese improves substantially in both trial runs. The other modes show roughly
2–7 ms higher p95 than the bracketing baselines. Preserve this negative result:
the experiment does not prove cross-mode non-inferiority or explain that smaller
difference. Accept the large Chinese improvement with this measured tradeoff;
do not call the performance acceptance gate passed. The fast Chinese and ordinary
English final values are slightly above the 33 ms proxy target.

At 150 ms typing, Chinese fresh-candidate-frame p95 improves from 765.9 ms
(49/50 observed) and 659.0 ms (48/50) in the two baselines to 163.9 ms (50/50)
and 170.9 ms (50/50) in the trials. This still misses the 50 ms candidate target.
At 60 ms typing, matching candidate frames are missing/superseded often; report
their denominators, not a favorable conditional latency alone. Final observed
counts are Chinese 22/50, English 50/50, Taiwanese 16/50 and Japanese 37/50.
Unchanged versus superseded frames are not distinguished by the current probe.

Four short runs total 1,856 actions, including Space. This is not the prescribed
large repeated acceptance sample. Injection bypasses the physical digitizer and
frame submission is not actual display presentation. The common query list mixes
English words and Chinese readings; it does not represent natural conversations
in every language. Human touch accuracy, large-sample tails and missed-frame rates
remain unmeasured. Expanded-grid creation remains eager and can still be costly.

## Validation and evidence

- Shared core: 887,604 assertions; pinned desktop: 13,014 rows / 11,272 native
  queries. These are correctness counts, not language accuracy.
- First trial: paging/deep selection plus nine candidate-stability checks pass.
- Final trial: **14 phone tests pass**, adding initial/mixed Pinyin, English
  correction and punctuation spacing, and language-mode switching.
- The baseline's longer stored-prefix suite exceeds both bounded run budgets;
  it remains incomplete. The final 14-test run does not silently replace it.
- Every session restores the original APK/preferences/IME and verifies display OFF.

[Final integration](phone/render-final-instrumentation.txt),
[initial baseline](latency/summary.json), [first trial](latency-candidate/summary.json),
[repeated baseline](latency-baseline-repeat/summary.json),
[final trial](latency-final/summary.json). Each timing directory includes compressed
raw samples. [Binary hashes](latency/experiment-binaries.json) identify the exact
APKs: trial versus rebuilt baseline differs only in classes3.dex outside signature
metadata; assets, native libraries and manifest match. Rebuilding the baseline
changed its whole-APK hash, so that fact is explicit rather than hiding it.

Next isolate remaining candidate latency and expanded-grid allocation, and finish
the long prefix-selection test with per-case progress. Keep coverage and candidate
precision separate from this rendering-only improvement.
