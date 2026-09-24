# Reading-prior loss diagnosis

September 24, 2026. Diagnosis only; no production data or ranking change.

The earlier source-reading-prior trial failed its frozen no-lost-target gate.
Its six lost labelled episodes cover two one-letter queries and two single
characters. They are not six distinct phrases. This follow-up does not rewrite
that result or silently relax its gate.

Hypothesis: the source penalties demote alternate readings below the bounded
abbreviation result window, while complete source spellings remain available.
Trace this at the shared PhoneticDictionary boundary using the unchanged accepted
runtime and both byte-pinned TSVs from weights-manifest.json. Do not change the
query budget, weights, dictionary entries or model based on the lost labels.

Before inspecting new outputs, freeze the scope: all 499 previously registered
syllable/prefix inputs, all single-Han source reading/output pairs, and all
complete source spellings for each identity lost from those queries. Record
complete-spelling failures, gains/losses by input length and glyph/phrase size,
and whether each lost reading actually had a source penalty. A source lookup is
a coverage contract, not semantic precision, conversational frequency or a fresh
holdout. Labels are used only after dictionary lookup.

This is a test/tooling change. It cannot alter app behavior, preferences or
assets. Generated per-query inventories stay in ignored artifacts. Keep only
aggregate results and reproducible code in the repository. Production admission
still requires a new explicit behavior proposal, broader quality evidence and
performance checks; this diagnostic alone cannot clear the release gate.

After the fixed audit, all 196 removed query identities retained all complete
spellings and matched a source reading penalty. A single causal control now
raises only ReadingUnitIndex's 128-candidate cap in an isolated class to an
effectively unbounded value, then repeats the identical comparison. If the
removals disappear, that supports bounded-list demotion rather than lost source
entries. This deliberately expensive control is not a proposed app change;
search traversal budgets and every other policy remain unchanged.
