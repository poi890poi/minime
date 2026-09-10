# Optional language loading

This records the intermediate split-TSV strategy. The phone exposed an excessive
POJ cold-load cost, addressed by the follow-up in `PREBUILT.md`.

One variable: combined optional asset versus verbatim per-pack asset/index loading.
No dictionary entries, scores, quotas, search limits or ordering rules changed.
`CompileAddonPacks` partitions all 323,719 admitted rows: Taiwan 35,667, POJ
244,131, Japanese 43,921. Duplicates and per-pack source order remain. APK
verification compares every packaged row against the source, including order.

Two fresh Java 17 processes per variant/pack, Windows desktop, `-Xmx1g`;
the repeat reverses both pack and variant order. Approximate retained heap is the
post-GC heap delta before reading evaluation inputs. It includes JVM allocations
and is not Android RSS, peak startup memory, or a physical-touch measurement.

| Requested pack | Combined cold ms, two trials | Split cold ms, two trials | Combined retained MiB | Split retained MiB |
| --- | --- | --- | --- | --- |
| POJ | 5,639 / 7,754 | 4,327 / 4,272 | 67.68 | 49.12 / 49.08 |
| Japanese | 5,564 / 5,547 | 1,212 / 1,258 | 67.68 | 9.67 |
| Taiwan | 5,542 / 5,831 | 1,176 / 1,293 | 67.68 | 9.62 |

The combined POJ repeat is a slow outlier; do not present a precise speedup ratio.
Both trial orders support lower cold loading work and retained heap for one pack.
The lookup algorithm is unchanged; per-genre timings in `results.json` are
diagnostics, not a claim of faster typing.

Each trial/pack uses all 3,922 frozen inputs from
`docs/input-modes/coverage-inputs.tsv`: 716 English conversation, 362 English
essay, 2,080 Chinese essay, 384 Japanese source-retrieval and 380 POJ
source-retrieval conditions. Full, partial and input-error conditions remain as
declared in that corpus. All 23,532 paired lookups preserve candidate order,
score, completeness, abbreviation, language-character status and paired metadata.
Thus this change has **no measured retrieval or coverage gain**. These are reused
probes, not a fresh holdout, and Japanese/POJ source retrieval is not independent
conversation or essay accuracy. Corpus/source hashes and individual result
signatures/timings are retained alongside this report.

Production loads only active optional packs. Once used, an enabled pack stays
cached for warm switching; disabling it evicts its cache ownership. Consequently,
after using every enabled language, their combined memory can still be retained.
This avoids repeatedly paying cold load cost rather than promising minimal heap
after every switch. Han annotations load only for POJ, standalone character data
only for Japanese. The base Chinese/English model remains shared.

Cold focused-mode lookup preserves the raw buffer and queues Space until the
dictionary arrives. Failure releases queued acceptance with raw recovery and an
error message. Mode/lifecycle changes invalidate obsolete attachment callbacks.
Optional Chinese specialist loading does not hold up base Chinese conversion.
Core verification: 34,990 assertions; Android integration results are recorded
in the implementation ledger after building.

Reproduce: compile the core tests; run `CompileAddonPacks` with the source TSV and
`artifacts/mode-mechanism-review/packs`; run `PackLoadingAudit <combined|split>
<poj|japanese|taiwan> <output.tsv.gz>` in separate JVMs. Repeat in reverse order.
