# Java dictionary lookup: avoid unused intermediate results

The shared reading-unit lookup now materializes candidates only at the requested
whole-input boundary. Sentence conversion still collects intermediate words.
Traversal order, visited-state checks, search budgets, scores, ties, source data,
candidate metadata and binary format are unchanged.

## Cause and evidence

`ReadingUnitIndex.lookup()` used the sentence matcher's all-offset collector and
discarded every list except the last. Each discarded result could trigger sorting,
deduplication, Unicode length counting and temporary maps. No search decision
depends on those collected lists. The baseline JFR profile has 2,457 execution
samples containing reading-unit frames, including 1,052 containing `trim`.
Sampling supports the diagnosis; it is not exact CPU accounting. Some deep stacks
are truncated, so the small count reaching `AddonDictionary.lookup` is not its
share of total runtime.

## Frozen desktop comparison

13,595 inputs, three passes per process. Baseline revision dce6ddb; the profiled
first baseline is retained but excluded from speed comparisons. Two unprofiled
baseline processes and two optimized processes ran sequentially. Windows,
Microsoft OpenJDK 17.0.11, 1 GiB heap. The same existing compiled models are loaded
before measurement. The timer excludes hashing and output I/O; allocation is
current-thread bytes, not retained heap or Android PSS.

| Active add-on | Warm baseline p95 | Warm optimized p95 | Mean allocation change |
|---|---:|---:|---:|
| Taiwanese | 13.07–13.28 ms | 2.78–2.99 ms | about −81% |
| Taiwan Chinese | 0.103–0.104 ms | 0.043–0.044 ms | about −58% |
| Japanese | 0.516–0.520 ms | 0.525–0.542 ms | about −23% |

Every ordered candidate digest matches across all 40,785 records per process.
Digests include text, exact scores, readings, consumption, flags and paired forms.
Report distributions and empty-output counts by source/genre group, condition and
pack are in `results.json`; raw timings and digests are retained as compressed TSV.

**Negative result:** this does not meet the original Japanese p95 improvement
hypothesis. Its repeated p95 is slightly higher, despite lower allocation and mean
lookup time. The supported benefit is Taiwanese and Taiwan add-on speed
and reduced allocation across packs. Do not call this a fix for the entire Japanese
pipeline or change the existing 50 ms admission gate. The optional native converter
remains test-only. No language-specific exception is introduced.

Chinese numbers here measure only the Taiwan add-on, not Rime or the full Chinese
keyboard. English does not use this index as an add-on; core language-isolation
and contraction tests cover its adjacent behavior. Corpora are consumed regression
inputs, including production-derived retrieval controls; no accuracy or fresh
holdout claim is made. See [PLAN.md](PLAN.md), [NOTICE.md](NOTICE.md), and the
frozen source and model hashes in `manifest.json`.

## Validation

- Shared core: 290,078 assertions passed. New generated cases compare final
  full/partial results with the retained all-offset path and check that sentence
  composition still uses intermediate boundaries.
- Pinned desktop Rime: 13,014 inputs completed, 11,272 uncached native queries.
  This is a required regression check, not Android latency evidence.
- Both old and new APKs passed the two phone tests, with 2,550 identical ordered
  public candidate-metadata results and 192 identical pipeline acceptance results.
  Each APK also passed all 3,933 ordered desktop/native-phone comparisons.

## Phone comparison

Samsung SM-G781B, Android 13 / API 33, authorized serial RFCR91GWXLX. Run the
preserved previous APK followed by the optimized APK with the **same test APK**;
its 850 inputs are every sixteenth frozen desktop row. Each run has one first and
two repeat passes. Model loading, digest creation and report I/O are outside the
lookup timer. The first pass is retained separately in `phone-results.json`.

| Direct Java lookup, warm | Before mean | After mean | Before p95 | After p95 |
|---|---:|---:|---:|---:|
| Japanese | 0.837 ms | 0.724 ms | 2.286 ms | 2.236 ms |
| Taiwanese | 23.879 ms | 5.447 ms | 68.233 ms | 15.520 ms |
| Taiwan Chinese add-on | 0.215 ms | 0.110 ms | 0.555 ms | 0.222 ms |

Taiwanese warm p99 remains 37.97 ms and maximum 54.44 ms. This is a substantial
improvement, not complete typing-latency acceptance. Empty-output counts and all
ordered candidates are unchanged. The small Japanese p95 movement is insufficient
to claim a repeatable tail-latency improvement across desktop and Android.

The separate full Japanese burst pipeline has warm p95 **49.98 → 50.46 ms**.
The old APK happens to pass the 50 ms gate in this session, whereas the optimized
APK fails it. Prior old-APK sessions also failed. This boundary-sensitive result
does not establish a pipeline speedup or justify changing the gate. Repeat-pass
stage p95 values remain about 19–21 ms for add-on plus conversion and 21–24 ms for
main-thread dispatch. Percentile components must not be added. Native conversion
remains about 1 ms p95. No physical touch-to-display latency was measured.

End-of-test PSS was 297,392 KiB before and 318,625 KiB after; these snapshots follow
native and all-language loading and do not isolate retained dictionary memory or
GC state. Do not infer an Android memory saving from desktop allocation reduction.

`build-manifest.json` verifies the baseline is the previous final APK, records
both app/test hashes, checks unchanged compiled dictionaries and keeps evaluation
assets out of both main APKs. An initial byte-equality check caught CRLF in the
test input versus LF in the repository; the APK payload hash and identical logical
rows are recorded explicitly. Android's line reader removes those terminators.

Both sessions used fresh explicit SHINE acknowledgement and the shared phone
mutex. The previous Samsung IME and MinIME settings/learning were restored and
verified, display OFF verified, and the reservation explicitly released. No
always-on-display setting, tablet, or global ADB server was changed. Local cleanup
evidence folders: `4a94ccd9-6a1e-4eff-b360-d0f7163774db` (baseline) and
`58bd2780-3105-4c86-9c8a-8dd5d1b2091f` (optimized), under `artifacts/device-tests`.

## Reproduce

Run `prepare.py` to reproduce the exact selection; it must retain the recorded
input and model hashes. Compile `LookupBenchmark.java` with shared-core Java
sources. For baseline use revision dce6ddb in a separate checkout; for candidate
use the committed optimization. Use separate class directories, no overlapping
timing processes, and the same Java runtime/heap:

```powershell
java -Xmx1g -cp <classes> dev.minime.core.LookupBenchmark docs/java-lookup-performance/inputs.tsv artifacts/java-lookup/<variant>-<run>.tsv
python docs/java-lookup-performance/report.py
./tools/test-core.ps1
./tools/test-desktop.ps1 -Output artifacts/java-lookup/rime.jsonl
```

For sampled profiling only, pass
`-XX:StartFlightRecording=filename=artifacts/java-lookup/baseline.jfr,settings=profile`.
Export execution samples with JDK `jfr print --json --events jdk.ExecutionSample`
to `artifacts/java-lookup/profile.json`, then run `profile.py`. JFR startup/loading
is included in sampling, never in per-query timing.

The Android harness is included only with `-PjapaneseEvaluation=true`. Copy
`phone-inputs.tsv` to the generated test-assets `japanese-evaluation/java-lookup.tsv`
before building. Run `JavaLookupPhoneTest` and the existing
`JapaneseProviderPhoneTest` through `tools/test-device.ps1`, collecting both
`java-lookup-phone.jsonl` and `japanese-provider-phone.jsonl`. A fresh explicit
reservation and the phone lease wrapper are required for both APK comparisons.
