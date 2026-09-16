# Completion boundary fix

Type: bug fix in shared composition acceptance. No dictionary, ranking, option,
layout, or automatic-correction default changes.

The live study established a missing separator after an explicitly selected English
completion on Pinyin. `selectChoice` enabled its boundary only in English mode;
`type` resolved it from the next key rather than the accepted segment. Merely
removing that mode guard would insert spaces before Chinese composed from Latin keys.

The fix lets every English-enabled board retain an English completion boundary.
Pure English resolves it on the next letter as before. Mixed boards resolve it
when a candidate/raw token is accepted, in the same editor replacement as the
accepted text. Han, kana and source-tagged Taiwanese choices cannot trigger an
English separator. Explicit punctuation/Space, cursor changes and deleting the
accepted word cancel it. Editing the new unfinished word retains it. Acceptance
queued behind a decoder result consumes it exactly once.

Scope: this does **not** change the existing Pinyin Space key's trailing-space
contract. `pronun` → tap `pronunciation` → `test` → Space now yields
`pronunciation test `, with MinIME's existing final space. Directly accepted English
tokens still have that trailing space; Google differs. Changing that established
contract is separate from fixing concatenated completion selections.

Risk boundaries: single editor replacement avoids losing the composition or
duplicating it; dictionary pack provenance excludes foreign Latin readings;
literal/direct fields still cannot create completion boundaries. No persistent
schema changes and no extra dictionary lookup per key.

Verification:

- New standalone regression against pre-fix HEAD fails on `alpha` completion +
  accepted `beta`: `alphabeta ` instead of `alpha beta `.
- `tools/test-core.ps1`: PASS, 309,236 behavioral assertions (not accuracy).
- `tools/test-desktop.ps1`: 13,014 frozen inputs evaluated against pinned Rime;
  11,272 uncached native queries. This is corpus execution, not a coverage claim.
- Native phone integration: four tests passed, covering mixed completion acceptance,
  Chinese adjacency, explicit spaces, edited spelling, existing English completion,
  correction undo, double Space, and sentence capitalization. Final build and
  phone evidence are retained alongside this report. Prior IME/preferences were
  restored and display OFF verified.
