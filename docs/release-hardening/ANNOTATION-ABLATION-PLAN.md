# Attribute composition-popup window cost before redesign

Frozen September 25, 2026, before the ablation build or phone run.

Type: diagnostic performance experiment. Hypothesis: the floating composition
annotation's per-spelling PopupWindow resize causes much of the window relayout
overlap in the complete system-queue baseline. Owner: KeyboardView's annotation
placement only. No dictionary, ranking, scheduling, acceptance, keyboard layout,
font, or test-workload changes.

Create an isolated copy of the system-queue diagnostic overlay. At the start of
`placeAnnotation`, dismiss and return through a diagnostic-only helper that always
returns true. Keep annotation text updates, measurement elsewhere, and candidates
unchanged. This intentionally removes a visible raw-text affordance for diagnosis;
it must not enter ordinary production sources or release packages.

Use the same Chinese shard 0, both frozen cadences, identical test APK, trace
markers, trace configuration and analyzer. Verify all 24 language assets match,
production sources are unchanged, and the sole source delta is the popup ablation.
Require complete workload/marker accounting and no trace warnings. Compare total
relayout time during request waiting, relayout count/duration across the measured
interval, per-request queue wait, and raw/Space observation counts. Report all
changes; do not label a traced single-pair timing difference a shipping speedup.

A substantial relayout reduction supports popup ownership; unchanged relayout
cost rejects this explanation. A mixed result remains inconclusive. This screen
can justify a preservation-oriented design experiment, never removal of the
buffer. Any design must preserve raw-text tapping, stable keyboard/editor height,
outside-editor touch behavior, accessibility and lifecycle/rotation behavior;
full-width invisible touch interception is unacceptable. A later admission needs
repeated uninstrumented comparisons across modes and the existing latency gates.

Obtain explicit phone acknowledgement, hold the shared mutex, retain bounded
trace/storage/deadline checks, restore original APK/preferences/learning/IME,
verify display OFF, remove only owned trace files, and explicitly release.
