# Loop: distinguish abbreviated matches from forward predictions

Baseline: `2363722`. This is a candidate-evidence bug fix with a ranking/acceptance
experiment, not a dictionary expansion. The reading-unit matcher has traversed
every stored syllable when it returns a terminal word, but labels omitted letters
with the same flag as flat-prefix predictions. Supplemental merging, placement,
and default selection consume that flag.

Hypothesis: distinguishing these matches can recover useful initial/mixed-reading
choices. Test the existing policy with corrected classification first. Do not land
it if it causes uncontrolled dictionary promotion or automatic acceptance.

Runtime evidence is limited to the typed reading, source reading boundaries,
source order/frequency, mode and existing context/preferences. Targets are only
post-run evaluation labels. No entry exceptions, dictionary changes, larger search
budget or tuned score weights.

Gates: existing core and pinned desktop regressions; whole/partial selection and
stale-result invariants; report first-eight intended coverage and useful slots on
the frozen Chinese corpus by genre, plus English first-five/eight and Space
changes. No regression in English-mode inventory. Record negative outcomes.
Timing is host lookup time, not phone touch latency. No new phone claim or APK is
part of this core loop. Existing corpora are development/regression evidence,
not a fresh holdout or a semantic precision oracle. No improvement claim from
assertion counts. Binary source assets must remain readable without regeneration.
