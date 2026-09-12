# Controlled null comparison

Frozen before execution, September 12, 2026; source revision 7f0f179.
Type: experiment tooling. No production behavior, dictionary, ranking, learning,
UI or model changes. Apply the evidence-based engineering and change-impact audit.

Null retains lookup through the existing traversals, including their budgets,
scores and single-entry results. It prevents traversal from extending a nonempty
word path. It does not replace the reading matcher with a different lookup method.
Native null skips only MakeSentence; native on/off use the same compiler, source
and library configuration. Java sources are exported under artifacts, with exact
guard insertions at the two joining boundaries. Production is untouched.

Compare null, native only, Java only, and both (historical control). Java's two
paths also have independent switches for diagnosis. Sources, context, add-ons,
ranking, acceptance, beam budgets and queries are identical between variants.
Reference targets are read by evaluation only, never by candidate generation.

Reuse all 11,220 frozen Taiwan.md queries: full, initials, mixed and partial,
with add-ons on/off. These are consumed prose regression data, not fresh holdout.
Report intended rank1, recall8, bounded recall, MRR, actual Space output hits,
constructed exposure, no-Han output, and paired per-document deltas. Retain raw
candidate lists, including plausible alternate outputs; reference mismatch is
not semantic nonsense. Repeat timings with balanced order and separate native
from core flush. No Android/touch latency or human correction-time claim.

Use the existing GUM conversation/essay rows as English interference checks,
keeping genres and fresh/primed/English modes distinct. They cannot establish
Mandarin conversation quality. Actual corrections/time and semantic judgments
remain unmeasured until independently reviewed interaction evidence exists.

Legacy Google Zhuyin is the Chinese/English reference. Freeze a deterministic,
stratified subset of the same inputs before querying the authorized phone. Use
the existing legacy study harness, record app version, input mode and visible
candidate/Space behavior. Google is a quality/UX comparator, not training data
or automatic semantic labels. Coordinate the phone lease and restore/sleep it.

Acceptance: both recall and first-choice gains must reproduce against null,
without a material genre/input-condition regression or increased invalid output.
No new mechanism is approved by clause recall alone. Absent semantic/conversation
and effort evidence, findings remain insufficient to enable construction in a
future release. Null remains the default experimental configuration. Existing
release behavior is not changed by this research-only commit.
