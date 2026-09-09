# Two-language modes: decision data

Decision: English is the one fixed secondary language for both 台 and 日.
Focused-language coverage is identical between the two secondaries in the frozen
probes, while English secondary avoids Chinese decoder work. Choose Chinese instead
only if entering Pinyin without switching boards outweighs that delay. The core
supports both alternatives for comparison. The product has no secondary-language
setting or extra pairing tabs; 台/日 always resolve to their English-secondary pair.

## Phone efficiency

Same authorized phone, same 93 frozen queries, one warm-up and two measured rounds
per mode (186 measurements each). Last key to worker/main callback; not rendered
frame latency. Queries, genre/error conditions and raw timing samples are retained.

| Pair | Median ms | p95 ms | Maximum ms |
|---|---:|---:|---:|
| Taiwanese / Chinese | 49.49 | 84.55 | 114.08 |
| Taiwanese / English | 31.39 | 63.91 | 105.35 |
| Japanese / Chinese | 46.42 | 61.99 | 81.50 |
| Japanese / English | 15.75 | 39.63 | 44.06 |
| Chinese / English | 42.72 | 61.98 | 74.17 |
| English-focused | 0.36 | 1.37 | 2.69 |

English secondary reduces the median by 37% for Taiwanese and 66% for Japanese
within the balanced-build run. Taiwanese still costs 31 ms median and reached
105 ms at worst. The earlier larger-memory build had a 383 ms outlier; the final
run did not reproduce it, but separate phone sessions cannot establish that every
such stall is eliminated. Same workload, fixed mode order, not a randomized
multi-session phone trial. The previous 0.7.3 Taiwanese/Japanese three-language
p95 values were 64.3/58.3 ms; the final English-secondary values are 63.9/39.6 ms.
Keep those cross-session comparisons distinct from same-run pairing comparisons.

## Coverage and switching cost

3,922 frozen conditions per mode: English GUM conversation/essay, Chinese Taiwan.md
essays, and source-derived Taiwanese/Japanese retrieval. All stages are exposed
regression data. Full/half/initial/mixed/transposition results remain separated in
coverage-results.json. There is no independent Taiwanese/Japanese conversation gold
or Chinese conversation gold in this comparison; do not generalize it to all chat.
Ordinal optional slots exclude raw input. They are not physical row/page visibility.

| Probe group | Taiwanese / Chinese | Taiwanese / English | Japanese / Chinese | Japanese / English |
|---|---:|---:|---:|---:|
| Focused-language half reading: target in first 8 / 128 | 50 | 50 | 76 | 76 |
| Focused-language half reading: available / 128 | 84 | 84 | 125 | 125 |
| Chinese essay full Pinyin: first 8 / 416 | 225 | 0 | 223 | 0 |
| English conversation half-word: first 8 / 256 | 19 | 59 | 0 | 43 |
| English essay half-word: first 8 / 128 | 4 | 49 | 0 | 40 |

An English target can coincide with source POJ/Japanese Romanization; a nonzero
English-target count in a Chinese-secondary pair does not mean it queries English.
English secondary requires switching to 中 for Pinyin. Chinese secondary requires
switching to EN for English completion; the one-tap EN return remains available.
No combined language score silently assumes the user's code-switching proportions.

## Extensive dictionary retrieval

All existing 148,313 rows are retained; 195,456 attributed rows added. Taiwanese
POJ inventory rises from 20,274 to 80,289 distinct output forms. The authored Taihoa
source contributes all 100,983 eligible reading/output pairs (91,339 source records;
four unsupported forms excluded). There are 51,308 source-backed Han examples.
Forty-eight existing canonical Han examples change under the same source/familiarity
policy as the eligible inventory grows; dictionary membership is not spelling
consensus. Original ch/chh POJ is preserved.

Japanese retains all eligible source-common readings and compatible common forms:
22,953 full reading probes are retrievable, unchanged. Half-reading reach increases
224 -> 418 / 1,024; initials 796 -> 948 / 1,019. First-eight hits remain 191 and 766:
newly reachable alternatives are mostly farther down the candidate list. Nineteen
unsupported common reading aliases remain; grammar/inflection conversion is absent.

Taiwanese beginner full retrieval stays 5,163 / 5,163. Prefix reach rises
2,893 -> 4,285 / 5,163, but first-eight hits fall 2,893 -> 2,645. Initial reach rises
1,792 -> 2,814, with first-eight hits falling 1,792 -> 1,751. This is the measured
cost of more exact vocabulary competing with incomplete beginner readings. No
source IDs, word exceptions or weights were tuned to repair these test counts.
Taihoa full-reading retrieval rises 14,247 -> 100,973 / 100,973; 100,970 targets
are in the first eight. Half-reading reach rises 66 -> 697 / 2,047, with 249 in
the first eight. All 103,020 queries completed. These are source retrieval, not
language accuracy; the very broad short-prefix space remains weak.

## Memory and validation

The first expanded build exhausted Android's 256 MiB Java heap while building
indexes. Rejected. Sharing immutable source values and releasing temporary maps
first reduced retained desktop all-pack heap from 195 MB to 154 MB. Removing the
redundant focused exact-lookup map and sharing equal candidate lists then reduced
it to 90 MB. A small branch index adds 2.4 MB: final retained heap about 93 MB,
compared with 96 MB for the old vocabulary. No words, results or budgets were cut.
All 23,532 mode outputs match before/after each representation optimization. See MEMORY.md for cause,
limits and the failed experiment. Lookup timing and load figures are separate from
phone callback timings; no Android retained-heap measurement was available after
restoration stopped the process.

Core: 32,338 mechanical assertions; pinned desktop Rime: 628 uncached queries;
source framework: 27 sources / 67 pinned inputs / 8 provenance groups and 19 tests;
asset checks, APK build and lint passed. Both formerly failing phone tests passed on pooled and balanced builds,
including pair switches, composition, keyboard bounds and one-tap return. Every
phone session restored its prior IME/preferences, slept and verified display OFF.

A same-JVM, interleaved linear/indexed lookup comparison holds the dictionary,
queries and candidate budget constant. Four measured rounds per strategy yield
372 queries per mode per strategy. Taiwanese/English median lookup improves
1.707 -> 1.326 ms and p95 14.877 -> 11.656 ms; every candidate vector matches.
Japanese/English changes are small: 0.083 -> 0.074 ms median. Separate-process
reverse-order repetitions had conflicting tail results; those are retained and
not used to claim a reproducible p95 speedup. No faster cold-loading claim: larger
source parsing took roughly 6–10 seconds on this busy desktop versus 3.4 seconds
in the old-inventory sample. Assets load once off the typing thread.

The final fixed English mapping, family accessibility labels and QWERTY-focused
boards were built/linted and passed the final phone session (104.643 seconds).
Both selected English-secondary UI paths were exercised; the timing test also
measures the Chinese-secondary alternatives directly in the shared engine.
A final regression removes Chinese-intent suppression of English completions when
Chinese is excluded. It fails on the earlier core and passes after the scope fix.
English conversation half-word first-eight hits rise 51 -> 59 / 256 for Taiwanese
and 36 -> 43 / 256 for Japanese; focused-language hit counts stay unchanged.
The scoped stage in coverage-results.json represents the released behavior.

Reproduce with tools/test-core.ps1, tools/test-desktop.ps1, ModeCoverageBenchmark,
DictionaryEfficiencyAudit, JapaneseCoverageAudit, TaiwaneseHanAudit,
tools/audit_focused_dictionaries.py and tools/report_two_language_modes.py. Raw
outputs and source-query identities accompany this report. No claim of complete
language coverage, fresh holdout accuracy, or Google Zhuyin parity.
