# Japanese character extension

The user additionally requested single hiragana, katakana and very common kanji.
This follows the independently measured ranking change `fb85a39`.

Use the pinned WanaKana 5.3.1 conversion tree to enumerate every single-kana
mapping, including aliases and small kana, and derive both scripts. No handwritten
syllabary or per-character exceptions. Use KANJIDIC2 from the same pinned release
as JMdict, selecting its frequency ranks 1–500. This is a fixed product coverage
cutoff chosen before output evaluation, not a fitted threshold. Retain only
complete Japanese on/kun readings without dot/affix markers; do not turn a reading
stem into an invented standalone word. Nanori and non-Japanese readings are out.

This is an optional Japanese-mode source, isolated from the ordinary vocabulary
asset. It provides a bounded extra eight source-backed character choices using
the existing prefix and reading-unit indexes. Kana precedes kanji; kanji follows
the source frequency rank. Whole matches precede partials. Existing word candidates
remain reachable, although character choices can displace them from the first few
positions. No lookup runs while drawing. Disabling Japanese or selecting another
mode removes this extension. No stored-data migration.

The Mandarin single-glyph safeguard must not veto Japanese readings of this
explicitly ranked Japanese character source. Preserve it for every existing
source and the other modes. Carry source-character identity with candidate pack
metadata through matching/presentation, not an output-string exception list.

Validate the complete generated kana/kanji inventory and source relationships;
report retrieval limits rather than calling assertion counts accuracy. Re-run
the frozen mode corpus against `fb85a39` with the same data-loading harness and
compare changes separately from the ranking-only results. Known limitations:
KANJIDIC frequency is newspaper-derived, not modern conversation frequency;
reading availability does not establish reading-level popularity. No sentence
conversion, inflection generation or new phrase corpus is included.
