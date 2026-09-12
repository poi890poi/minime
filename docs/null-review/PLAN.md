# Null review APK

September 12, 2026. User authorized a usable no-construction review build after
the controlled null benchmark. Keep independently reviewable commits.

1. Bug fix: engine can preserve a later whole-input preferred candidate behind
   prefix recovery, so the visible strip omits the Space choice. Promote the
   already selected nonliteral default to position 1, preserving raw slot 0 and
   actual acceptance. Keep remaining order stable. Pending retained snapshots
   must not falsely highlight a stale default. Verify core then visible phone UI.
2. Behavior change: remove Java joining and disable native MakeSentence in the
   production build. Stored entries, abbreviations, explicit incremental selection,
   English and other focused languages remain available. Native construction
   stays available only in the existing isolated experiment tooling. Update tests
   that previously required automatic construction to exercise stored/explicit
   entry instead; do not change corpus reference labels or add production words.
3. Lookup improvement: reuse single-entry reading-unit lookup; benchmark it
   independently against the controlled null. Preserve full matches before shorter
   consumed-prefix recovery when Chinese is the chosen interpretation. Do not
   drop alternatives, blacklist words, change source weights or override English.
4. Core and pinned desktop gates precede Android. Compare the frozen reference
   cases with Google Zhuyin on RFCR91GWXLX under explicit reservation/mutex; restore
   preferences/prior IME and sleep/verify OFF. Produce a signed debug review APK
   and ZIP, clearly separate from a Play production release.

Risks: losing assembled clauses is intentional under the null contract; stored
lookup, initials, partial phonetics and explicit selection must remain usable.
UI row stability and asynchronous composition ownership must remain intact.
Existing corpora are consumed regression evidence; no new semantic precision or
fresh Mandarin conversation claim. Document incomplete gates and negative results.
