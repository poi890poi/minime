# 0.7.8 verification and remaining limits

Core: **35,052 correctness assertions**, plus the pinned desktop Rime evaluator
(628 native queries). These counts are not language accuracy. Both loading
experiments used reversed trial order and preserved all outputs/metadata in
23,532 paired lookups each. The 3,922-input corpus contains separate English
conversation/essay, Chinese essay, and Japanese/POJ source-retrieval groups;
it is reused diagnostic data, not a fresh holdout.

Android build and lint succeeded: **0 errors, 17 existing warnings**. APK checks
verify the pinned base/Rime models, every partitioned source row in order, and
the exact prebuilt index files. Final APK: 76,936,211 bytes. The three prebuilt
optional assets were identical between the audit and Android build outputs.

The final prebuilt APK passed **20 targeted integration tests** in 44.502 s:
scoped cold loading, warm reuse, disabling, persisted choices, private/editor
boundaries, worker cancellation, candidate identity/stability, Japanese basics
and paired POJ/Han interactions. Earlier editor and expanded-list controls also
passed. The failed real Japanese case and deterministic accessibility test are
preserved, along with their corrected results; failures were not discarded.

Only RFCR91GWXLX was used. Every session, including both failures, restored
settings and learning XML with readback, restored Samsung Honeyboard, then slept
the display and verified OFF. Preference contents and complete system display
dumps are not committed. No always-on-display setting was changed.

## Touch response

The final diagnostic completed in 92.914 s: 400 letter actions and 64 Space
actions, with all 400 expected raw-text checkpoints and all 64 Space frame
callbacks observed. It injects OS events into the actual visible IME and measures
frame-commit **callback delivery** in a same-process test editor. It does not
measure physical contact/presentation, Chrome, human imprecision hit rate, or a
fresh independent language corpus. Each mode/pace has only 50 letter samples.

| Mode | DOWN interval ms | Raw callback p95 ms | Candidate callback p95 ms | Observed candidate callbacks |
| --- | --- | --- | --- | --- |
| Chinese/English | 150 | 22.04 | 121.50 | 50/50 |
| Chinese/English | 60 | 39.26 | 45.56, **one sample only** | 1/50 |
| English | 150 | 29.58 | 27.86 | 50/50 |
| English | 60 | 28.28 | 28.17 | 50/50 |
| Taiwanese/English | 150 | 21.27 | 84.81 | 29/50 |
| Taiwanese/English | 60 | 20.87 | 77.24 | 12/50 |
| Japanese/English | 150 | 20.37 | 83.49 | 29/50 |
| Japanese/English | 60 | 19.84 | 81.91 | 25/50 |

All intervals originate at injected UP; DOWN-to-UP dwell is separate. Candidate
numbers are conditional on observing a matching frame before the next action.
The diagnostic cannot separate unchanged rows from superseded updates, so absent
callbacks are not all classified as dropped predictions. Full means/medians/p95/
p99/maxima and denominators are in `touch-final/summary.json` and its raw samples.

**Performance acceptance is not certified.** Fast Chinese raw response remains
above the 33 ms p95 target in this proxy, and Chinese suggestions reach 165 ms.
The Taiwan/Japanese candidate results also do not establish the 50 ms target.
These findings prevent a general typing-speed PASS; no test-session duration or
cold-load reduction is presented as a typing-speed improvement. The prebuilt
loader fixes initialization (POJ 17.58 s to 0.462 s in the recorded phone runs),
and cancellation avoids obsolete downstream work, but remaining steady-typing
costs require their own controlled performance work.

## Scope not changed

There is no corpus expansion, new focused-language phrase joining, grammatical
continuation, or new Han-annotation extraction policy in this release. Explicit
focused-choice preferences are fixed; missing words are not invented by learning.
The duplicate-removal ranking experiment remains rejected because it had losses
as well as gains. All production dictionary rows and static ranking rules remain.

Recompute final timings with:

```powershell
python -X utf8 tools/report_touch_latency.py --input artifacts/mode-mechanism-review/touch-final.tsv --output docs/mode-mechanism-review/verification/touch-final --build 'b52f07c / 0.7.8'
```
