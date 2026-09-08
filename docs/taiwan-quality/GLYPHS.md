# First-choice and first-row glyph audit

Baseline: MinIME 0.6.3 / 3f25366. Frozen query set: **16,208 unique inputs**.
It includes every supported syllable, every syllable prefix, missing-letter,
adjacent-key and transposition inputs, plus full/initial/mixed readings and every
prefix of 2,000 source-hash-selected phrases. Groups overlap. No frequency labels
or individual complaint examples enter query selection or production ranking.

Each input is traced through core, native Rime, merge, composition with no packs,
Taiwan only, and all packs: **97,248 snapshots per run**. Composition flushes the
final revision and records both the preferred candidate and actual Space output.
All editors/native sessions are fresh; personal history is not represented.
The separate first-100 native trace covers all 499 syllable/prefix inputs.

## Confirmed defect and correction

An optional static dictionary could promote a glyph missing from the bounded base
candidate list. The 0.6.3 duplicate guard only protected entries already present.
Source identity and a pronunciation alias were incorrectly treated as evidence of
priority. English completion interleaving exposed the promotion at position one.

| Input | Add-on first choice | Native evidence |
|---|---|---|
| `ding` | 耵 | Rime ranks it 25th, beyond the adapter's 24 whole-input results |
| `gong` | 玒 | Rime ranks it 35th, beyond that same window |
| `shi` | 纚 | Not present within the first 100 native candidates; fallback only has its `xi` reading |

These cases were found by enumeration. They do not supply production exceptions.
The general rule now places an unweighted new Han glyph after the established
whole-input base glyphs. Existing duplicate order stays protected; a user can still
explicitly select the rare glyph. With no base Han competitor, optional source order
is unchanged. A first experiment that reordered optional-only Kanji/Kana choices
was rejected; its raw output is retained separately.

The final fix removes all measured static-add-on first-choice glyph promotions
flagged by this audit (three inputs in Taiwan-only mode; two with all packs).
It changes 202 candidate snapshots, but **no Space output across all 16,208 inputs**.
Core, native, merge and no-pack outputs are unchanged. It does not delete rare
characters or change any character frequency, native schema or source dictionary.

## Remaining root causes

1. **Exact unusual readings compete with unfinished ordinary input.** Rime supplies
   㩐 for `den` and 𨈖 for `din`; the fallback instead starts with 等 and 定, whose
   readings are still incomplete. These exotic readings are in the native lexicon,
   despite absent essay/reference counts. They are the first Chinese alternatives
   even when English completions occupy the first visible slot. A whole-input
   native result precedes fallback completion; this is a matching-policy problem,
   not another add-on promotion.
2. **Native spelling corrections extend rare readings to imperfect input.** For
   example, the configured `ao → oa` correction makes `fioa` reach 覅 (`fiao`). The
   audit distinguishes whole-input correction/expansion from prefix consumption;
   consumed-all does not establish an exact dictionary spelling.
3. **Native frequencies reflect a different usage distribution.** The source essay
   gives 咋 13,090 and 雜 3,334; the independent MOE school reference records 1 and
   177. For 俺/安, essay counts are 17,692/12,474 versus MOE 5/1,121. Reading weights
   and source counts are retained in the machine report. This is evidence of a
   distribution mismatch, not proof of political intent or universal correct order.
4. **Some syllables have only uncommon alternatives, and reference corpora miss
   everyday expressions.** A low-frequency alert for 謬 under `miu`, or an absent
   school-corpus count for 囧/咩, does not justify suppressing that valid choice.

The machine report also lists every all-pack first-eight reference alert and the
first Chinese glyph when another language precedes it. Its review threshold is
MOE rank after 3,000 or absent, with UD count at most one. These are evaluation-only
flags. Missing counts mean unknown; global character counts cannot distinguish
polyphonic readings or conversational/register differences. Alert counts must not
be presented as confirmed error counts.

## Verification and next boundary

Core: 19,425 assertions pass, including generic fixtures under every pack name,
English overlap, explicit rare-glyph selection and optional-only source ordering.
The separate 21,044-record conversation run preserves all 130,233 English token
outputs, all Chinese Space outputs and all top-one results. Top-eight retrieval
improves slightly in every reported group; see glyph-conversation-parity.json.
These are comparisons on frozen corpora, not universal language-accuracy claims.

Native dictionary frequencies and exact-versus-completion competition remain
unchanged in this fix. A further native-data experiment should compare an attributed
Taiwan usage model and pronunciation-specific priors on fresh held-out text. Blindly
boosting globally common glyphs would conflate alternate readings; deleting rare
syllables would remove valid user input. Neither is justified by the current audit.

Reproduce: make_first_choice_corpus.py, FirstChoiceAudit (baseline and final classes
with identical 0.6.3 assets), report_first_choice_audit.py. Native first-100 tracing
uses desktop_rime.cpp --audit-all; its default adapter behavior is unchanged.
Raw snapshots, corpus/reference hashes, acceptance changes and rejected output are
retained in this directory. Independent frequency references and licences remain
in ../glyph-ranking; they are never packaged or consulted by the runtime.
