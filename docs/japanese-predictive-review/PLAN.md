# Existing Japanese predictive mechanisms

Type: evaluation tooling and source-backed mechanism review. Baseline 1b3aa8d.
No production changes are authorized by a favorable lookup score alone.

Compare two existing APIs in the already pinned Kazuma C++ converter:
CommonPrefixPlusPredictive in the conversion graph, and the CLI's separate
print_prediction lexical completion. Keep top eight, beam 50, dictionary bytes,
costs, reference readings and raw-input adapters fixed. Use upstream predK=1.
Reference text is scored only after output; never supplied to an engine.

Source inspection raises a specific concern: graph predictive mode searches the
first kana at each position, permits nonmatching following kana, and excludes
words longer than the remaining input. Verify with actual graph-node counts and
outputs, rather than interpreting its name as full-reading completion. The
separate lexical API filters against the entire supplied prefix.

Development screen: hash-order the previous experiment's development clauses,
select 32 per conversation genre with valid readings and at most 24 raw keys,
then query full, half and three-quarter inputs. These are consumed development
data, not a fresh holdout. Freeze the probe file before evaluation. Save every
attempt, including timeout and unavailable output. Compare the same requests
through current MinIME and CommonPrefixOnly conversion.

Stop a candidate immediately on a 5-second query timeout or after three queries
exceed 250 ms. These are coarse feasibility rejection thresholds, not acceptance
targets. A surviving predictive candidate must preserve supplied phonetics,
improve partial clause coverage over current lookup without losing complete
coverage, and have desktop query p95 <=50 ms. Measure whole response transport
and engine work separately. No phone timing is inferred.

If neither API survives, retain the negative result and do not build an Android
integration. Audit the lexical API on word-level development probes as a distinct
use: phrase completion and dictionary word completion have different coverage.
Use the same deterministic per-source sampling and record that this is a
separate mechanism question, not a rescue claim for whole-clause prediction.
Only a surviving implementation proceeds to new document-disjoint holdouts and
three isolated timing passes. Never tune on old holdout labels or call them fresh.

Keep the original benchmark outputs immutable. Validate adapter output against
the pinned CLI, test stop/failure accounting and verify source/production ledgers.
No APK or device operation is required for an evaluation-only rejection. SHINE's
explicit phone release is still required before any later device operation.

## Follow-up frozen after the default-setting screen

The graph timed out on the first query; lexical completion exceeded the same
latency rejection criteria. Test one representation-independent optimization:
call the existing lexical API with predK equal to the entire supplied kana length.
That API already filters retrieved readings with starts_with(full input) before
scoring and sorting; searching the full prefix should produce the same set and
cost ordering while avoiding a broad one-kana subtree scan. Graph prediction is
not changed: its semantics are different, so this equivalence does not apply.

Use identical frozen probes and rejection thresholds; compare exact ordered
outputs for every completed baseline query, flag timeout queries as unavailable
parity evidence. Validate the prefix-search set identity from its filtering rule
and with a small corpus-independent trie fixture; this is not corpus accuracy.
This follow-up can support a faster lexical API, not sentence completion or an
unmeasured Android integration. No weights, vocabulary or reference labels change.

## Posting-index experiment, frozen after full-prefix rejection

Full-prefix search removes work on empty matches but populated completions remain
slow. TokenArray::getTokensForTermId performs two linear select0 scans and two
rank scans over postingsBits on every lookup. Test an owned load-time array of
posting offsets, retaining the original serialization, token order/costs and
fallback for builder-created objects. This is a performance-only source patch,
isolated under artifacts; the original pinned source/build remain intact.

Preserve the original last-term behavior (a missing trailing zero makes it empty)
in this experiment; do not silently turn this optimization into a coverage fix.
Validate every loaded posting list against an independent sequential bit walk,
and compare completed baseline outputs exactly. Re-run the frozen development
screens, then compare all prior sentence/word/error regression outputs and three
isolated per-key timing passes. Previously consumed holdouts are parity regression
only; a new quality claim or ranking change still requires fresh documents.
