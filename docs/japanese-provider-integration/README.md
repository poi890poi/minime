# Japanese provider: phone integration and admission results

The optional converter now runs through the shared core and Android worker in an
explicit test build. On the authorized Samsung SM-G781B (Android 13 / API 33),
native warm p95 is **1.01–1.03 ms**, p99 **2.06–2.20 ms**. All **3,933 ordered
desktop/phone comparisons** pass after a second deterministic-order fix.

The surrounding pipeline still misses the frozen whole-burst p95 <= 50 ms gate:
repeat passes measure **51.3 ms** and **54.0 ms**. Keep the converter disabled in
ordinary builds. Passing native timing and acceptance does not establish the
quality or responsiveness of the complete shipping keyboard.

## Integration boundary

[CORE-CONTRACT.md](CORE-CONTRACT.md) describes input eligibility, fallback, merge,
cancellation and acceptance. The existing focused-language dictionaries and partial
predictions remain. No production entries, model weights or source costs changed. A final review also
replaced surface-based kana classification with explicit transliteration identity:
an attested kana word keeps lexical priority even when it equals generated kana.
The prior merge fails that regression; the correction passes.

No Japanese model/library enters main APK assets; `-PjapaneseEvaluation=true`
adds it and the JNI test adapter only to the instrumentation APK. Build manifest
checks verify this separation and the exact native test-library bytes.

The JNI adapter retains the indexed converter's graph, costs, beam width 50 and
top-eight budget. It does not call the rejected lexical predictor. Loading is
separate from typing and measured at 321 ms in the final run (390 ms initially).
App PSS was approximately 95 MiB after native loading and 238 MiB after the Java
dictionary/pipeline stage. Those snapshots include different app responsibilities;
they do not establish the native model's isolated memory increment.

## Remaining ordering defect and causal fix

The initial phone run repeated one mismatch in each of three passes: tied seventh
and eighth choices swapped. The earlier A* insertion ordinal was insufficient
because forward beam pruning still used cost-only `std::sort`. Equal-cost order
is unspecified and differed between desktop libstdc++ and Android libc++.

`portable-beam.patch` changes only that beam sort to `std::stable_sort`, preserving
source order for equal costs. A synthetic 80-node beam fixture fails on the old
implementation and passes on the new one. It does not add any word exception.
The first diagnostic input is preserved in the raw regression stream; it is not
used to select dictionary entries, weights or thresholds.

All 18,730 consumed conversation/full/partial/error/encyclopedia regression records
were rerun on desktop. There are 132 changed candidate-list records relative to
the previous stable-A* variant, and **zero changes beyond candidate lists**:
recorded target ranks, hit/completion outcomes, action counts and committed output
remain identical. The initial and final phone references retain identical typed
inputs. Final phone parity is 3 × 1,311 queries with zero ordered differences.
This supports the tie fix; it is not fresh language accuracy evidence or a proof
that every upstream operation is deterministic on every platform.

## Phone timing breakdown

Each native pass replays the same 1,311 keys from 64 previously frozen development
clauses. JNI gets the original jaconv kana stream for an unchanged platform
comparison. The real core/pipeline test uses the existing WanaKana transducer.
No claim is made that the two transducers behave identically.

| p95 metric | First pass | Repeat 1 | Repeat 2 |
|---|---:|---:|---:|
| Native query | 1.054 ms | 1.032 ms | 1.008 ms |
| Java/JNI call | 1.075 ms | 1.053 ms | 1.030 ms |
| Whole burst → result | 57.046 ms | 51.291 ms | 53.950 ms |
| Last key → result | 40.174 ms | 36.226 ms | 34.659 ms |
| Main-thread burst dispatch | 24.118 ms | 20.193 ms | 24.811 ms |
| Add-on lookup + conversion stage | 27.793 ms | 26.649 ms | 24.868 ms |

Pipeline passes each contain 64 whole-clause bursts. Stage times are per-clause
aggregate worker counters and can include superseded work; percentile columns
must not be added together. The 8 ms coalescing delay remains. Last-key timing
excludes earlier keys in the same burst, whereas the original admission metric
includes them. Both are retained: measuring last-key latency does not turn the
failed whole-burst gate into a pass. Warm pipeline maxima are 56.6 and 59.4 ms;
all native warm queries finish below 2.72 ms. Full distributions are in
phone-summary.json, with per-query logs retained.

The next performance investigation is the Java add-on/dispatch path, whose stage
cost is substantially larger than native conversion. No physical touch-to-display
latency was measured here. Existing synthetic touch-precision tests passed, but
they cannot establish real-user touch accuracy or responsiveness.

## Validation, failures and device care

- Shared core: 289,500 assertions passed; pinned Rime: 13,014 inputs completed.
- Initial Android run: 65 tests, 60 passed, four failures and one error. This is
  retained as a failed run. One failure was native ordering; four were outdated
  interaction expectations, diagnosed against existing production contracts.
- Focused repeat: all five corrected tests passed. A final native-only rerun after
  the kana-identity correction also passed. Every one of its 192 pipeline
  clauses accepted the displayed winner; disabling/superseding the provider did
  not deliver old Japanese results into the subsequent English query.
- Existing editor, async cancellation, candidate stability and synthetic human
  input tests passed in the initial run. See [test maintenance](INTERACTION-TESTS.md).
- Initial pipeline p95 57.7 ms and final warm p95 54.0 ms both remain admission
  failures. There was no threshold tuning, dictionary tuning or new holdout claim.

Only RFCR91GWXLX was operated, under acknowledged SHINE windows and the shared
Windows mutex. The prior Samsung IME and MinIME settings/learning preferences
were restored and verified, and the display was slept and verified OFF after
every operated session, including the failed APK-path attempt. Always-on-display
settings were untouched. All reservations were explicitly released.

Automatic approval review rejected one repeat attempt because its previous
reservation might have expired. No device operation occurred on that rejected
attempt. A fresh explicit SHINE acknowledgement was obtained before retrying.

## Reproduce

Use the pinned modern Python/dependencies and source preparation from the original
benchmark. Preserve original source copies; never apply the patch to production
dictionary data. NDK 27.2.12479018, Clang 18.0.3, arm64-v8a, API 29 native target;
desktop comparison uses GCC 10.3.0. No new upstream download is needed.

```powershell
python docs/japanese-determinism/prepare.py
python docs/japanese-provider-integration/prepare-portable.py
./docs/japanese-provider-integration/build-desktop.ps1
python docs/japanese-provider-integration/prepare-stream.py
./docs/japanese-provider-integration/build-native.ps1
./tools/test-core.ps1
./tools/test-desktop.ps1
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest '-PjapaneseEvaluation=true' '-Pandroid.injected.build.abi=arm64-v8a' --offline
# Obtain a fresh explicit phone window before invoking test-device.ps1.
# ABI-injected debug APKs are under app/build/intermediates/apk/.
```

See [PLAN.md](PLAN.md), [NOTICE.md](NOTICE.md), manifests and raw result files.
Source registration remains evaluation-only; normal release vocabulary and
provenance ledgers are unchanged. Production activation still needs a fresh
independent ranking holdout and complete keyboard latency acceptance.

The test runner checks APK paths before device access, supports a bounded timeout,
and collects named reports inside the lease. ABI-injected debug APKs are emitted
under intermediates/apk; the initial missing-path attempt is preserved as a failure.
The final test APK includes the upstream code/data licenses and corpus notice.
