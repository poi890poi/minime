# Release hardening — September 22–25, 2026

**Not cleared for public release.** This report concerns 0.8.8 (38), application
`app.minime.keyboard`. The original packaged runtime was `995b535`; subsequent
admitted changes and their actual package evidence are recorded below. Candidate quality was measured
on baseline `fd728d5`; the accepted rendering change does not alter dictionary or
ranking logic. Evaluation-only commit `1231638` adds evidence. Earlier publishing checklists are historical evidence,
not certification of this build. No Play upload or GitHub CI was used.

The [grammar-source replacement screen](../source-replacement/PRODUCTIVE-GRAMMAR-RESULTS.md)
rejects three metadata-only replacements. The broadest preserves annotated
auxiliary coverage on 40 new documents and passes 486 engine-contract episodes,
but loses mixed-mode prefix coverage and changes one automatic Space output.
No source or runtime change is admitted, and the EWT rights item remains open.

The [accepted-build language-specific replay](LANGUAGE-BASELINE-RESULTS.md)
completes 2,692 injected actions across four modes. All raw/Space frames are
observed and their aggregate tails meet the stated budgets in this sample;
Chinese/Taiwanese/Japanese candidate p95 remains above 50 ms. This is one shard
and one session per mode, not large-sample or human-touch release acceptance.

The subsequent [three-language stage diagnostic](LANGUAGE-STAGES-RESULTS.md)
completes 2,354 further injected actions with all raw/Space submissions observed.
Request-to-main delivery averages about 19–21 ms, while aggregate provider work
averages 0.8–5.2 ms per call. This localizes an unaccounted scheduling/queue interval
without establishing which queue causes it. Hooks perturb timing, so no speedup
or release-latency acceptance is claimed. Production remains unchanged; a separate
queue-boundary measurement is planned before another performance intervention.

The completed [queue-boundary measurement](QUEUE-STAGES-RESULTS.md) localizes
8–11 ms mean result-delivery wait across all three modes, versus about 0.16 ms
worker deadline overshoot. All 2,354 injected raw/Space submissions are observed;
three Taiwanese pipelines are cancelled. Production is unchanged. The next
isolated experiment marks decoder-result messages asynchronous while retaining
the 8 ms scheduling delay and requiring repeated unhooked tail comparisons.

That [asynchronous-delivery experiment](ASYNC-DELIVERY-RESULTS.md) is now rejected:
all four contracts and 1,856 injected raw/Space observations complete, but fast
Chinese Space p95 worsens by 4.33/4.20 ms in the paired runs, with repeated
Taiwanese acceptance-tail costs too. Some candidate gains do not meet the frozen
cross-mode admission rule. Active sources are restored; the trial and aggregate
evidence are archived outside Android source sets. No release gate is waived.

The complete [system trace](SYSTEM-QUEUE-RESULTS.md) accounts for all 665 Chinese
queued results. Window relayout overlaps 56.52% of their summed waiting duration;
this points to the floating composition popup but does not identify its ownership
causally. A separately frozen diagnostic ablation will test that hypothesis.
No runtime change or release-latency acceptance follows from instrumented timing.

The [compiled-validator experiment](VALIDATION-PATTERN-RESULTS.md) is rejected.
Despite lower isolated ART validation cost and identical desktop predictions,
Taiwanese raw/Space tails and fast Japanese raw tails worsen in both phone pairs.
All 1,856 injected raw/Space actions are observed, but this is not physical touch
or large-sample acceptance. The trial is archived; accepted runtime remains
`6bffd17` / publication `e89a337`, including the two quality changes below.

The [phrase-derived pronunciation change](../chinese-recovery/PHRASE-READING-INTEGRATION.md)
passes its declared independent reading screen: reference-supported Space choices
305 → 313 of 405; a new 28-query Google diagnostic agrees on 5 → 12 defaults.
English retention and full glyph-reading access are preserved, while broad
first-eight whole-target retrieval has ten net encyclopedic losses. Native/core
comparisons preserve first-page metrics, exact Pinyin/Zhuyin access is intact,
binary size is unchanged, and three packaged integration tests, including 28
pronunciation cases, pass. Conditional frequencies are integrated.

The [single complete-match preference](../chinese-recovery/COMPLETENESS-SINGLE-RESULTS.md)
is also admitted after rejecting all-complete grouping. Complete-reading Space
support improves 313 -> 337 of 405, while conversation/essay first-eight whole
and compatible coverage is preserved. Some abbreviated targets lose first place;
those tradeoffs remain documented. A frozen 28-case Google diagnostic has 13
agreement gains and one loss; this is not population accuracy. Core and pinned
native checks pass, all 24 language assets are unchanged, and the actual
non-debuggable candidate passes 55 spelling/Space episodes plus four-mode and
English smoke tests. Original phone state is restored, display OFF verified,
and ownership explicitly released. Broader release gates remain open.

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

The [explicit Latin casing fix](../source-replacement/EXPLICIT-LATIN-RESULTS.md)
is accepted: title/all-caps input now uses the same complete English vocabulary
as its already-classified Latin intent. All 37,336 source-derived mode checks
agree on completion inventories, while 6,144 lowercase English probes and 4,608
Chinese chat conditions remain identical. Core and pinned desktop checks pass;
two production-activity phone tests pass on a non-debuggable test-signed APK.
All 24 packaged asset files are unchanged. This is a scoped completion fix, not
clearance of broader language quality, latency, rights or platform release gates.

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

The subsequent [language-specific timing screen](language-timing/RESULTS.md)
keeps font paging out of this release. Five guarded sessions cover 4,258 actions
with all raw-editor/Space submissions observed; Japanese trial timing is worse,
Taiwanese results are mixed and repeated accepted-baseline sessions vary materially.
720 labelled queries and an exact action-sequence validator now replace the shared
eight-spelling workload for new tests. Only one shard in two modes has been run;
no full-corpus, human-touch or release-latency acceptance is claimed.

## Quality loop

The [expanded-grid allocation trial](expanded-viewport/RESULTS.md) improves
opening but is rejected: visible scroll-frame p95 rises from 16–19 ms to
83–85 ms when appending views. All candidate/default hashes and 20 integration
checks pass, which does not excuse the scrolling regression. A shared attached
frame harness now measures both opening and deep scrolling on either runtime.
The [follow-up geometry trials](expanded-viewport/GEOMETRY-RESULTS.md) are also
rejected: row increments increase scroll effort, and viewport increments retain
36–38 ms scrolling p95 above the fixed 33 ms budget. All runtime prototypes were
removed; accepted production remains `1ea175a`.

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
