# Reading-prior removals are bounded-list demotions

September 24, 2026. **Diagnosis confirmed; production weights unchanged.**

The previous report's six lost labelled episodes were two single-character
targets under two one-letter queries, repeated across corpus rows. Calling them
six lost phrases was incorrect. The original trial still failed its declared
no-lost-target gate; this follow-up does not retroactively turn it into a pass.

## Problem and cause

The current importer assigns one whole-glyph occurrence count to every source
pronunciation. McBopomofo's own compiled model distinguishes alternate readings.
The already-frozen audit variant applies only its relative single-glyph reading
penalties: 618 of 171,708 rows change, with no added/removed entries or increased
weights. For example, the upstream lists rank 兒's ㄦˊ reading first, ㄦ second,
and ㄋㄧˊ third. No example becomes a production exception.

The [fixed follow-up](READING-PRIOR-FOLLOWUP.md) checks every one of the existing
499 syllable/prefix inputs and all 25,100 distinct single-Han reading/output pairs
using the shared core, then checks every complete source spelling of a removed
identity. Query labels never enter decoding.

| Measure | Normal 128-candidate retrieval cap | Diagnostic cap removed |
|---|---:|---:|
| Complete single-Han reading pairs missing before / after | 0 / 0 | 0 / 0 |
| Query/output pairs removed by reading penalties | 196 | 0 |
| Removed pairs retaining every complete source spelling | 196 | — |
| Removed pairs matching a source-defined reading penalty | 196 | — |
| Query/output pairs newly returned | 205 | 36 |

All 196 removals are single characters: 106 use one-letter input, 72 two letters,
and 18 three letters. Normal-cap gains comprise 159 character/query pairs and
46 phrase/query pairs. These are inventory counts, **not accuracy percentages**.
Complete-spelling access remains intact even for the two targets lost in the
older labelled comparison. An identical-dictionary control reports zero changed
queries and zero removed identities.

The causal control changes only ReadingUnitIndex's `CANDIDATES=128` to
`Integer.MAX_VALUE` in an isolated class. All 196 removals disappear. This proves
the list bound contributes to the losses; it does not prove that unlimited lists
would be a good keyboard. Other search limits remain active, and 36 new phrase
pairs still appear, so this is not an exhaustive-search claim. No cap change is
installed or compiled into the app.

## Decision and next work

Retain the diagnosis and the previous rejection. A new source-prior proposal
should explicitly distinguish complete-spelling access from bounded initial
suggestions. Preserving every old abbreviated result is not the same objective
as improving its relevance. Any changed tradeoff must be declared before a new
evaluation, retain genre/condition losses, and have independent pronunciation
and conversational evidence. The current audit does not establish that every
removed reading is useless or that every promoted candidate is appropriate.

No production dictionary, source decision, model, retrieval limit, app preference
or acceptance behavior changes here. This addresses an uncertain diagnosis;
language quality, source-rights migration, phone latency, Android 16/16 KB and
Play delivery remain release gates.

## Reproduction and limitations

Regenerate the variant with `tools/audit_chinese_reading_weights.py` and the
already-pinned upstream `data.txt`; its SHA-256 must equal the original
`weights-manifest.json`. An older ignored copy had CRLF line endings, so the raw
hash check correctly failed. Regeneration restores exactly the original LF hash;
the repeated parsed results are identical. No source hash was updated to accept
that mismatch.

Compile `ReadingPriorAudit.java` with the standard core classes, then run it with
the production TSV, regenerated reading-prior TSV, `docs/glyph-ranking/inputs.txt`
and an ignored output path. Repeat with the production TSV on both sides for
the identity control. `tools/report_reading_prior_losses.py` verifies the original
data pins and summarizes the inventories. The diagnostic unbounded class is an
isolated copy of the accepted ReadingUnitIndex with only the stated constant
changed, placed before the ordinary core classes on the classpath.
Pass that generated file as `--unbounded-source` to the reporter; it verifies
the single-constant difference and records the isolated class source hash.

Aggregate reports are `reading-prior-summary.json` and
`reading-prior-unbounded-summary.json`. Per-query output stays under ignored
`artifacts/reading-prior-followup`. This reuses inspected source/retrieval inputs,
not a fresh holdout or a natural-conversation sample. The English/context-free
fixture isolates Chinese dictionary retrieval; it does not certify mixed-mode
ordering, Space selection, native Rime, optional packs or device behavior.
The standard core suite passes all 1,770,117 contract assertions after adding
the audit tooling; that count is not a language-quality score.
