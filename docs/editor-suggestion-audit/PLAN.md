# App/editor suggestion audit

Defect report: English suggestions appear to lose many words/phrases in some apps,
including Google Keep. Expected: comparable ordinary text editors produce the same
candidate stream for the same language, dictionary settings and typed input.

This is a diagnosis and test change before any runtime fix. Freeze representative
mechanism probes, not a dictionary accuracy benchmark: English `pronun`, `tomorr`,
`keybo`; Chinese `nihao`, `xiexie`; first reproducibly eligible everyday POJ and
Japanese packaged-data readings and their prefixes. Include English prefixes in
the mixed modes. No production dictionary, weights or per-app exceptions.

Audit normal text, multiline/autocorrect/capitalization, app autocomplete,
no-suggestions, no-personalized-learning, URI and password policies. Compare the
visible candidate lists and editor lifecycle after same-field restarts. Treat
secure/numeric restrictions separately from ordinary text. Check the shared core
first; use Android tests for metadata, editor callbacks and visible suggestions.
Previously evaluated corpora are not fresh holdouts, and assertion counts do not
measure language coverage.

Inspect actual Keep and Chrome fields alongside the controlled Android editor.
The user authorized Google Play sign-in/setup. The supplied account was already
signed in; skipping optional payment setup started the official Keep installation.
No password was needed. Do not record existing personal notes or submit test
messages/searches. Remove only test-created text. Restore IME/preferences, verify
display OFF and explicitly release the shared phone reservation.

Initial code hypotheses (not established causes):

- `EditorPolicy.literalEnglish` suppresses all English dictionary assistance for
  NO_SUGGESTIONS and URI/email fields; mixed-language paths differ.
- `onStartInput` preserves active composition on some same-field restarts, but an
  empty composition with recent-word context falls through to engine reset.
- Selection callbacks can abandon composition/context. Actual app observations
  must distinguish this from missing data and from a flawed test harness.

The first observation-only matrix is invalid as app-quality evidence: it omitted
editor-content and active-mode checks, and recorded inconsistent streams (including
Chinese output in rows labelled Japanese). Retain that negative result locally;
do not count its 88 rows as verified language/editor coverage. The replacement
must verify text delivery and mode identity before comparing candidate lists.

Android references inspected:
https://developer.android.com/reference/android/text/InputType
https://developer.android.com/reference/android/inputmethodservice/InputMethodService

Confirmed boundaries (2026-09-16):

- Keep 5.26.361.01.90 note body advertises inputType 0xac001, including
  NO_SUGGESTIONS. EditorPolicy maps this to literal English, which suppresses
  all completions, while mixed modes retain dictionary candidates. Change this
  general hint policy to retain manually selectable candidates, suppressing
  optional automatic spelling correction instead. Password/numeric/URI/email
  literal behavior and private-field learning restrictions remain unchanged.
- The validated native audit retains unfinished-word candidates on restart,
  but all four English committed-context probes lose next-word candidates.
  onStartInput only resumes nonempty composition, so a same-field restart
  resets the engine's accepted-word context. Preserve context only when field,
  policy, cursor and a bounded suffix of IME-owned committed text still match.
  External edits, cursor movement and new fields must still clear context.

Both changes are Android editor integration fixes; dictionaries/ranking and
shared core start/reset semantics remain unchanged. Test the two causes
independently, then repeat actual-app and controlled-editor comparisons.
