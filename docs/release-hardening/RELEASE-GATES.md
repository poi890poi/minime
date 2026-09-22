# Release hardening — September 22–23, 2026

**Not cleared for public release.** This report concerns 0.8.8 (38), application
`app.minime.keyboard`, runtime commit `fd728d5`. Evaluation-only commit `1231638`
does not alter the runtime. Earlier publishing checklists are historical evidence,
not certification of this build. No Play upload or GitHub CI was used.

## Quality loop

The new Taiwan chat-derived evaluation adds 4,608 complete, initial and mixed
spelling conditions. These edited excerpts are not intact conversations or a
fresh conversation holdout. Source omissions, reading ambiguity and dictionary
overlap are reported in [QUALITY.md](QUALITY.md).

Extending duplicate-source rank protection gained 15 and lost 44 intended phrases
among the first eight Chinese candidates in the broad corpus, with no first-choice
gain. The trial was rejected; production ranking is unchanged. More tests are an
improvement in evidence, not proof of better language quality.

Core contracts: 887,604 assertions. Pinned desktop evaluator: 13,014 rows, 11,272
native queries. Neither count is language accuracy or a phone performance result.

## Binary checks

| Check | Result |
|---|---|
| Local release APK/AAB build | PASS |
| Release lint | 0 errors, 15 warnings |
| APK and AAB signature, expected upload certificate | PASS |
| Bundle validation, 24 language assets, four ABIs | PASS |
| Static 16 KB ELF and APK ZIP alignment | PASS |
| Non-debuggable production manifest, no Internet permission | PASS |
| Android 16 / 16 KB-page runtime | NOT MEASURED; phone is Android 13 / 4 KB |
| Play-generated delivery and upgrade | NOT MEASURED |
| Source rights | BLOCKED: existing UD underlying-text review remains open |

The APK SHA-256 is
`8a54391407771f3093cd5fe38b02328b492024210b563fd65f0f46b3f370d4bf`.
The AAB SHA-256 is
`157889b303e7d0af98f8ee6be7582059152d2e1ca592dc45488b757fa6187de7`.
[Package evidence](package/inventory.json) records asset/native hashes;
[signature verification](package/apk-signature.txt) identifies the upload certificate.
The AAB verifier reports a valid signature with the expected self-signed certificate;
its trust-chain/timestamp warnings are retained in the evidence, not suppressed.

Phone release tests use a copy signed with the existing installation's test key
to avoid uninstalling user data. All 58 non-signature ZIP entries match the upload
APK byte for byte. [Parity record](package/payload-parity.json). This exercises
the release payload, not the Play signing chain or an upload-signed upgrade.
Code 38 is a validation build; increment before a new Play upload.

## Phone verification

The exclusive SHINE handoff and shared Windows mutex cover every operation.
Each session restores the original APK, preferences and IME, then verifies display
OFF. Private preference backups and complete Android state dumps remain local.

An initial release typing harness failed to open the IME. Android reported MinIME
selected and show requested, but no connected service. Keeping the prior IME
selected until the instrumented activity exists makes the same release payload
connect and type. The dedicated `tools/test-release-device.ps1` preserves that
ordering and restores the supplied original APK before restoring preferences.

That first successful harness run also mislabeled a Chinese-mode observation as
English: changing preferences and restarting the same field intentionally preserves
the active language. The revised test uses the visible mode selector, checks its
badge, and rejects Han/kana in English. The earlier observation is excluded from
four-mode certification. These were test defects; neither establishes a production
language-mode regression.

The corrected release run passes all **15 tests**: settings/defaults/backup/mode
preferences plus the four-mode visible-key smoke test. The latter is one test
with four input/selection episodes, not a four-language coverage benchmark.
[Actual mode outputs](phone/release-modes.json) and
[instrumentation status](phone/release-instrumentation.txt) are retained.

The original broad debug suite stopped progressing after 16 completed checks and
was aborted through its owning runner, with cleanup verified. A partial run is
not a pass. Per-test raw instrumentation status is now recorded by the normal
runner, so subsequent stalls identify their method rather than only progress dots.
An isolated follow-up completes five touch-handling and nine candidate-stability
checks, then times out in `testStoredPhrasePrefixesAndSuffixEditing`. Its remaining
four checks did not run. [Incomplete run](phone/integration-timeout.txt).
The standalone prefix replay also exceeds its 600-second budget. Its screenshots
show progress, so elapsed silence must not be called a proven deadlock; the full
test still has no completed result. That gate remains incomplete.

The frozen Google/MinIME comparison completes 48 observations. Chrome and Keep
complete 27 editor/mode/query observations with candidates available in each.
Injected latency telemetry exposes a substantial Chinese main-thread delay;
see the [phone report and its explicit measurement limits](PHONE.md).

## Still required

- Close remaining candidate precision/recall gaps against Google Zhuyin; do not
  substitute assertion totals or a small phone sample for language quality.
- Finish independent conversation coverage for Taiwanese and Japanese.
- Measure physical human touch hit rates and actual presented-frame latency.
  Synthetic input and frame-submission telemetry cannot certify those gates.
- Complete the prescribed large latency sample and tail/missed-frame checks.
- Exercise Android 16 / 16 KB runtime and a Play-generated upgrade.
- Resolve the [existing source-rights decision](../play-publishing/RIGHTS-REVIEW.md).

The next upload must be built and checked after any accepted runtime changes.
