# Four input modes — 0.7.0

This is a language-scope feature. It adds 中 (Chinese + English), EN
(English-focused), 台 (Chinese + English + Taiwanese POJ), and 日
(Chinese + English + Japanese). The bottom language key pairs EN with the
last mixed mode in one tap. Tap the current-mode down arrow, then a mode tab
to change that mixed mode. The idle toolbar has a mode chooser too.
[Verified phone screenshot](mode-chooser.png).

Enable 台 and 日 using their existing optional dictionary settings. Existing
enable settings are retained. Disabling a selected optional mode falls back to
中 without deleting its stored selection. Taiwan culture and geography remain
available in all mixed modes when enabled. Manual user entries remain shared
explicit overrides. Mode selection preserves unfinished input, invalidates stale
candidate gestures/callbacks, and isolates third-language learning contexts.

No production dictionary, source spelling, rank weight or per-word exception
changed. All 16 packaged assets are byte-identical to the baseline APK.

## Coverage experiment

Baseline: `c16f878`, all four optional packs enabled. Candidate: explicit modes,
same assets. A second baseline control uses each candidate's exact pack scope.
All **15,688 input/mode pairs** have identical candidate order, preferred index,
consumption and literal/supplemental flags against those same-scope controls.
Pinned desktop Rime 1.16.1 also produced byte-identical results on the existing
352-input native regression corpus. No labels or expected target strings enter
the decoder; accepted English context consists only of preceding source words.

The frozen corpus contains **3,922 conditions**. Results use exact, whole-input
target matches that are fully visible in the initial candidate row or expanded
viewport, with measured Android TextView sizes. This is not top-N rank. Literal
spelling availability and default highlight are separate fields in the raw report.
Full English spelling often equals the literal input, so its high coverage does
not establish predictive accuracy. Continuations after an empty buffer are outside
this benchmark.

Geometry was measured on RFCR91GWXLX: 1080px width, density 3, font scale 1.
Both accepted builds have a 960px candidate row, 1080 × 531px expanded viewport,
and 852px keyboard height. Font measurements for all 49,110 unique strings are
byte-identical. Other screens, font scales and landscape remain unmeasured.

Counts below are **baseline → modes**. English rows use EN; Chinese rows use 中;
Japanese and POJ rows use 日 and 台. All applicable cross-mode results are in
[results.json](results.json), including English coverage in every mixed mode.

| Source / input | Targets | First row | First page |
|---|---:|---:|---:|
| English conversations, full | 256 | 255 → 255 | 255 → 255 |
| English conversations, half prefix | 256 | 66 → 74 | 113 → 106 |
| English conversations, transpose | 204 | 167 → 168 | 174 → 174 |
| English essays, full | 128 | 126 → 126 | 126 → 126 |
| English essays, half prefix | 128 | 42 → 51 | 71 → 70 |
| English essays, transpose | 106 | 97 → 98 | 101 → 101 |
| Chinese essays, full | 416 | 225 → 225 | 231 → 231 |
| Chinese essays, half prefix | 416 | 9 → 9 | 16 → 16 |
| Chinese essays, syllable initials | 416 | 59 → 59 | 82 → 82 |
| Chinese essays, mixed syllables | 416 | 122 → 122 | 144 → 144 |
| Chinese essays, transpose | 416 | 26 → 26 | 26 → 26 |
| Japanese source retrieval, full | 128 | 78 → 78 | 81 → 81 |
| Japanese source retrieval, half prefix | 128 | 15 → 21 | 70 → 80 |
| Japanese source retrieval, transpose | 128 | 3 → 3 | 3 → 3 |
| POJ source retrieval, full | 128 | 122 → 122 | 124 → 124 |
| POJ source retrieval, half prefix | 128 | 8 → 8 | 35 → 37 |
| POJ source retrieval, transpose | 124 | 4 → 4 | 5 → 5 |

The change improves English prefix first-row coverage (25.8% → 28.9% for
conversations; 32.8% → 39.8% for essays), but English page coverage decreases.
Inspection traced some removed English-looking strings to Taiwanese entries,
including `at`, `to`, `oh`, and `in` on single-letter prefixes. These are diagnostic
examples, not production exceptions. EN now excludes that source. Mixed-mode
English prefix coverage also falls in several conditions. No ranking adjustment
was made to hide these losses.

Chinese essay half-prefix page coverage is only 16/416 (3.8%), and POJ half-prefix
page coverage is 37/128 (28.9%). Separating languages does not solve these retrieval
gaps. Transposition recovery is particularly poor for Japanese and POJ. These
results justify separate general matching/data work, not tuning complaint words.

## Provenance and limitations

- English: eight conversation documents and four essay documents selected by a
  fixed document hash from the pinned [GUM train/dev exports](https://github.com/UniversalDependencies/UD_English-GUM/tree/34d01cb603867d0c085896e8286a0fe01229fa37).
  Those exports did not train MinIME. They were acquired after the mode design,
  frozen before candidate outputs, and used as first-use local holdouts. They are
  now exposed regression data and cannot be called fresh holdouts again. Sample
  up to 32 eligible word occurrences per selected document; retain two preceding
  words as context. This is English conversation evidence, not Taiwan Mandarin.
- Chinese: 26 previously audited Taiwan.md essays, two documents per category,
  sampled Han spans of 2–12 glyphs. Original McBopomofo annotations supply
  unambiguous reference readings; no decoder generates gold. This shares lexical
  lineage with production and excludes difficult ambiguous readings, so it cannot
  establish independent natural-language accuracy. Before sampling there were
  4,435 eligible spans, 3,285 outside the length window and 4,791 with missing or
  ambiguous readings. Headings, tables, lists and frontmatter are excluded.
- Japanese and POJ: hash-selected 128 everyday entries per source pack, used only
  as seen-source retrieval probes. These are neither essays nor conversations.
- Full spelling, half-length prefix, and deterministic adjacent transposition are
  separate conditions. Chinese additionally has one initial per syllable and
  alternating initial/full syllables. Apostrophes are removed from typed English
  to test restoration. Transpositions include no-op swaps of identical letters:
  English conversation 4, English essay 1, Chinese 4, Japanese 5, POJ 5. Therefore
  the transpose aggregate is a declared perturbation condition, not pure typo
  correction accuracy or a model of all human touch errors.
- No accessible licensed Taiwan Mandarin conversation corpus was obtained.
  The [NCCU public endpoint](https://spokentaiwanmandarin.nccu.edu.tw/corpus-data.html)
  returned a suspended-site page during acquisition. [Sinica TMC](https://tmc.ling.sinica.edu.tw/guide_en/)
  requires an approved account. Independent Japanese and POJ conversation/essay
  evidence is also missing. No coverage claim is made for those missing groups.

The [manifest](coverage-manifest.json) preserves source hashes, exclusions and
document identities; [source evaluation registry](../../sources/evaluation.json)
records the roles and exposure. No data entered production from this experiment.

## Phone performance

Same phone, same test harness, all dictionaries preloaded, Rime enabled, no user
learning, one warmup plus two measured rounds. Each mode has 186 samples across
93 frozen first/half/full-prefix queries from the previous latency corpus.
Each sample rapidly types a prefix and measures the final key through shared core,
worker scheduling/lookup and main-thread result delivery. This includes queued
prefix work and excludes touch dispatch, view rendering, cold load and display
frames. It is not end-to-end typing latency or steady human typing throughput.
Runs were baseline then candidate, not randomized across independent sessions;
small differences among mixed modes are inconclusive.

| Mode | Mean ms | Median ms | p95 ms | Maximum ms |
|---|---:|---:|---:|---:|
| Old mixed, all packs | 46.82 | 44.59 | 63.97 | 89.41 |
| Old English, all packs | 37.70 | 38.94 | 53.60 | 65.36 |
| 中 | 42.89 | 40.79 | 59.17 | 69.26 |
| EN | 0.61 | 0.47 | 1.80 | 2.81 |
| 台 | 45.57 | 43.72 | 62.54 | 84.57 |
| 日 | 43.86 | 42.16 | 59.43 | 70.10 |

EN's large gain has a structural explanation independently checked in core tests:
it no longer queues inactive phonetic/add-on work. The feature does not establish
a broad Chinese typing speedup. Mode-switch duration, cold startup, memory and
frame pacing were not measured. Optional indexes still load according to enabled
settings; this feature scopes queries, not asset residency.

## Rejected implementations and verification

An initial separate 48dp mode badge reduced the composing row from 320dp to 272dp,
losing first-row hits without a dictionary change. Its results are retained in
[rejected-separate-badge-results.json](rejected-separate-badge-results.json).
The final design reuses the expand slot and existing expanded toolbar, restoring
the original width. An initial PopupWindow chooser took input focus in a real UI
test; it was removed. The final in-keyboard chooser passed that test. An early
font harness run failed because a reusable TextView lacked LayoutParams; that
failed run supplied no timings or coverage metrics.

Final validation: 29,089 core assertions, pinned desktop Rime parity, Android
build, mode migration and scoped add-on tests, real midword mode switching and
geometry checks, English typing, Chrome omnibox, candidate stability, and human
touch-imprecision regressions. Source ledger and its 19 contract tests pass.
These assertion counts are engineering checks, not language accuracy.
Every device session restored the previous IME and preferences and verified
display OFF; the final IME is Samsung HoneyBoard. No AOD settings were changed.

## Reproduction

Run `tools/test-core.ps1` and `tools/test-desktop.ps1` before Android builds.
The desktop evaluator uses the repository's pinned Rime 1.16.1 runtime and bridge.
`ModeCoverageBenchmark` is compiled with all core test/main sources by the core
runner. Invoke it with `coverage-inputs.tsv`, output `.jsonl.gz`, and `baseline`
or `candidate`; the baseline classes must come from `c16f878`, with this benchmark
harness added. Retain immutable class directories so a later compile cannot
silently replace the baseline. It reads the same generated model and assets in
both runs. Its lookup caches are for coverage only, never performance claims.

To regenerate the frozen inputs, download `en_gum-ud-train.conllu` and
`en_gum-ud-dev.conllu` from the pinned GUM commit into `artifacts/mode-corpus/`,
verify the manifest hashes, then run `python -X utf8 tools/make_mode_corpus.py`.
All other inputs are pinned repository files. Do not change the seed or document
selection after inspecting outputs. Reusing this corpus is regression evaluation.

Run `ModeRenderingMetricsTest` and `ModeLatencyTest` against baseline and candidate
APKs using the same instrumentation APK and the guarded phone test runner. Save
the exported geometry, fonts and timings as `artifacts/modes-{geometry,font,latency}-`
`{baseline,candidate}.{json,tsv,tsv}` respectively. Save the paired coverage gzip
files in `artifacts/`, then run `python -X utf8 tools/report_modes.py`.
The report asserts exact control parity and identical typography before scoring.
Committed compressed outputs allow offline inspection without reinstalling an APK.
