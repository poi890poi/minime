# Prefix access follow-up — 2026-09-19

Baseline: 96db0d6. Type: source-backed retrieval/ranking experiment and test
expansion. The previous trie collects reached words, then keeps eight by consumed
length before frequency. Hypothesis: shorter common words can be lost despite
matching the input, while extra consumed initials do not establish stronger intent.

Freeze 6,000 hash-selected source phrase families outside the earlier 12,000
partial-form families. Use seven complete/initial/mixed/separated spellings,
followed by deterministic source-derived suffix units. Split by phrase identity
into development and validation before evaluating. All vocabulary is seen-source;
only these suffix conditions are new. This is not a natural-language holdout.
Report source-frequency bands and input conditions, not a pooled accuracy claim.

Compare one variable at a time: (1) preserve the existing eight prefixes and add
the eight highest source-score prefixes, bounding the union at sixteen; (2) if
retention helps, compare source-score versus consumed-length preview selection.
Scores remain the existing source scores and omission penalties. No word-specific
rules, new data weights, concatenated sentences or expected-target runtime inputs.

Accept retention only with no lost prefix targets or whole targets. For preview
ordering, inspect first-eight changes, glyph access and whole-match access; do not
trade away whole matches or change Space. Run the earlier 31,527 composition
episodes as reused regression evidence and preserve negative outcomes. Verify
independent source/span validity and explicit selection/suffix editing. Measure
lookup latency separately, then core and pinned desktop gates before Android.

Conversation-source discovery remains separate: provenance, rights, transcript
genre and independently annotated readings must be established before evaluation.
No academic conversation material is approved for production vocabulary here.
