# MinIME 0.6.3 verification and decisions

Baseline: 146bf1e / 0.6.2. Changes are separated into provenance, source metadata,
benchmark, native merge, shared matching, duplicate promotion, worker/acceptance,
and release commits. See [glyph investigation](../glyph-ranking/RESULTS.md),
[speculation results](RESULTS.md), and [matching results](MATCHING.md).

## Decisions and scope

- Keep native hypotheses and stored fallback entries; suppress extra core word
  concatenations when native decoding already has a whole-input result. Preserve
  full fallback when native results are absent or prefix-only. The exhaustive
  670,020-case experiment found only 0.25% dictionary attestation among appended
  core-composed occurrences. Attestation is not semantic correctness, and native
  results are not classified as lexical by this API. Removing all composition
  was rejected because it destroyed natural-span fallback coverage.
- Use the same bounded prefix and source-unit matching for every optional pack.
  Japanese boundaries come from pinned WanaKana conversion units. All 56,869
  normalized language-asset records retain their output/key/source/category.
  The 329,599-case source-derived lookup evaluation shows unchanged full-key
  retrieval. Initial ambiguity remains substantial, especially for POJ.
- Preserve the rank of an existing exact base entry when an optional dictionary
  duplicates it. Also preserve whole-input native single-glyph entries absent from
  the fallback dictionary. Across 499 syllable/prefix queries, optional dictionaries
  caused 9,540 existing-glyph pair reversals in 0.6.2 and zero after this rule.
  These are correlated pair counts, not 9,540 independently judged language errors.
- Run optional and base lookup together on the decoder worker and publish one
  revision-bound result. Space waits for that result and follows the first eligible
  displayed conversion choice. English/raw recovery, valid bare spellings,
  apostrophe restoration, prefix-only explicit selection, and private fields retain
  their guards. Stale callbacks cannot accept abandoned text.
- Preview one incomplete alternative early; place the remaining incomplete entries
  after the first eight alternatives. This is an intentional presentation policy,
  identical across packs. The first experiment placing eight incomplete entries
  together crowded out base choices and was rejected. Its raw results are retained.

## Broad integration results and tradeoffs

The final desktop comparison uses 13,014 frozen inputs and 21,044 output records,
with all four packs enabled. Pure repeated dictionary lookups are memoized in this
evaluation harness; it makes no end-to-end latency claim. See final-conversation.json,
english-final-changes.json and the compressed raw records in conversation-artifacts.json.

| Held-out group | Cases | 0.6.2 top 8 | Final top 8 |
|---|---:|---:|---:|
| Chinese full | 1,000 | 927 | 932 |
| Chinese initials | 1,000 | 213 | 307 |
| Chinese mixed-left | 1,000 | 734 | 728 |
| Chinese mixed-right | 1,000 | 658 | 643 |
| MOE glyph probes | 999 | 694 | 727 |

Chinese full top-one falls from 713 to 703. Preserving native order removes some
artificially helpful duplicate promotions as well as harmful ones; partial previews
also consume visible slots. The final change is not an across-the-board quality win.
Known source-reading ambiguities prevent interpreting generated phonetic spans as
absolute conversation accuracy. No evaluation labels or individual exceptions enter
production ranking.

All 43,411 English-board token outputs are unchanged. Fresh and Chinese-primed
Pinyin modes each change 15 of 43,411 English-corpus tokens. Changes include Chinese
guesses becoming optional-language entries, and unknown raw words becoming partial
phrase matches when a conversion candidate already owns Space. All differences are
retained; this is not a claim that every replacement is intended. Unknown names and
cross-language ambiguity remain an acceptance-policy limitation.

## Android costs and gates

Phone RFCR91GWXLX: all 21 editor, Rime, add-on, candidate-stability and dictionary
impact integration tests pass. Previous IME and settings/learning preferences were
restored; post-test power state was verified as Dozing (display asleep).

Android component benchmark (phone-performance.json): language index load 4.17 s,
geography index load 3.48 s, approximately 42.4 MiB retained heap. All-pack lookup
p50/p95 is 1.80/6.76 ms; partial lookup is 1.40/6.43 ms with a 65.23 ms maximum.
This excludes Rime, touch dispatch and rendering. Worker scheduling avoids this
lookup on the UI thread, but does not eliminate load cost or long-tail result delay.
These costs are a remaining optimization target, not a demonstrated speedup.

Core: 19,390 assertions pass, including shared partial matching, source-wide
apostrophes, stale callback/Space ownership and generic glyph-duplicate fixtures.
These assertions verify behavior, not language-model accuracy. Debug APK, test APK
and lint build successfully; lint has zero errors and 17 warnings. Four native ABIs,
packaged source assets and the unchanged base-model hash are verified in package.json.
No keyboard geometry changes are included.

## Remaining data work

The glyph report identifies native frequency-distribution mismatches and 11 missing
fallback glyph counts, including 臺. These are diagnosed, not corrected in this
release. A separate experiment should use attributed Taiwan usage data, distinguish
missing counts from observed zero, preserve pronunciation-specific frequencies, and
evaluate on a fresh independent corpus. Globally boosting common characters would
confuse polyphonic readings and is not supported by this evidence.
