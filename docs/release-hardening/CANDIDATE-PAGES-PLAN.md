# Candidate font-validation pages — development experiment

Type: performance experiment with a presentation API change. Baseline runtime is
`1ea175a`; no dictionary, ranking, learning, or font-support rule changes.

The accepted callback profile spends a Chinese p95 of 15.17 ms in font checks,
including candidates outside the 24-item strip batch. A warm code-point cache
micro-optimization saved too little to justify adoption. This experiment instead
defers work that the current viewport does not request.

Contract: finish ranking first. Resolve the existing preferred choice and its
automatic-correction eligibility immediately. Keep an immutable source snapshot
and validate its remaining entries in order, on demand. A requested prefix must
equal the same prefix of today's complete readable list, including paired-form
fallback, exact-raw recovery, and metadata. Asking for everything must return
everything. Old presentation snapshots must remain independently pageable while
a newer query is pending. No background publication, extra ranking pass, timer,
candidate cap, or production vocabulary change is part of this experiment.

Risks: changed index mapping, stale selection, hidden alternate forms, early
fallback before an unreadable run, unbounded work when most entries cannot be
drawn, and expansion/tap latency transferred from typing. Test these explicitly.
The existing complete-list API stays complete. The Android strip requests its
current batch plus one readable lookahead; expansion still requests all entries.
Identity-based acceptance searches only as far as the displayed match. New input
replaces the engine's page without mutating the old view snapshot.

First compare a standalone page implementation to the unchanged eager filter on
deterministic generated lists, blocked-font patterns, paired alternatives,
preferred positions, and page sizes. Then compare actual core engine outputs,
Space/Enter, partial selection, mode/editor resets, and stale async results.
Use the normal full core and pinned desktop tests before any app build.

Only a successful core experiment proceeds to the phone: paging/stability tests,
same frozen A/B/B/A typing replay, and expanded-list/selection checks. Record
missed frames as well as conditional percentiles. Reject incorrect output or
repeatable raw/acceptance regressions. A probe-count reduction is work avoided,
not milliseconds saved or language accuracy. The short replay cannot certify
release performance acceptance; retain an inconclusive result outside production.

Follow-up frozen after the first A/B/B/A run, before additional measurements:
the Chinese 150 ms candidate tail improves in both pairs, but thermal status
changes from 0 to 1 during B1 and stays 1 through A2. Do not attribute all later
baseline slowdown to paging. Repeat B then A, starting each only after thermal
status is 0 and battery temperature is below 34 C. Before timing, measure the
shifted work with CandidatePageCostTest: the same 46 frozen prefixes as the
application diagnostic, two complete passes, persistent real font cache, real
LocalLearning, precomputed unchanged providers, and offscreen view measure/layout/
draw for strip and expanded grid. Keep first/second passes separate. Record
apply/strip/expand/deep-selection costs, font probe counts, full ordered candidate
and preferred identity hashes. The test is detached rendering work, not physical
touch or panel presentation. Require identical identities; report any expansion
regression explicitly. No further implementation changes are included in this
comparison. Programmatic paging checks now wait for actual viewport reachability
within a fixed 40-scroll budget, not just allocation followed by one scroll.
