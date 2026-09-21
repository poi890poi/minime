# Development loop: Rudy reading boundaries

Baseline `7efab7f`. Bug fix in geography data compilation, not a dictionary/source
expansion. Of 175 source-present misses in the existing Chinese audit, 161 fall
outside the bounded unit candidate set even under exhaustive matching; these are
not demonstrated traversal failures. Twelve require future syllables rather than
matching every source unit. Two are compact Rudy readings with no unit boundaries.

The pinned Rudy snapshot has 3,144 upstream Pinyin tags, 3,057 without separators.
The shared matcher treats such tags as one unit, preventing normal initial/mixed
matching. Derived McBopomofo readings already contain boundaries.

Retain original tags. Add a separated alias only when exactly one segmentation
uses the existing licensed syllable inventory, respects supplied separators and
has one syllable per Han glyph. Ambiguous/invalid/mismatched readings keep their
original full spelling only. No alternate letters, phonetic guesses, entity
allowlist, weights, source snapshot refresh or runtime search-budget increase.
The existing initial-alias rule applies to the recovered separated reading.

Before compiling, freeze all upstream tags and independent source-derived
boundaries from the existing McBopomofo word readings/unambiguous units. Benchmark
full, initial and alternating full/initial readings. Targets are evaluation only;
production sees source tags and syllable inventory. This is known-source retrieval,
not language-model accuracy or a fresh conversational holdout.

Require preserved full-spelling lookup and name inventory; no invented names;
source/binary integrity checks; core and pinned desktop gates. On the frozen
31,527 Chinese episodes, report full/first-eight target gains AND losses by genre,
useful slots, acceptance changes and new suggestion exposure. English remains
protected. Reject or narrow only on a demonstrated mechanism if unrelated
language quality regresses; never tune individual aliases. Report host latency,
pack size and limits. No phone/release claim in this loop.
