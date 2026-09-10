# Japanese typing continuity

Bug-fix audit, 2026-09-11. Japanese words and characters are indexed as whole
readings; there is no runtime kana transducer. Once a sequence is outside that
index, lookup returns nothing. There is no three-kana cutoff: all 7,291 source
readings of 1–3 kana and 15,662 longer readings retrieve their target. But 997 of
998 deterministic joined-reading probes have no Japanese choice. These joins
test typing availability, not natural phrases or language-model accuracy.

Fix boundary: provide hiragana/katakana spelling from the pinned MIT WanaKana
grammar through JapaneseBasics in the shared core. Retain dictionary matches
ahead of fallback spellings; do not invent Kanji sequences or vocabulary. A
trailing unfinished syllable stays owned by composition and needs an explicit
partial selection. Raw recovery, mode isolation, secure fields, dictionary
source order and the 96-character input bound remain intact.

Risks: ambiguous n, doubled consonants, digraphs, suffix consumption, and changing
existing lexical ranking. Compare conversion to the independent pinned library,
run all source readings and frozen joins before/after, exercise acceptance and
backspace, then core and pinned desktop Rime gates before an Android build.
The existing source audit corpus is already exposed; no fresh holdout claim.

Results: all 998 joined inputs now have Japanese candidates; all 22,953 source
reading targets remain retrievable. Lookup p95 by group: 0.173 ms joined, 0.326 ms
short source readings, 0.171 ms longer readings (single desktop run, not Android
latency). `results.json` and compressed before/after query records retain evidence.
545 source grammar rules add 19322 bytes to the uncompressed character asset.

24,989 differential probes pass against WanaKana. Shared core: 85,936 assertions;
pinned desktop Rime: 628 queries, p95 7.08 ms including native IPC. Android build,
lint and packaged source checks pass. Long kana candidates and explicit acceptance
passed on the authorized Android 13 phone. First phone fixture wrongly expected
は from `wa`; corrected to the pinned library's わ, with no production change.
Phone preferences and previous IME were restored and display OFF verified.

This closes missing kana availability, not sentence-level Kanji conversion. Kana
spelling fallback does not infer particles, conjugations or lexical boundaries;
full sentence conversion remains a separate dictionary/decoder capability.
