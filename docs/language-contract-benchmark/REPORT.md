# Shared suggestion-contract experiments

**Decision: implement the acceptance and English-context contracts in separate production changes; reject the tested English-default and deduplication variants; retain the remaining prototypes for more evidence.** This turn lands benchmark tooling and evaluation data only. No combined variant was tested.

Baseline **5dd9bb8**, production code **d353be4**, MinIME **0.8.2**. Six one-variable source snapshots were frozen before evaluation. The original 21,598 conditions produce 27,136 mode-condition evaluations; the new conversation corpus adds 61,941 conditions per variant. Read [PLAN.md](PLAN.md) and [REPRODUCE.md](REPRODUCE.md) for causal boundaries and reproduction.

## Decisions by prototype

| Prototype | Original 27,136 comparisons | Decision |
|---|---|---|
| `span` | Zero candidate-order/default changes; acceptance controls below improve | Support a production fix with an explicit raw-span contract and regression tests |
| `english-context` | Zero typing-order/default changes; continuation improves on all English-enabled boards | Support a separate context/learning dispatch change |
| `evidence-rank` | 537 default gains, 36 losses; candidate lists unchanged | Reject this fallback-based default rule; it sacrifices intended focused-language defaults |
| `dedup` | 83 order changes; no reference default/top-3/top-8 gain or loss | Reject as a behavior-preserving change: new corpus also loses six previously retrievable targets |
| `unit96` | One additional complete match, no measured loss | Retain: small targeted benefit, not a phrase-coverage solution; long-input latency still unmeasured |
| `prefix-cache` | Exact order/default parity | Retain: median English work improves, but tail speedup is inconsistent |

The new-corpus paired outcomes and baseline coverage are in [Conversation report](conversations/REPORT.md). The default policy adds just one correct default and loses 100. Dedup changes 900 orders and loses six previously retrievable historical Taiwanese targets beyond the first eight; it is not behavior-preserving. Their bounded-index interaction needs a separate causal investigation before reconsideration. The corpus exposes a much larger word-to-phrase gap than source-entry lookup controls. Do not use pooled gains to approve the ranking policy.

## Acceptance and continuation

Acceptance uses 256 independently generated kana-oracle strings, four input forms, tap/space/stale-selection actions, and normal/private fields: **6,144 mechanical controls**. Baseline passes **3,124/6,144**; `span` passes **6,144/6,144**. For explicit prefix taps, uppercase, title case and hyphenated inputs lose their suffix in all **1,536/1,536** baseline controls; the prototype preserves it in all. Stale-selection controls already pass and remain passing. These are mechanical checks, not language accuracy.

Normal partial choices move into the focused-language learning namespace; private controls make zero learning calls. This narrow prototype still uses existing Pinyin-oriented filtering and separator trimming elsewhere. A production change needs language-provider span metadata and shared validation, not an assertion that every composition contract is already universal.

For **532 natural English context/target pairs per board**, the existing provider has alternatives for 458 and contains the next source word in its first eight choices for **118 (22.2%)**. Baseline exposes these only in English mode. The context prototype exposes the same results in Chinese/English, Taiwanese/English and Japanese/English, taking each mixed board from **0/532 to 118/532** target hits. English mode remains 118/532. Private fields retain zero learning calls. This is dispatch improvement, not improved prediction probability; persistence, correction, autocorrection and spacing were not redesigned.

The default-policy experiment gains 537 English defaults but loses one English, 21 Japanese and 14 Taiwanese defaults on the original corpus. It uses the kana-fallback score as a prototype evidence marker and changes only the preferred slot. Neither that score sentinel nor an English-first rule is a suitable shared production evidence contract.

## Desktop typing latency

Three separate JVM runs, reversed variant order in the middle pass. Table entries are the median of each run’s **repeat-pass p95**, milliseconds per typed key. Each mode uses 192 hash-selected full inputs, every prefix, two cycles, uncached providers. Exact per-sample timings and stage decomposition are committed; output-audit cache timings are excluded.

| Variant | Chinese/EN | English | Taiwanese/EN | Japanese/EN |
|---|---:|---:|---:|---:|
| baseline | 9.540 | 0.260 | 14.886 | 0.796 |
| span | 9.486 | 0.262 | 14.084 | 0.929 |
| english-context | 9.498 | 0.269 | 15.149 | 0.806 |
| evidence-rank | 9.374 | 0.288 | 14.069 | 0.844 |
| dedup | 9.527 | 0.298 | 14.203 | 0.786 |
| unit96 | 9.411 | 0.273 | 14.103 | 0.920 |
| prefix-cache | 9.465 | 0.266 | 14.116 | 0.780 |

Baseline repeat-pass distribution (median of three per-run statistics):

| Mode | p50 | p95 | p99 | Maximum |
|---|---:|---:|---:|---:|
| chinese | 5.110 | 9.540 | 11.275 | 59.401 |
| english | 0.054 | 0.260 | 0.505 | 0.977 |
| taiwanese_english | 3.089 | 14.886 | 15.387 | 24.679 |
| japanese_english | 0.120 | 0.796 | 1.001 | 1.375 |

The English cache reduces median repeat work from roughly 0.054 to 0.028 ms, but p95 is roughly 0.260 versus 0.266 ms. The hypothesized tail-latency benefit does not reproduce. Its filled-cache heap and multithread contention were not measured; there is no basis to ship it as a responsiveness fix.

Chinese includes native IPC. First-pass files retain startup outliers separately; load files retain model-loading time and JVM heap. This harness loads all add-ons plus geography, unlike an active-mode-only budget. Corpus preparation overlapped portions of the timing run, so small differences remain inconclusive. Desktop timing excludes Android debounce, queueing, rendering and touch; it does not certify the 20/30 ms applied or 50/80 ms visible p95/p99 requirements. Long-input latency beyond the 24-character timing sample is not measured.

## Cache working-set tradeoff

A separate workload toggles Chinese and Japanese six times, then changes focused language nine times. Three JVM repetitions compare keeping all loaded add-on packs with keeping Chinese plus the last focused pack. Forced-GC retained heap excludes the base model and geography.

| Policy / phase | Cache misses per run | Median mean load, ms | Median peak add-on heap, MiB |
|---|---:|---:|---:|
| keep-all / fast-toggle | 2 | 23.16 | 17.27 |
| keep-all / focus-changes | 1 | 13.38 | 59.52 |
| warm-pair / fast-toggle | 2 | 21.06 | 17.27 |
| warm-pair / focus-changes | 5 | 46.84 | 50.78 |

Retain this as a resource-policy proposal: a smaller warm set can save memory but reloads discarded focus packs. Loading must remain asynchronous and preserve the current visible snapshot. This desktop experiment does not certify Android mode-switch responsiveness or native-library memory.

## Remaining evidence and gates

Staged publication/cancellation, pixel-level stability, raw-span adapters for every provider, Japanese grammar conversion, and Taiwanese contextual decoding are not integrated prototypes here. Their performance and quality are **untested**, not assumed equivalent to a shared lexical ranker. Dictionary-only mechanisms cannot be promoted into universal language models from these results.

Baseline `tools/test-core.ps1` and the pinned `tools/test-desktop.ps1` results are recorded in `verification.json`. Source-ledger validation and deterministic corpus checks cover this test/data change. Android builds and phone tests were unnecessary; the phone was not touched.
