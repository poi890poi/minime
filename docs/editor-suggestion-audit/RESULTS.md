# App-specific suggestion audit — 2026-09-16

Keep 5.26.361.01.90 reproduced the missing English completions in both its title
and note body. The body advertises inputType `0xac001`, which includes Android's
NO_SUGGESTIONS hint. MinIME interpreted that hint as literal-only English, but
retained English suggestions in its mixed modes. This is an editor-policy defect,
not missing dictionary data.

The fix retains manually selectable dictionary candidates in ordinary text
fields and uses NO_SUGGESTIONS to disable optional automatic spelling correction.
URL/email, password and numeric literal policies are unchanged. Learning remains
disabled in private fields. There are no package-name exceptions or data changes.

## Verified comparisons

| Editor / probe | Before | After policy fix |
| --- | --- | --- |
| Keep title and body, English `pronun` | No candidates | Pronunciation, Pronunciations |
| Keep title and body, English `tomorr` | No candidates | Tomorrow, Tomorrow's |
| Keep, Chinese `nihao` | 你好, 妳好, 您好… | Unchanged |
| Keep, Taiwanese packaged everyday reading `ache` | a-ché, a-chek, at-chè… | Unchanged |
| Keep, Japanese `konnichi` | こんにち, 今日, コンニチ, こんにちは | Unchanged |
| Chrome local textarea, all four modes | Not measured before | Candidates present for all nine probes, including after Space |
| Keep title/body and Chrome, English `thank ` | Native restart dropped context | you, god remain available after Space |

Keep: 18 field/mode/input observations, including four English completion rows
restored and two added next-word probes; the other 12 candidate lists match the
baseline. Chrome: nine observations on a
disposable loopback-served page. These are editor-boundary probes, not estimates
of dictionary accuracy or coverage. English prefixes are also exercised in each
mixed mode. Taiwanese probes are selected reproducibly from the packaged source.

The native matrix checks 84 unfinished-input combinations (four requested modes,
seven editor profiles, three inputs), plus four accepted-word context probes.
Normal, multiline/capitalized, autocomplete and private-field completion lists
survived editor restart. URI automatically selects English, so those rows do not
establish explicit non-English conversion in URI fields. Password rows remain
literal. Before the lifecycle fix, all four next-word contexts disappeared after
restart. After the fix, all 88 candidate lists match their respective post-restart
lists. Comparing old and new APKs, only the three English NO_SUGGESTIONS rows and
four context-restart rows changed; all other lists are unchanged.

## Same-field restart fix

`onStartInput` previously resumed only nonempty composition. A same-field restart
after Space therefore reset accepted-word context. It now preserves that context
only when field identity, policy, cursor and a suffix of IME-owned committed text
match. The suffix is bounded to 96 UTF-16 code units, stays in memory, and is
cleared on session completion. Password/numeric/direct fields do not retain it.
External replacement, cursor movement and a new input session still clear context.
The shared core's start/reset semantics are unchanged.

The new regression **failed on the saved old APK**, with expected `[you, god]`
versus actual `[]` after restart. It passed on the fixed APK, including negative
cases for same-length external replacement, cursor movement and field changes.
Ownership checks cover the size bound, surrogate boundaries, deletion and rewind.

## Verification and limitations

- Shared core: `tools/test-core.ps1` passed (308,652 assertions; not accuracy).
- Android: eight EditorIntegrationTest cases and the NO_SUGGESTIONS regression
  passed. The regression enables correction globally and proves the editor hint
  still prevents automatic `teh` → `the`, while manual completion remains usable.
- Real Keep audit passed; real Chrome audit passed after fixing test focus.
- The first native matrix omitted text-delivery assertions and is invalid as
  app-quality evidence. Its replacement verifies each character reaches the
  intended editor. Chrome's first attempt failed to focus its textarea; physical
  editor taps plus waiting for the keyboard corrected that fixture failure.
- A later Keep fixture repeat failed because a physical tap missed the title;
  native accessibility focus/click fixed delivery. An already-focused field can
  return false from ACTION_FOCUS, so the fixture verifies actual typed text rather
  than treating that return value as an app defect. The final Keep run passed.
- A character left by the failed fixture was removed. Keep's note list was then
  verified empty. The previous IME/preferences and display OFF were verified, and
  the shared phone reservation was explicitly released.
- No Google Zhuyin comparison in this audit. No corpus/ranking changes, release
  build, phone performance claim or hosted CI run.
- These app fixtures disable Rime to isolate editor integration. No desktop Rime
  rerun was needed because dictionaries, decoders and ranking did not change.
- Phone runner restored preferences and the previous Samsung IME, and verified
  display OFF after each test session, including failures.

The Chrome fixture binds only device loopback. Its INTERNET permission is in the
debug manifest only; the release manifest and offline product policy are unchanged.
The current merged release manifest was checked and has no INTERNET permission.
Privacy wording was updated consistently in the app, repository and generated
HTML to explain validation of recently committed text as well as composition.

Sanitized probe outputs: [observations.json](observations.json). They contain only
test inputs, field/mode labels and visible candidate lists, not personal notes.

Commands (select app audits explicitly; they require installed apps):

```powershell
./tools/test-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk `
  -TestClass 'dev.minime.ime.KeyboardInteractionTest#testEditorSuggestionAudit' `
  -TimeoutSeconds 900 -Reports @('editor-suggestion-audit.json')
./tools/test-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk `
  -TestClass 'dev.minime.ime.AppEditorAuditTest#testKeepSuggestionAudit,dev.minime.ime.AppEditorAuditTest#testChromeSuggestionAudit' `
  -TimeoutSeconds 300 -Reports @('keep-suggestion-audit.json','chrome-suggestion-audit.json')
```
