# Dedicated language suggestion priority

Baseline: `7fb0ae1` (0.7.2). This is an intentional ranking behavior change.
Taiwanese and Japanese modes currently scope enabled packs without giving their
matches priority over the Chinese base. Incomplete supplemental matches share the
generic third/ninth-slot preview rule.

Give the selected mode's source-tagged candidates a stable priority tier: complete
matches, then incomplete matches, before Chinese alternatives. Keep explicit user
entries/learned phrases and apostrophe restoration ahead. Keep each source's own
ordering and the existing unfamiliar/duplicate Han glyph safeguards. Chinese and
English modes, raw slot zero, literal/private fields, stored preferences, source
data, match algorithms and retrieval bounds stay unchanged. Space continues to
accept the highlight; existing English/raw protection remains. Selecting a mode
is the opt-in boundary; switching to Chinese restores its original policy.

Candidate pack identity must survive prefix/initial matching and paired Han/POJ
presentation. It is runtime metadata, not a new binary model or persisted setting.
Only mode, source pack, match completeness and existing runtime user preference
may rank results. Evaluation targets must never enter the ranking policy.

Freeze evaluation before edits: reuse the exact 3,922 rows and four modes from
`docs/input-modes/coverage-inputs.tsv` at this baseline. This is exposed regression
data, with English conversations and essays, Chinese essays, and source-derived
Japanese/POJ retrieval separated by input condition. No new accuracy/holdout claim.
Save complete per-query outputs before/after; score top-three/top-eight optional
positions (not physical first-row/page coverage), availability and highlight.
Verify Chinese/English output parity, whole-input Space acceptance, partial paths,
language/pack isolation, explicit overrides, raw/technical recovery and Han forms.
Keep collateral Chinese/English coverage losses in dedicated modes visible.

Use a second deterministic source-derived query set sampled without reference to
outputs if practical, for retrieval robustness rather than natural-language
accuracy. Measure ranking/application cost separately from dictionary/native
lookup; no claim about rendering or phone speed from desktop timings. Run shared
core and pinned desktop Rime before Android build. No new sources or corpus
training changes belong in this change. Missing retrieval and uncommon source
spelling are separate limitations, not reasons to handpick entries or weights.
