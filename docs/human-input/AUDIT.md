# Human input precision test expansion

Type: test/tooling change, baseline 7816734 / MinIME 0.5.4. Existing touch tests
cover four centered pairs and four near-edge releases. Add deterministic spatial
and temporal perturbations through real Android ViewGroup dispatch, then a small
wall-clock replay through the installed IME. No production tuning is proposed.

Freeze seeds and profiles before execution. Intended keys/text are the oracle;
the runtime receives only MotionEvents. Use actual measured key geometry to map
portable coordinates, never emitted output to derive the intended text. Sample
English and full/initial/mixed Pinyin from the existing attributed corpus by hash,
with disjoint development and holdout identities. Source text is evaluation-only.

Gates: all interior taps, small release drift, all ordered letter pairs with two
thumb release orders, deliberate cancellation, explicit vertical slides and
correction sequences must retain their expected commands. Report substitutions,
insertions/deletions, unexpected literal/trace commands, and first divergence.
Initial downs outside the intended key are exploratory: report their errors but
do not pretend the intended key is uniquely recoverable. Keep full failure events
and replay seeds, configuration and per-case outcomes, including negative results.

Virtual MotionEvent timestamps test ordering, not handler latency/long-press time.
The separate installed-IME replay uses actual waits, uneven cadence, off-center
downs, drift, cancellation/retry and typo/backspace recovery. Check raw spelling
and composing spans after each segment so Chinese cannot be silently committed.
Retain existing candidate/caps/slide/hold regressions as adjacent controls.

Limit claims to this synthetic envelope on the authorized ARM64 phone. These
distributions are not fitted to human participants and cannot establish a human
error rate, sensor accuracy, Google parity or held-key scheduling from virtual
timestamps. No ranking/data changes; core ranking evaluation need not be repeated
for test-only Android dispatch changes. Restore preferences/IME, sleep and verify
the display even on failure. Keep test corpus and telemetry out of app APKs.

The first standalone matrix passed. A combined run then lost its synthetic
activity between configuration batches (landscape stopped after 1,638 cases,
with zero input mismatches). Preserve that incomplete run. Each configuration
now starts a fresh explicit debug activity, verifies its attached live keyboard,
and finishes it afterward. The frozen profiles/seeds and acceptance criteria
remain unchanged. Slow real-time tests use separate entry points so the default
interaction batch does not silently exceed its existing timeout.
