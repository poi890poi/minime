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
| Chrome local textarea, all four modes | Not measured before | Candidates present for all eight probes |

Keep: 16 field/mode/input observations, including four English rows restored;
the other 12 candidate lists match the baseline. Chrome: eight observations on a
disposable loopback-served page. These are editor-boundary probes, not estimates
of dictionary accuracy or coverage. English prefixes are also exercised in each
mixed mode. Taiwanese probes are selected reproducibly from the packaged source.

The native matrix checks 84 unfinished-input combinations (four requested modes,
seven editor profiles, three inputs), plus four accepted-word context probes.
Normal, multiline/capitalized, autocomplete and private-field completion lists
survived editor restart. URI automatically selects English, so those rows do not
establish explicit non-English conversion in URI fields. Password rows remain
literal. All four next-word contexts disappeared after restart; that separate
lifecycle defect is tracked in PLAN.md.

## Verification and limitations

- Shared core: `tools/test-core.ps1` passed (308,652 assertions; not accuracy).
- Android: seven EditorIntegrationTest cases and the NO_SUGGESTIONS regression
  passed. The regression enables correction globally and proves the editor hint
  still prevents automatic `teh` → `the`, while manual completion remains usable.
- Real Keep audit passed; real Chrome audit passed after fixing test focus.
- The first native matrix omitted text-delivery assertions and is invalid as
  app-quality evidence. Its replacement verifies each character reaches the
  intended editor. Chrome's first attempt failed to focus its textarea; physical
  editor taps plus waiting for the keyboard corrected that fixture failure.
- No Google Zhuyin comparison in this audit. No corpus/ranking changes, release
  build, phone performance claim or hosted CI run.
- Phone runner restored preferences and the previous Samsung IME, and verified
  display OFF after each test session, including failures.

The Chrome fixture binds only device loopback. Its INTERNET permission is in the
debug manifest only; the release manifest and offline product policy are unchanged.

Commands (select app audits explicitly; they require installed apps):

```powershell
./tools/test-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk `
  -TestClass 'dev.minime.ime.KeyboardInteractionTest#testEditorSuggestionAudit' `
  -TimeoutSeconds 900 -Reports @('editor-suggestion-audit.json')
./tools/test-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk `
  -TestClass 'dev.minime.ime.AppEditorAuditTest#testKeepSuggestionAudit,dev.minime.ime.AppEditorAuditTest#testChromeSuggestionAudit' `
  -TimeoutSeconds 300 -Reports @('keep-suggestion-audit.json','chrome-suggestion-audit.json')
```
