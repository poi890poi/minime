# Japanese vocabulary, kana and kanji — 0.7.3

MinIME now gives 台/日 modes a selected-language suggestion tier, expands Japanese
common vocabulary across parts of speech, and offers single hiragana, katakana
and source-ranked kanji. The existing Japanese option controls all these sources.
Chinese and English modes retain their prior output and acceptance behavior.

## Sources and extraction

The previous Japanese importer accepted only common expression/interjection senses.
The existing JMdict snapshot has **22,637 source-common records**; ordinary nouns,
verbs and adjectives were largely absent. The expanded rule adds **62,468 indexed
rows** while preserving all **85,845** previous add-on rows exactly. Indexed rows
include reading aliases and alternative output forms; they are not unique phrases.
Every new row is attributed to JMdict. Every imported common reading/spelling is
checked against source commonness, reading restrictions and applicable senses.
See [source audit](source-audit.json) and [source catalog](../../sources/README.md).

WanaKana 5.3.1 supplies **114 single-kana input aliases in both scripts**, covering
**82 hiragana and 82 katakana**. The compiler enumerates the pinned library's
conversion tree, including small kana and IME aliases; no handwritten syllabary.
KANJIDIC2 supplies **500 kanji** (frequency ranks 1–500), with complete unmarked
Japanese on/kun readings. The separate character asset contains **1,279 rows**.
Source-character identity survives matching and overrides Mandarin glyph order
only for this validated source in Japanese mode. Other glyph safeguards remain.

KANJIDIC's frequency is newspaper-derived, not contemporary conversational or
reading-level frequency. JMdict common flags are also an imperfect indicator.
Sources: [KANJIDIC documentation](https://www.edrdg.org/kanjidic/kanjidic2_ov_legacy.html),
[JMdict priority documentation](https://www.edrdg.org/jmdict_edict_list/2016/msg00060.html),
[WanaKana](https://wanakana.com/). Source pins, attribution and CC BY-SA 4.0/MIT
terms are packaged in Sources/Notices and recorded under third_party.
No Simplified Chinese vocabulary conversion or per-word promotions are involved.

## Broad source retrieval

All eligible common kana readings plus a fixed hash-selected partial sample were
frozen before output evaluation. **24,996 queries** retain source IDs, readings,
targets and conditions. There are 19 unsupported source aliases, listed in the
[manifest](inputs-manifest.json). None is silently counted as covered.

| Input condition | Queries | Before: reachable | After: reachable | After: first 3 / first 8 |
|---|---:|---:|---:|---:|
| Complete | 22,953 | 744 | 22,953 | 22,509 / 22,819 |
| Half prefix | 1,024 | 37 | 224 | 75 / 191 |
| One initial per reading unit | 1,019 | 37 | 796 | 499 / 766 |

This is retrieval from the production source, **not independent language accuracy**.
It does not establish sense correctness, modern conversational usage, Japanese
sentence conversion or inflection support. Prefix retrieval remains weak even
though complete and initial retrieval improve substantially. The 500-character
asset is likewise character data, not inferred word or phrase frequency.

## Rejected approach and remaining tradeoffs

The first expansion put new vocabulary into the existing eight-result search.
Japanese expression half-prefix availability on the old frozen corpus fell from
97 to 40/128. That approach was rejected; its [outputs](rejected-unpartitioned-modes.jsonl.gz)
and [results](rejected-unpartitioned-results.json) are retained.

The final implementation gives ordinary source-common vocabulary a separate
eight-result index, preserving the original expression/name/culture budget.
Characters add at most eight more; Japanese optional lookup is bounded at 24
before deduplication. Complete matches precede partial matches. This rule uses
source categories, never complaint words, IDs, test targets or tuned word weights.
Old Japanese expression half-prefix availability recovers to **96/128**. One
remaining loss follows the expanded expression import, and is not patched by name.

On the previously exposed 3,922-condition mode corpus, the Japanese-data extension
has exact parity for **Chinese, English and Taiwanese modes**. Japanese complete
expression coverage remains 128/128. Relative to the ranking-only stage, Japanese
expression half-prefix first-three coverage falls 64 → 48/128 as character choices
and complete vocabulary matches take earlier positions. Relative to shipped 0.7.2,
it improves **25 → 48/128**; first-eight improves **29 → 72/128**. Taiwanese
half-prefix first-three remains the ranking-stage improvement **14 → 28/128**.

English half-prefix suggestions move down in Japanese mode: conversation first-three
26 → 17/256, essay 21 → 14/128 versus the ranking-only stage. Chinese essay complete
first-three falls 223 → 219/416. English full-word highlights stay unchanged.
These are ordinal optional slots, not measured physical row/page coverage. Every
source/genre/condition/mode is retained in [results.json](results.json). No independent
Japanese/Taiwanese conversation or essay corpus is claimed; their probes are
source retrieval. English conversation/essay and Chinese essay results remain separate.

## Performance and validation

The larger dictionary costs more lookup work. On the source-query desktop run,
complete lookup median/p95 is **0.080/0.572 ms**, half-prefix **0.176/3.738 ms**,
and initials **0.100/0.971 ms**. Before expansion those p95 values were
0.052/0.392/0.040 ms. Raw timings and maxima are retained. Loading is once off the
input thread; no dictionary search is performed while drawing.

Phone last-key dispatch through worker/main callback, excluding rendering,
186 measured inputs per mode after warmup, versus the committed 0.7.2 telemetry:

| Mode | Before median / p95 | 0.7.3 median / p95 |
|---|---:|---:|
| Chinese | 40.9 / 58.5 ms | 41.3 / 60.7 ms |
| English | 0.52 / 1.76 ms | 0.42 / 1.78 ms |
| Taiwanese | 42.4 / 62.2 ms | 43.6 / 64.3 ms |
| Japanese | 41.8 / 61.3 ms | 44.5 / 58.3 ms |

Different sessions do not establish a causal speedup or a precise slowdown. The
Japanese median rises while its p95 falls; both are reported. Raw [phone timing](phone-latency.tsv.gz)
and [aggregate statistics](phone-performance.json) preserve the evidence.

Core passes **31,957 mechanical assertions**. The pinned Rime 1.16.1 native gate
runs 628 uncached queries with byte-identical 0.7.2 output. Deterministic character
and paired-form regeneration, source validation, all 19 source-contract tests,
Android build and lint pass (0 errors, 17 warnings). All 12,346 Taiwanese Han pairs
remain unchanged. Four phone tests pass across the sessions: actual kana/kanji and
new common-word taps, mode-switch/geometry behavior, Taiwanese paired acceptance,
and latency. The first Japanese UI probe erroneously included its header; the
loader was corrected and only that test rerun. The failed and successful logs
are retained rather than reported as a single clean run.

Only RFCR91GWXLX was tested. Every session restored Samsung HoneyBoard and compared
settings/learning with their original backups. The display was verified OFF and
AOD was untouched. The phone window has been released to the other task.

[Download 0.7.3 APK ZIP](https://que-put-miracle-wal.trycloudflare.com/MinIME-0.7.3.zip).
The link uses a temporary Cloudflare tunnel. Package identities and verification
are recorded in [verification.json](verification.json).

## Reproduce

`tools/freeze_japanese_queries.py` verifies the immutable source-derived inputs.
`tools/audit_japanese_sources.py` checks source forms and preserved rows.
`tools/compile_japanese_basics.py --check` and `tools/compile_paired_forms.py --check`
verify deterministic assets; source-catalog and asset verifiers cover input hashes.
Run `tools/test-core.ps1` and `tools/test-desktop.ps1 -Corpus
 docs/suggestion-latency/native-inputs.tsv` before Android builds.
`JapaneseCoverageAudit` takes the add-on TSV, character TSV (or `-`), frozen input
TSV and output TSV. Baseline add-on bytes come from `fb85a39`; current code is used
for both source comparisons. `ModeCoverageBenchmark` reuses the input-modes corpus;
`tools/report_japanese_coverage.py` records the before/after outputs and metrics.
