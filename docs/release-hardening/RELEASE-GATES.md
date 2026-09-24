# Release hardening — September 22–24, 2026

**Not cleared for public release.** This report concerns 0.8.8 (38), application
`app.minime.keyboard`, packaged runtime commit `995b535`. Candidate quality was measured
on baseline `fd728d5`; the accepted rendering change does not alter dictionary or
ranking logic. Evaluation-only commit `1231638` adds evidence. Earlier publishing checklists are historical evidence,
not certification of this build. No Play upload or GitHub CI was used.

September 24 follow-up: [query-local sort keys](ORDER-RESULTS.md) improve fast
Chinese candidate-frame observations from 52/100 to 62/100 across two corrected
comparison pairs, without changing candidate order or dictionaries. Strict
submission before the next key release improves from 38/100 to 45/100. These
are timing observations, not language accuracy. General candidate p95 improvement
is not established and acceptance remains open. The replay now removes a hidden
main-thread wait before every key and observes raw frames through the next key
release; older nominal-interval timings must not be treated as equal-load results.
Core follow-up passes 888,180 assertions and the desktop output is unchanged.
No new signed release payload has been built for this follow-up.

The subsequent [zero-delay scheduling trial](SCHEDULING-RESULTS.md) is rejected:
Japanese candidate timing improves, but fast Chinese raw-text p95 worsens in
both comparison pairs and ordinary candidate gains are mixed. Four unhooked
runs capture all raw-editor and Space frames; this does not certify human touch
or release latency. The existing 8 ms scheduling delay remains in production.

The [unchanged-board layout trial](LAYOUT-RESULTS.md) is also rejected. A
redundant layout request is demonstrably removed, but repeated typing gains
are mixed and fast Taiwanese raw-text p95 worsens in both pairs. The guard is
archived; accepted runtime remains b47bb2a. Four further unhooked replays capture
all raw-editor/Space frames without establishing release acceptance.

The [exact English completion-existence query](EXISTENCE-RESULTS.md) is accepted:
880,100 comparisons preserve the original decision and desktop outputs are
identical. Repeated Chinese shared-observation timing improves, with modest
first-pair gains and a slower second baseline that limits causal attribution.
Unrestricted tail latency remains mixed; this does not clear the release gate.

The [learning-key binding trial](LEARNING-KEY-RESULTS.md) is rejected despite
lower isolated lookup cost: fast Chinese raw-frame p95 worsens in both phone
pairs, and candidate gains are conditional on fewer observed frames. Its runtime
changes are reverted; the accepted runtime remains 1ea175a.

The [candidate font-page experiment](CANDIDATE-PAGES-RESULTS.md) is held outside
production. It preserves full ordered results and improves slower Chinese
candidate p95 in three pairs, including a cooled repeat, but shifts work to
expansion and has mixed cross-language/acceptance timings. Its eight-query
workload cannot settle those tradeoffs. Broader language-specific timing inputs
are needed before admission; 12 integration checks and 92 ordered-result
comparisons do not establish release performance. Accepted runtime remains 1ea175a.

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
`a599a799369b5424e4d700e0aed8520b6133f0656cfee730f62d3e9ada021dc1`.
The AAB SHA-256 is
`3c7e8690adfcbfeb246cfbfb5e136ea3f28abbbbe8bd38e8cedeec55a5422304`.
[Final package evidence](final-package/inventory.json) records asset/native hashes;
[signature verification](final-package/apk-signature.txt) identifies the upload certificate.
The AAB verifier reports a valid signature with the expected self-signed certificate;
its trust-chain/timestamp warnings are retained in the evidence, not suppressed.

Phone release tests use a copy signed with the existing installation's test key
to avoid uninstalling user data. All 58 non-signature ZIP entries match the upload
APK byte for byte. [Parity record](final-package/payload-parity.json). This exercises
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

The corrected harness passes all **15 tests** on both the baseline and final release payload: settings/defaults/backup/mode
preferences plus the four-mode visible-key smoke test. The latter is one test
with four input/selection episodes, not a four-language coverage benchmark.
[Final mode outputs](final-package/phone-modes.json) and
[instrumentation status](final-package/phone-instrumentation.txt) are retained.
The final debug implementation also passes 14 paging/stability/input checks.

The original broad debug suite stopped progressing after 16 completed checks and
was aborted through its owning runner, with cleanup verified. A partial run is
not a pass. Per-test raw instrumentation status is now recorded by the normal
runner, so subsequent stalls identify their method rather than only progress dots.
An isolated follow-up completes five touch-handling and nine candidate-stability
checks, then times out in `testStoredPhrasePrefixesAndSuffixEditing`. Its remaining
four checks did not run. [Incomplete run](phone/integration-timeout.txt).
The standalone prefix replay also exceeds its 600-second budget. Its screenshots
show progress, so elapsed silence must not be called a proven deadlock; the full
test still had no completed result at that point. The September 24
[sharded replay](PREFIX-INTEGRATION-RESULTS.md) now completes all six tests and
20 episodes, preserving every original assertion and both field policies.
The frozen prefix-selection gate is complete; the old aggregate stall has no
proven runtime root cause and prolonged-session testing remains distinct.

The frozen Google/MinIME comparison completes 48 observations. Chrome and Keep
complete 27 editor/mode/query observations with candidates available in each.
Injected latency telemetry exposes a substantial Chinese main-thread delay;
see the [phone report and its explicit measurement limits](PHONE.md).

The accepted [rendering fix](RENDER-RESULTS.md) reduces Chinese raw-frame p95 from
148.8/559.3 ms in a repeated baseline to 26.8/33.2 ms at 150/60 ms key intervals.
Other modes' measured p95 increases roughly 2–7 ms in the short replays. Fresh
Chinese candidate p95 remains 170.9 ms at the slower interval, above its target.
This is a significant improvement, not completed performance acceptance.
The source/rights, Android-version, human-touch and larger-sample gates stay open.

The subsequent [candidate profiling loop](FONT-RESULTS.md) rejects worker font
warming: the main-thread tail improves, but end-to-end improvement does not
repeat and Japanese latency worsens in the final comparison. Production remains
at `995b535`. Its corrected fresh baseline measures Chinese candidate p95 at
118.96/75.89 ms for 150/60 ms typing; cross-session differences are not a speedup.
The earlier fast-frame probe stopped at finger-down before the next spelling
existed. Correcting that test observes all 50 fast Chinese baseline frames.
Earlier low frame counts do not establish missing suggestions. The target remains
50 ms; the correction does not relax it or clear release acceptance.

Final cleanup restores the original APK/preferences/IME and verifies display OFF.
The shared-phone reservation has been explicitly released to SHINE.
[Cleanup record](final-package/phone-cleanup.json). The `package` directory retains
the earlier baseline package evidence; `final-package` identifies the current build.

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
