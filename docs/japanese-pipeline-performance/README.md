# Full Japanese pipeline: remove repeated and excluded-language work

Baseline revision: 8f2003b. The first change moves fixed regular-expression compilation
out of typing calls in intent classification, kana transduction, character lookup,
add-on separators and reading-unit validation. Each call still creates its own
matcher. Patterns and matching semantics are identical; no matcher or input text
is shared or retained between requests.

The final phone whole-burst warm p95 is **43.70 ms** and **42.82 ms** in separate
sessions, versus **50.55 ms** before. Both final sessions pass the unchanged
50 ms aggregate gate. Every final first/repeat pass also has p95 below 50 ms.

The old path compiles the intent pattern for each typed key and validates every
kana segment with another compilation, repeated again for native conversion's
whole reading. The desktop profile contains regex compilation frames, but only
70 samples including startup; it is insufficient to apportion Android CPU time.
The controlled APK comparison is the evidence for the phone effect.

The second change passes the already-selected Chinese-enabled flag into intent
classification. When Chinese is excluded and the input is not an English word,
the previous result always became Latin intent after core coercion. Pinyin
legality and completeness calculations cannot affect that result and are skipped.
English ambiguity, technical tokens, case checks and Zhuyin retain their original
paths. The public legacy classifier overloads retain Chinese-enabled behavior.

The 8 ms coalescing delay, cancellation, traversal budgets, ranking, candidate
limits, acceptance, source data and native converter are unchanged. Native model
activation remains instrumentation-only; this performance result cannot establish
independent ranking quality. Main APKs contain no experimental native model assets.

## Core comparison

Use all 64 clauses from the prior 1,311-key phone stream, not a timing-selected
subset. Four fresh JVM processes run baseline/candidate/candidate/baseline; each
has one first and nineteen repeat passes. Deferred core dispatch runs the actual
CompositionEngine typing path and final lookup/delivery, but excludes Android
scheduling and native conversion. Java 17.0.11, Windows, 1 GiB heap.

Warm dispatch p95 is 0.344–0.375 ms before and 0.337–0.338 ms with regex reuse. This is a small
desktop effect. Warm lookup/delivery p95 ranges overlap (0.305–0.318 ms before,
0.316–0.318 ms after). First-pass distributions and outliers remain in the report;
they are not a first-use improvement claim. Phone performance must stand on its
own measurements. Adding the excluded-language shortcut reduces dispatch p95
to **0.138–0.143 ms** in two further fresh processes, versus the preserved regex-only
variant. The same raw input and dictionary assets are used for every variant.

All 7,680 pipeline candidate-digest/preferred-index records match. All 40,785
multilingual lookup metadata records also match the preceding frozen reference,
covering 13,595 complete/partial/error inputs over three passes. Full candidate
digests include text, scores, flags, reading alignment, consumption and paired
forms. These are consumed regression inputs, not new language accuracy evidence.
Source/genre breakdowns are retained separately in the reports.

## Validation and reproduction

### Phone results

Authorized Samsung SM-G781B, Android 13 / API 33. The same test APK replays the
same 64 clauses, with the actual AsyncDecoder, CompositionEngine and test-only
native provider. Each session has one first and two repeat passes.

| Variant | Warm mean | Warm p95 | Warm p99 | Maximum |
|---|---:|---:|---:|---:|
| Original | 34.16 ms | 50.55 ms | 60.53 ms | 64.49 ms |
| Regex reuse | 31.94 ms | 48.22 ms | 59.84 ms | 61.07 ms |
| Regex repeat | 32.52 ms | 50.33 ms | 56.10 ms | 60.05 ms |
| Final focused classifier | 28.27 ms | 43.70 ms | 53.11 ms | 57.36 ms |
| Final separate repeat | 28.04 ms | 42.82 ms | 54.65 ms | 57.62 ms |

Regex reuse alone fails repeat acceptance; preserve that negative result. Final
first-pass p95 is 47.63 and 46.39 ms. Main dispatch repeat p95 drops from roughly
19.5–22.7 ms to 14.1–16.8 ms. Add-on/conversion stage counters still include
superseded work and cannot be added to dispatch percentile columns.

Across five sessions, each native comparison matches all 3,933 ordered results.
All 768 cross-session pipeline acceptance/count comparisons match the original.
Space still accepts the displayed winner, and cancellation/provider-disable
checks pass. No model or ranking edits were needed.

**Remaining tail limitation:** the small casual-chat subset has only 19 clauses
(38 warm observations). Its p95 is 51.73/54.65 ms after versus 45.93 ms before,
although its mean improves from 28.68 to 25.28/24.61 ms. Both upper observations
are the same single-key clause; its worker stage already cost 33.6–39.8 ms before
and 34.7–39.7 ms after, while dispatch remains about 0.3 ms. This is a remaining
worker bottleneck and a sparse-subset tail, not evidence that all inputs finish
under 50 ms. Task-dialogue p95 improves from 51.64 to 43.70/42.82 ms. The aggregate
gate passes; a per-source 50 ms p95 gate would not pass for casual chat. No word
exception or budget change was introduced to make this diagnostic input faster.

The five instrumentation runs passed, but the first final-build wrapper falsely
rejected its primary-display OFF report. [DISPLAY-CHECK.md](DISPLAY-CHECK.md)
records the failure, parser correction, offline controls and separate verified
recovery. The final repeat fully passed cleanup. Every operated session restored
the prior Samsung IME and MinIME preferences; final display OFF was verified, the
phone mutex released and SHINE explicitly notified of release. No AOD setting,
tablet or global ADB restart was involved.

Evidence folders under `artifacts/device-tests`: original
`6c81072b-b9ff-42fc-944b-d4364adc431b`; regex
`ca38372f-d95c-418f-9d0b-33c24ce7aecd`; regex repeat
`ba4860cf-61b1-431c-a70b-ed3cef285050`; final
`f79e2d10-c16a-440a-bfda-06337bda4393`; final repeat
`829835e0-7c3a-4eb3-b093-58a786395c45`.

### Core and source checks

Core: 306,303 assertions passed, including the existing 24,989 differential kana
probes, language isolation, completion and cancellation/acceptance checks. Pinned
desktop Rime completed 13,014 inputs / 11,272 uncached native queries. Source
catalog and 19 source-contract tests pass; production source ledger and compiled
dictionary bytes are unchanged. The new intent regression compares enabled and
excluded Chinese classification with the prior explicit coercion over generated
Latin, uppercase, separators, Zhuyin, newline and numeric inputs in both English
contexts. It verifies that English/Pinyin ambiguity is exercised and preserved.

Use `prepare.py` to reproduce the final raw clauses from the pinned stream. Compile
`PipelineBenchmark.java` and the previous `LookupBenchmark.java` with shared-core
sources into separate baseline/candidate/focused class directories. Baseline uses
8f2003b; candidate is regex-only commit 33350d5; focused adds the classifier shortcut.

```powershell
java -Xmx1g -cp <classes> dev.minime.core.PipelineBenchmark docs/japanese-pipeline-performance/clauses.txt artifacts/japanese-pipeline/<variant>-<run>.tsv 20
java -Xmx1g -cp <candidate-classes> dev.minime.core.LookupBenchmark docs/java-lookup-performance/inputs.tsv artifacts/japanese-pipeline/lookup.tsv
python docs/japanese-pipeline-performance/report.py
./tools/test-core.ps1
./tools/test-desktop.ps1 -Output artifacts/japanese-pipeline/rime.jsonl
```

The profiling run uses 100 passes with JDK flight recording; export execution
samples to `artifacts/japanese-pipeline/profile.json` and run `profile.py`.
Profiling timings are excluded from the controlled comparison.

Build the Android app and existing test harness with `-PjapaneseEvaluation=true`
and `-Pandroid.injected.build.abi=arm64-v8a`. `build-manifest.py` checks APK hashes,
baseline identity, native asset isolation and dictionary identity. Under a fresh
explicit phone reservation, use `tools/test-device.ps1` to run the existing
`JapaneseProviderPhoneTest` and collect `japanese-provider-phone.jsonl`. Preserve
old/new/repeat raw reports and run `phone-report.py`.

See [PLAN.md](PLAN.md), [NOTICE.md](NOTICE.md), `manifest.json`, `results.json` and
the compressed raw evidence. No physical touch-to-display measurement is implied.
