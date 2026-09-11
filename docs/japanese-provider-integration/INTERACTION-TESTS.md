# Interaction test maintenance found during phone admission

Type: test correction, not a production behavior change. The initial broad phone
run executed 65 tests. Besides one native ordering failure, four older keyboard
tests disagreed with already implemented contracts:

- Two expected mode switching to commit the previous composition. Both
  CompositionEngine.switchMode and an existing live interaction test preserve it.
  Updated checks assert retained spelling and an explicit subsequent acceptance.
- The mixed-mode test expected Japanese/Taiwanese quick switching through English.
  ModePreferences.quickTarget implements the user-requested Chinese ↔ last focused
  language behavior. Updated checks exercise both directions and retain geometry,
  composing-span, optional-pack and field-lifecycle assertions.
- The contraction test remained in Japanese focus after its Japanese assertion,
  then expected English to win. It now selects the actual Chinese and English
  modes whose contraction defaults it intends to test. Japanese priority remains.

No production ranking, switch behavior or vocabulary was changed to satisfy these
assertions. The four corrected tests passed in the focused repeat run. Sixty
unchanged checks passed in the initial run, including async delivery, editor
integration, candidate stability and the three synthetic human-input precision
tests. Preserve the initial failure log and final five-test pass log; do not label
the initial 65-test run a pass.
