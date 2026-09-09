# Direct Taiwanese paired output

Approved behavior: tap selects the visible primary form, long press directly
selects the paired alternative without opening a menu. The secondary line shows
that exact alternative. An optional Taiwanese Han-primary setting reverses the
two forms. Pairing can be disabled. Keyboard and candidate-row height remain fixed.

Type: new feature and intentional acceptance behavior. Shared core owns alternate
acceptance, preference mapping and stale-result rejection. Android owns rendering
and gesture recognition. Risk: long-press followed by UP could insert twice,
scrolling could select, a refreshed cell could change the held choice, and a
recognition example could be mistaken for an attested spelling. Tests cover these
boundaries. No new phonetic entries, reading aliases or rank weights are added.

Source decision recorded before generation: reuse the pinned CC0 iTaigi source's
PojUnicode/HanLoTaibunPoj pair within the same source record. Only already shipped
iTaigi output/source relationships are eligible; do not borrow another homophone's
characters for beginner examples. Select one single all-Han source form per phonetic
output using source occurrence data, never per-word exceptions. Its source ID identifies
the chosen example; other iTaigi entries with the same complete phonetics share that
example, rather than inheriting every original record’s meaning. No MOE-derived
BY-ND data enters production.

Character familiarity policy frozen before output inspection: use positive
single-Han-character occurrence rows from the pinned Taiwan-authored McBopomofo
phrase.occ, retaining the smallest frequency-ranked set covering 99% of their
occurrence mass, including all frequency ties at its boundary. This is a written
Taiwan Mandarin familiarity proxy, not Taiwanese spelling popularity or consensus.
Require every character of the alternate to be in that set. Among eligible
attested forms prefer the greater minimum glyph count, then the greater source
whole-form occurrence count, then stable source ID. Do not use Unicode order as
frequency. This may omit legitimate Taiwanese spellings; omission leaves the POJ
candidate available. The 99% policy is not tuned to examples or evaluation labels.

The paired TSV is a separately attributed, reproducible sidecar. Existing
addons.tsv and language model remain byte-identical. Metadata is attached during
background dictionary loading, shared between lookup aliases, and included in
the candidate's immutable result. No lookup or secondary asynchronous annotation
work occurs during drawing. Disagreement filtering remains absent because suitable
systematic evidence was not established.

Verification: core regressions and pinned desktop Rime before Android build;
source-owned pair integrity and coverage audit; paired-off/on output parity and
lookup timing; actual phone tap/hold/scroll/cancel/stale/lifecycle checks, geometry
and rendering. Restore prior IME/preferences and verify the authorized display OFF.
