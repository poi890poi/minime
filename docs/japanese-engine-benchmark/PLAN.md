# Japanese conversion and completion experiment

Type: evaluation tooling and source audit. Baseline: MinIME 52e083d.
No production vocabulary, ranking, APK, preferences or device changes.

## Frozen contract (before engine evaluation)

Compare the shipped Japanese/English shared core with Kazuma Naka's existing
C++ kana–kanji converter, pinned at f8af0c2e6a7538f83e7483e9171ebec04fd963a0.
Use its non-neural CommonPrefixOnly A* conversion, top 8, documented beam 50;
do not tune these settings on reference outputs. This uses Mozc dictionary data,
not the Mozc converter. Audit Mozc and azooKey as alternatives; mark unrun engines
explicitly. Third-party Hechima release lookup returned HTTP 404 and cannot
establish a reproducible Mozc baseline here.

References never enter a provider. Providers receive typed romanization/kana
only, and the baseline may use its own accepted context. A simulated oracle user
uses references only to choose among returned candidates. Missing candidates
remain failures; never insert reference Kanji as a successful fallback.

Freeze new dialogue splits using SHA256('completion-v1:'+source+':'+document).
Exclude all conversations in the preceding conversation split, plus schema-read
00001/001. Per source choose 16 development and 48 holdout conversations; choose
eight turns per conversation by the same salted hash, before checking readings.
Keep all selected turns, including unreadable/OOV failures. Preserve Japanese
clauses, source word boundaries, literal separators and document lineage.
SudachiPy 0.6.10 / core 20250129 provide silver readings; jaconv 0.4.0 provides
independent romaji. Check round trips, count exclusions, and report original-kana
versus silver references. All earlier corpora are regression, not fresh holdout.
Also reuse all 200 human-reviewed AJIMEE-Bench items (CC-BY-SA-3.0), with its
acceptable outputs and minimum character error rate, as an external encyclopedia
conversion test. Its example published in README is already exposed.

## Actions and measurements

For each utterance, reset the session, then replay source clauses in order.
Policy A types a whole clause and chooses an exact whole-input candidate among
the first eight visible slots (including MinIME's raw slot). If missing, delete
the composition and retry each independent Sudachi word. Policy B enters each
word separately from the outset. Each successful selection is committed through
the adapter; MinIME uses CompositionEngine.selectCandidate. Check emitted text
and remaining raw. Literal separators are explicit literal keystrokes, without
assuming a physical symbol-page navigation cost. Failed words are cancelled,
never replaced with gold. Full-turn success requires every source part to work.
Source token boundaries and perfect candidate selection are oracle assistance,
not measured human efficiency. Whole-clause and word-start policies share these
references; neither is presented as an optimal typing strategy.

Separately query complete, half, three-quarter, omission, adjacent-key and
transposition inputs. Error positions derive from the salted item hash, with
unchanged perturbations identified. These are simulated errors, not touch data.
For delayed-error recovery, type the changed reading before conversion; if the
intended output is unavailable, delete and retry the correct reading. Report
failure, typed keys, selection actions, deletion/retyping costs and rank; report
costs for all attempts and successful attempts separately to avoid survivorship.

Time actual uncached per-key engine work separately from process transport and
simulated action costs. Run three isolated processes per engine, alternating
order; select 64 development clauses by hash, bounded to 48 romanization keys,
for repeatable latency. Report cold load, p50/p95/p99/max, process memory and
required data size. These are desktop feasibility measurements, not Android
latency or touch-to-display performance. Use the existing product acceptance
requirements for a later native phone gate; no desktop number waives that gate.

## Decision gates and limitations

An engine is eligible for a native integration experiment only if it improves
held-out clause completion in both conversation genres, preserves interpretable
failure reporting, has traceable licensing and fits a bounded resource budget.
Desktop warm p95 >50 ms or required data >64 MiB blocks an as-you-type pilot at
these settings. Faster results are necessary, not sufficient for phone approval.
No ranking tuning or production import follows automatically from this pilot.
Report partial-input losses, orthographic ambiguity, silver-reading errors,
context differences and full-turn failures. Preserve negative results.

Verify corpus disjointness/determinism, evaluator failure sensitivity, actual
selection state, core regression and source registry. Native Rime is unchanged;
run its existing gate for shared-core baseline confidence. No phone is used while
the SHINE reservation remains unreleased.
