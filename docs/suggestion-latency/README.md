# Suggestion latency while typing — 2026-09-09

MinIME's shared prefix lookup repeatedly sorted every candidate accumulated so
far at every visited tree node, solely to obtain the eighth-highest score for a
search bound. Broad prefixes in optional dictionaries made this expensive. A
bounded eight-element heap now tracks that score. The complete candidate map,
512-node search budget, per-reading limit, penalties, final ordering and returned
candidate limit are unchanged. Existing entries that improve their score are
removed from the heap before their score is replaced.

This is a performance change in `ReadingIndex`, shared by core and optional
dictionary completion. It changes no source data, weights, preferences, model
format, JNI, asynchronous scheduling, UI, learning, or commit/selection policy.
The principal risk is an incorrect bound after duplicate text, changing scores
or ties; a frozen old implementation provides differential regression coverage.
There is no cache of user input and no added persistent data.

## Measured result

All times below are milliseconds. The desktop is the same Windows/Java 17 host,
with a 2 GB heap. Each desktop run has one warmup and three measured passes over
1,488 frozen queries, serially without query caching or a profiler. Phone tests
use only RFCR91GWXLX, the same Rime model, one warmup and two component passes over
93 queries; callback measurements have one pass per configuration. These are
per-source/mode samples, not traffic-weighted usage estimates. p95 uses the
nearest-rank definition.

| Measurement | Before p50 / p95 | After p50 / p95 |
|---|---:|---:|
| Desktop core conversion | 0.83 / 4.96 | 0.59 / 4.90 |
| Desktop all optional dictionaries | 3.85 / 70.22 | 1.61 / 4.77 |
| Phone core conversion | 2.04 / 9.45 | 1.62 / 9.43 |
| Phone Rime | 10.48 / 16.46 | 10.20 / 15.86 |
| Phone all optional dictionaries | 11.13 / 79.13 | 5.81 / 11.81 |
| Phone main-handler delivery, all packs | 52.70 / 111.89 | 39.99 / 58.96 |
| Phone main-handler delivery, no packs | 34.31 / 53.99 | 35.31 / 57.44 |

The last two rows exercise the actual `AsyncDecoder`, including its unchanged
8 ms scheduling delay, lookup/merge work and callback to the main Handler. They
exclude touch dispatch, CompositionEngine's callback processing, view layout and
screen refresh. Requests are sequential, so this is not a burst/backlog or
frame-latency benchmark. Keyboard stability/imprecision tests separately exercise
Android presentation and stale callbacks; they are not timing measurements.

Short prefixes account for the improvement. With all packs on the phone:

| Input condition (31 queries each) | Before p95 | After p95 |
|---|---:|---:|
| First character | 146.65 | 42.97 |
| Half input | 108.53 | 51.47 |
| Full sampled input | 54.43 | 63.81 |

Negative/control evidence: full-input callback p95 was higher in this paired run,
and disabling all packs showed no improvement. Core and native component p95
were essentially unchanged. Do not claim that every input or configuration got
faster, or infer statistical significance from these small per-condition phone
samples. This change addresses the measured short-prefix bottleneck. Rime,
scheduling, UI work and overlapping requests remain possible follow-up costs.
Combined repository/native initialization took 11.57 s before and 11.04 s after;
that is a separate cold-loading observation, not a typing latency improvement.

Initial diagnosis used JFR execution samples. Frequent stacks were TimSort and
ReadingIndex's score comparator inside `complete()`, plus allocation of sorted
HashMap value lists. That profiled baseline measured optional lookup p95 59.06 ms.
It was excluded from the paired speed comparison because profiling overhead is a
confound. A clean baseline rerun and clean candidate repeat above confirm the
effect; the earlier unprofiled candidate measured 4.07 ms optional lookup p95.

## Output and integration checks

- All 4,464 desktop query pairs have exactly equal ordered candidate fingerprints,
  counts, double score bits, consumption offsets, reading metadata and flags.
- 768 generated structural probes compare the new algorithm with the frozen
  full-sort implementation from `1c2c51f`, covering ties, duplicate text, improved
  scores, filtered candidates, absent prefixes and empty input. These are
  correctness checks, not language-model accuracy.
- `tools/test-core.ps1`: PASS, 29,000 assertions including composition, acceptance,
  Pinyin continuity, apostrophes, privacy and add-ons.
- `tools/test-desktop.ps1`: the pinned Rime 1.16.1 evaluator produced byte-identical
  output on 352 hash-selected legacy inputs (480 result rows including English
  modes), with all packs enabled. This is acceptance/output regression coverage,
  not an independent language-quality holdout or native speed improvement.
- Android baseline latency test passed; candidate latency, CandidateStabilityTest
  and HumanInputPrecisionTest passed, 10 tests total. All 372 phone component and
  callback fingerprints match the baseline (public Candidate fields).
- `tools/sources.py check` and all 19 source-contract tests passed. Only evaluation
  metadata/pins changed; production source files and add-on row hashes are frozen.

The standard streaming APK install stalled before any measurement. Its client
was stopped; restoration and display-OFF verification completed. A local copy of
`tools/test-device.ps1` used `install --no-streaming -r -t` and woke the phone
inside the existing try/finally before installation. This changed installation,
not benchmark execution. The test harness also had a compile-only error accessing
package-private Candidate.reading; phone fingerprints use public fields instead,
while desktop fingerprints retain reading metadata. Neither failed attempt is a
performance result.

After each device session, the prior Samsung HoneyBoard IME and MinIME settings
and learning were restored. Final checks: `mWakefulness=Dozing`,
`mScreenState=OFF`. AOD settings were not changed. No other device was used.

## Corpus and reproduction

`corpus.json` pins original inputs and the selected 1,488-query corpus.
`tools/make_latency_corpus.py` reproduces the corpus, Android test asset and native
acceptance subset using fixed hashes, without consulting candidate outputs.
Existing web/encyclopedic, glyph and specialist pack groups remain separate in
`results.json` and the compressed raw TSVs. These are previously evaluated,
source-derived regression materials. The historical conversation-ranking folder
does not make them natural conversation; neither conversational nor essay
representativeness is established. No production entries or phrase-specific
promotions were selected. Roles and source lineage are registered through
`sources/evaluation.json`.

To compare revisions, compile core main sources and `LatencyBenchmark.java` into
separate class directories. Keep baseline classes intact, use the same packaged
`model.bin`, then run each serially:

```powershell
java '-Dfile.encoding=UTF-8' -Xmx2g -cp <classes> dev.minime.core.LatencyBenchmark docs/suggestion-latency/inputs.tsv <output.tsv> 3
$env:JAVA_TOOL_OPTIONS='-Dminime.addons=true'
./tools/test-desktop.ps1 -SkipCompile -Classes <classes-with-DesktopEvaluation> -Corpus docs/suggestion-latency/native-inputs.tsv -Output <output.jsonl>
Remove-Item Env:JAVA_TOOL_OPTIONS
```

For the phone, build `SuggestionLatencyTest` once and run that same test APK against
both application APKs through the guarded device runner. It writes only frozen
test inputs and measurements to external app files as `suggestion-latency.tsv`.
`tools/report_latency.py` checks exact parity, computes the report, and archives
the raw exports (filenames are declared in that script).

Baseline APK SHA256:
`74b4de18e2664b308a47de189267d99b8399608504bc4f3d6efaa37e36d1ed44`.
Optimized APK SHA256:
`6f773a93158aadab86e75b56c4dd069e5c7f2f0843d8fd0a9f3d8bda1c2128d9`.
