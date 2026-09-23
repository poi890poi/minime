# First-glyph visibility assertion

Type: test bug fix; no production ranking change.

The integration assertion required a particular glyph for each of four inputs.
It fails for `jintian` on both runtime baseline 995b535 and the query-local sort
trial, with the same screenshot: 今天, 今天的, 進, 今, 今天在. First-glyph
choices are visible, but 金 is not one of the two leading glyphs. Requiring its
exact rank conflates visibility/consumption with a fixed dictionary ordering.

Use each fixture's known first syllable to obtain eligible single Han glyphs
from the source dictionary. Find a source-valid choice whose center lies in the
visible candidate strip, physically tap it, and verify the selected glyph plus
the independently specified remaining spelling. Check the composing boundary,
then finish the remainder with Space. No per-glyph promotion or ranking change
is introduced; no completion/consumption assertion is removed.

The source decoder supplies eligibility, not expected rank or consumption. This
is an integration contract test, not an independent dictionary accuracy audit.
The four fixtures are established development cases, not a fresh holdout.

Evidence:

- [Baseline failure](baseline/instrumentation.txt) and
  [baseline screen](baseline/review-control-failure.png).
- [Trial failure](trial/instrumentation.txt) and
  [trial screen](trial/review-control-failure.png).
- [Corrected test](corrected.txt): PASS, eight selections over four inputs with
  both Java and native Rime decoding. Local Android test build also passes.

Risk: a glyph may be source-valid but consume the wrong span. The explicit
expected suffix and composing-boundary assertions continue to catch that.
Source changes can alter eligible rank without breaking the intended contract.
Original APK, settings, learning and IME were restored; display OFF verified in
session f165c655-17bb-4c61-8e60-630fb6735771, then the phone was explicitly released.
