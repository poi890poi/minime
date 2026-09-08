# Chrome Pinyin field-policy regression

Type: Android field-policy bug fix. No dictionary, decoder, ranking, layout or
source-catalog change.

On 2026-09-09, the authorized phone's Chrome address bar reported input type
`0x80011`: text class, URI variation and NO_SUGGESTIONS. Its IME options were
`0x12000002`. The probe opened a blank page and inspected editor metadata without
submitting a search. The phone was slept afterward. The active IME during this
metadata probe was Samsung; it was not a MinIME UI reproduction.

MinIME's EditorPolicy maps either URI or NO_SUGGESTIONS to `literal`. The service
then passes literal mode to CompositionEngine, displays English keys and hides
the bottom-row language switch. The upper toolbar still offers a Chinese switch,
but tapping it cannot override the literal policy. Thus the app's request for an address/default literal input
also prevents the user from choosing Chinese conversion. Email fields share the
same mechanism. Ordinary text fields without these flags take a different path.

Android describes NO_SUGGESTIONS as an assistance flag and notes that it overrides
automatic correction. It does not declare that the field accepts only ASCII.
[Android InputType documentation](https://developer.android.com/reference/android/text/InputType#TYPE_TEXT_FLAG_NO_SUGGESTIONS)

The owning boundary is the Android editor policy and its service lifecycle:
distinguish mandatory literal fields from an English default and optional English
assistance. URI/email editors should start with literal English while exposing a
single-tap Chinese switch. Ordinary NO_SUGGESTIONS text should keep the user's
chosen language; its English assistance remains disabled. Secure, numeric and
TYPE_NULL restrictions remain unchanged, as does NO_PERSONALIZED_LEARNING.

Input restarts in the same field must preserve a manually chosen language even
when composition is empty. Otherwise Chrome can revert to English after a commit
or restart. A newly focused field still applies its own default. No package-name
exception is needed.

Verification targets: reproduce the inaccessible switch with Chrome's exact flags;
then exercise English literal entry, Chinese selection/commit, empty-buffer restart,
switching back, plain NO_SUGGESTIONS/private fields, password and number isolation.
Use core tests before the Android build. Verify on the authorized phone and restore
its previous IME/preferences and sleeping display after every test session.

## Results

- Before the fix, the new URL test failed to expose `Switch to English` after
  attempting Chinese selection and restarting the editor. The compiled baseline
  EditorPolicy bytecode was checked: URI and NO_SUGGESTIONS still forced literal
  mode. This is a policy/lifecycle regression, not a dictionary quality test.
- Core verification: all 19,526 assertions passed before the Android build.
- Fixed build: debug app, instrumentation APK and Android lint passed. Dictionary
  assets are unchanged and existing asset/model/Rime integrity checks pass.
- Targeted phone run: seven EditorIntegrationTest methods and six visible-keyboard
  tests; 12 passed and the existing hide/restart test could not find Space in the
  combined run. That same test passed when rerun alone. The two new Chinese-input
  tests passed, including the exact Chrome field flags, empty-composition restart,
  switching back to literal English and private/no-suggestions input.
- Separate live Chrome automation attempts were inconclusive. Another authorized
  SHINE AAC task confirmed simultaneous use of the same phone and explained the
  unexpected `io.iris.phonetarget` foreground activity. Those runs cannot support
  product behavior claims; no IME workaround was added for that interference.
  Device operations were paused and the phone released to that task.
- After exclusive access was returned, all 13 targeted tests passed together.
  The separate Chrome test then passed: Chinese selected through the visible IME,
  `nihao` entered, and Space committed `你好` in Chrome's address bar without
  submitting a search. The final debug app SHA-256 is
  `74b4de18e2664b308a47de189267d99b8399608504bc4f3d6efaa37e36d1ed44`.
- The Chrome test initially failed before address editing, then at a coordinate
  tap on the language switch. It now uses visible accessibility ACTION_CLICK,
  matching the established keyboard test harness. This validates live editor
  integration; the separate existing gesture/touch tests cover physical input.
  No production changes were made in response to these harness failures.
- Final cleanup restored the Samsung IME and MinIME settings/learning. Display
  verification reported `mScreenState=OFF`; the phone was released to SHINE AAC.

The fix preserves English URL entry, secure/numeric restrictions and private-field
learning rules. Language selection and word-gesture eligibility are separate view
inputs: allowing Chinese selection must not turn on English word gestures in
literal fields. Existing view-test calls were updated to express the same gesture
eligibility they had before this signature change.

`ChromeBrowserTest` is an explicit smoke test rather than part of the generic
device list. It uses Android's multi-window accessibility API to see both Chrome
and the IME, selects Chinese, types and commits a phrase in the address bar, and
does not submit a search. Run it only with exclusive access to the authorized phone:

```powershell
./tools/test-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk `
  -TestClass dev.minime.ime.ChromeBrowserTest
```

The runner restores the prior IME/preferences and sleeps the display. The caller
must also inspect the final display state, and coordinate exclusive device access
with other active tasks before invoking it.

Saved logs: [baseline failure](chrome-input-policy/baseline-phone.txt),
[initial combined run](chrome-input-policy/fixed-phone.txt),
[isolated restart check](chrome-input-policy/restart-isolated.txt),
[exclusive targeted run](chrome-input-policy/exclusive-phone.txt), and
[successful live Chrome check](chrome-input-policy/chrome-browser.txt).
The exclusive combined log includes an earlier Chrome harness failure after its
13 passing targeted tests; the final separate Chrome log records the resolved run.
