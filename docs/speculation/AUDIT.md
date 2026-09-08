# Speculative sequences and shared partial matching

Baseline: 146bf1e / MinIME 0.6.2. User requires Taiwanese and Japanese partial
matching to follow Chinese/English behavior, without a separate weaker policy,
and requests a very large dictionary benchmark before trusting speculation.

First establish evidence without changing ranking or acceptance. Trace actual
multi-unit composition in the core, preserving every candidate text, score and
order. A one-unit path is lexical, not speculative. Deduplication retains the
winning candidate's provenance. Rime's public desktop API does not expose the same
provenance; report native whole-candidate results separately, not as known composed
paths. Dictionary absence is not proof of nonsense.

Freeze all eligible CC-CEDICT multi-character entries with their source readings,
plus held-out natural multi-token Chinese spans, before any ranking change.
Evaluate full input, initials, alternating complete/initial syllables and trailing
prefixes. Report seen/unseen production vocabulary separately. Membership uses the
complete CC-CEDICT/McBopomofo vocabulary union, independently of test labels.
Measure intended-entry top-1/top-8/any retrieval, speculative-only contribution,
dictionary attestation, failures and costs. Do not tune on these labels.

Then use the evidence to choose the narrowest candidate-policy change. Share match
types, partial-reading semantics and Space selection across languages; retain
source-specific reading boundaries as data. Existing pack enable switches isolate
optional data. Preserve known English, explicit raw recovery, Chinese phrase
coverage, asynchronous composition, privacy and fixed keyboard geometry.

Use core and pinned desktop Rime before Android. Keep implementation slices in
independent commits. Phone RFCR91GWXLX only; restore IME/preferences and sleep/verify
display after every session. Never add per-word production exceptions.

Matching implementation boundary: reuse the existing reading-prefix index and
source-unit trie for all optional packs. This is a feature/behavior correction:
exact-only becomes exact, trailing prefix, initials and mixed unit prefixes.
Japanese conversion units come from the pinned WanaKana mapping; POJ and Chinese
units come from source separators. Preserve normalized exact aliases and outputs.
No combinations are materialized. Search remains bounded; measure full-pack load
and lookup costs and source-derived complete/partial retrieval before packaging.
Metadata must survive scoring so optional choices retain privacy/learning rules.
Existing pack switches, source attribution, English recovery and binary model
compatibility remain intact. No source-derived benchmark labels enter ranking.
