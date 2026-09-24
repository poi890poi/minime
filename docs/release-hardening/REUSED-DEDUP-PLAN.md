# Reuse trimming scratch storage without changing candidate selection

Frozen September 25, 2026, after the provider profile and before implementation.
The unchanged lookup profile has 619 of 1,258 lookup CPU samples inside
ReadingUnitIndex.trim. Its duplicate-removal lambda and trim body account for
about 6.80 billion of 12.39 billion lookup-associated sampled allocation-weight
bytes. These are profiler estimates, not retained heap or exact elapsed shares.

Test one change: retain a temporary String-to-Boolean map across trim calls in
one match invocation. Reset its existing values before deduplication, mark each
encountered text, remove unused entries, and remove quota-pruned entries. Existing
nodes for retained candidates can then be reused instead of constructing a new
HashSet and its nodes on every terminal state. Allocate lazily, and keep the map
local to the invocation; no dictionary/query cache or cross-thread state.

Preserve sort order, stable ties, every search budget, omitted-reading penalty,
length-diversity rule, metadata and binary representation. The scratch map must
contain exactly the retained distinct texts after each trim, including when
successive trims use different offset lists or empty inputs. Do not change the
visited-state map, priority queue, source data, quotas or font implementation.

Before editing, archive the baseline compiled core and source hashes. Test the
selection against an independent mathematical reference: highest-score stable
representative per text, globally sorted top limit plus the first six per output
length when diversity is enabled. Include repeated calls, interleaved lists,
score/tie changes, quota evictions, empty lists and String hash collisions.
The exact retained Candidate identities must match, not merely their text.

Require complete fingerprint equivalence on every query in the frozen provider
inventory and across all measured passes. Run fresh unprofiled A-B-B-A JVMs with
the same inventory, warmup, heap and three measured passes each. Advance only if
corpus p95/p99 improve in both pairs and no source/genre p95 or p99 repeatedly
regresses by both more than 10% and 100 microseconds. Preserve all strata and
outliers. Run one identically configured JFR trial to examine lookup allocation;
do not treat sampled weights as a retained-memory measurement. Record scratch
membership bounds through contracts; assess Android memory before admission.

This is a local screening rule, not production admission. Run full shared-core
and pinned desktop Rime checks before any Android build. Require unchanged assets
and candidate/default outputs. Any eventual phone trial needs a new explicit
reservation, unhooked comparisons in all four modes, raw/Space tail protection,
candidate/frame accounting and cleanup. A local speedup alone cannot be landed.
Keep the trial isolated or revert it if any frozen gate fails. No hosted CI.
