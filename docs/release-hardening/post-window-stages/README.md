# Prepared diagnostic, not a release APK

The [frozen plan](../POST-WINDOW-STAGES-PLAN.md) reuses the existing queue and
candidate-stage observers together on the admitted single-window runtime.
`prepare.py` copies current Java sources into an ignored overlay, invokes the
original queue-overlay generator, redirects its source sets, and changes only
the diagnostic replay to enable the existing stage hooks. It refuses overwrite
and verifies that active sources remain unchanged. `verify.py` checks package,
source, asset, corpus, manifest and test-key boundaries after the local build.

The prepared diagnostic builds locally in 22 seconds. Relative to the admitted
test fixture, only `classes.dex` differs among non-signature ZIP entries; all 24
language assets and native libraries remain equal. The fixture is non-debuggable,
has no Internet permission or profileable flag, and includes a test-only editor.
Its test-key signature is verified. Neither app nor test APK is a distribution.
Binary identities are recorded in `binaries.json`.

The existing queue observer passes its desktop identity, chronology, terminal
state and bounded-overflow contract; six queue-report fixtures pass. Existing
core evidence remains 1,770,165 assertions, with 13,014 pinned desktop inputs and
11,272 native queries; no core/provider/data source changed in this diagnostic.
Those counts prove neither language accuracy nor Android performance.

No phone results are claimed by this preparation. Obtain a separate explicit
reservation before installation or replay. Preserve cancelled, ambiguous and
missing observations; keep raw spelling/stage files and device dumps local.
Use the strict language timeline and queue reporters, publishing only aggregates
and hashes. Earlier unhooked comparison results remain a separate measurement.

Build with the generated `artifacts/post-window-stages/diagnostic.init.gradle`,
ordinary local release plus debug-instrumentation tasks, then sign the fixture
with the existing phone test key. The generated source sets must never be used
when creating an ordinary release package. No hosted CI or upload credentials.
