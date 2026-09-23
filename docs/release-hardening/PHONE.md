# Phone audit — September 22–23, 2026

Runtime baseline fd728d5, Android 13, RFCR91GWXLX, 4 KB memory pages. Release
tests use an equivalent non-debuggable payload with the installed test certificate;
comparison, external-app and telemetry runs use the debug editor/instrumentation.
Neither is a Play-delivered upgrade test. All sessions use the acknowledged SHINE
handoff and mutex, restore MinIME preferences and the previous IME, and verify
display OFF. Release-runner sessions also restore the original APK.

## Candidate usefulness against Google Zhuyin

All 48 provider/case observations completed: 24 cases for each keyboard. The plan
was frozen by hash selection before Google observations. It reuses inspected core
inputs and is not a fresh holdout. Google learning persists; MinIME is reset per
case. Do not generalize this small sample to population accuracy.

| Requested task / visible first row | Google Zhuyin | MinIME |
|---|---:|---:|
| Chat excerpts: intended whole text available | 6 of 12 | 2 of 12 |
| Chat excerpts: whole text or usable prefix available | 10 of 12 | 10 of 12 |
| Chat excerpts: task-compatible candidate slots | 16 of 49 | 14 of 46 |
| Essay excerpts: intended whole text available | 1 of 6 | 0 of 6 |
| Essay excerpts: whole text or usable prefix available | 2 of 6 | 3 of 6 |
| Essay excerpts: task-compatible candidate slots | 3 of 24 | 3 of 23 |

Expanded visible grids have a task-compatible choice in 12/12 Google versus
11/12 MinIME chat tasks, and 5/6 versus 4/6 essay tasks. These counts describe the
visible viewport, not every candidate reachable by scrolling. Compatibility means
the intended text or its prefix. It does **not** label all alternative homophones
as meaningless. Candidate text alone does not prove correct consumed-input spans.

Google holds Chinese preedit inside its own IME and leaves the editor empty until
acceptance. The report therefore separates literal-editor spelling verification
from end-to-end UI usefulness after the requested, acknowledged injected touches.
The latter includes input delivery effects and must not be called decoder-only
accuracy. All 18 Google Chinese preedits are unverified by the literal-editor rule;
that does not mean 18 missed touches.

Six English word/prefix tasks per keyboard also completed. Candidate ranking is
unscored because Chinese accessibility parsing and raw-recovery provenance cannot
establish English dictionary hit rates. Both commit the two complete references.
Both preserve valid shortened words on Space; Google also changes two ambiguous
prefixes to other words while MinIME preserves them. A withheld reference word is
not automatically the correct Space behavior for a valid shorter word.

[Frozen plan](phone-plans/manifest.json), [raw observations](phone/observations.json),
[derived report](phone/comparison.json). Reproduce with `tools/report_release_phone.py`;
six contract tests guard duplicate/missing observations, unverified input and
unsupported accessibility extraction. These are reporter checks, not accuracy.

## External editors and release payload

- Corrected release harness: **15 tests pass**, including four visible mode
  selections, actual key taps, candidate display and acceptance. Earlier mislabeled
  English output is explicitly rejected in the evidence.
- Chrome textarea: **9 of 9** observed mode/query combinations have candidates.
- Keep new-note title/body: **18 of 18** have candidates. Fields were cleared.
- English completion and next-word results are consistent across these editors
  for the tested probes. This small audit cannot establish complete vocabulary
  coverage or behavior in every app/version.
- Five synthetic touch-handling and nine candidate-stability checks completed
  before the first prefix-suite timeout. A partial run is not a suite pass.

See [Chrome](phone/chrome.json), [Keep](phone/keep.json) and
[release modes](phone/release-modes.json). No message was sent.

## Baseline latency: a release blocker

The same eight frozen queries are replayed in all four modes at 150 ms and 60 ms
intervals. There are 50 letter events per mode/interval plus eight Space events:
464 actions total. This single session measures OS-injected key-up to editor
callback/frame submission. It omits the physical digitizer and actual screen
presentation. The queries mix English words and Chinese readings; they are not
representative Taiwanese/Japanese conversational workloads.

| Mode | Raw frame p95, 150 ms typing | Raw frame p95, 60 ms typing |
|---|---:|---:|
| Chinese | 158.5 ms (50/50 observed) | 544.6 ms (49/50) |
| English | 30.9 ms (50/50) | 28.5 ms (50/50) |
| Taiwanese + English | 21.9 ms (50/50) | 19.9 ms (50/50) |
| Japanese + English | 21.5 ms (50/50) | 20.9 ms (50/50) |

p95 is the delay at or below which 95% of observed samples fall. These are not
means. Chinese fails even this narrower proxy for the 33 ms raw-display target.
Historical probe limitation: its candidate observation ended at the next DOWN,
not the next UP that changes the spelling. The following fast-typing missing
counts are therefore under-observation, not proof of missing suggestions. See
[the correction and fresh baseline](CANDIDATE-STAGES.md). Raw-editor timings are
unaffected.

At 150 ms typing, Chinese fresh-candidate frame p95 is 765.9 ms (49/50 observed).
At 60 ms, only 11/50 matching candidate frames are observed; its conditional
53.4 ms p95 is **not** evidence that faster typing improves responsiveness.
Superseded and unchanged frames are not distinguished by this telemetry.

[Baseline telemetry](latency/summary.json) retains all missing counts and raw samples.
The separate [render trial](RENDER-TRIAL.md) investigates eager off-screen view
creation without changing dictionary coverage or candidate order. Acceptance still
requires larger repeated samples, tail/missed-frame checks and human touch trials.
