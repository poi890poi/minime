# Explicit language modes

Approved design: Chinese+English, English-focused, Chinese+English+Taiwanese,
and Chinese+English+Japanese. The existing bottom language key pairs English
with the last mixed mode. The composing row uses its existing expand control to open mode tabs in the
expanded toolbar; an idle badge opens the same choices. Optional third-language modes follow the existing pack
enable settings; original settings are preserved for reversal. Height, key
positions, Space, Shift, punctuation and symbol page storage retain their roles.

Mode boundaries live in the shared core: only active dictionaries are queried;
switching preserves unfinished spelling, rejects obsolete callbacks and invalidates
old candidate gestures. An already queued explicit acceptance keeps event order.
Per-mode choice contexts prevent preferences learned in one mixed mode affecting
another. Manual user dictionary entries remain explicit, shared overrides.
No vocabulary imports, per-word promotions or ranking-weight changes are planned.

Baseline is c16f878 with immutable core classes and APK retained locally. Freeze
coverage inputs before reading candidate outputs. Report conversation and essay
genres separately, complete and partial input separately, dictionary probes as
retrieval only, and all input-generation/reading exclusions. Candidate visibility
means fully visible in the initial horizontal row or initial expanded viewport,
using phone font and layout metrics; literal spelling availability is separate.
Do not call top-N rank a measured first-row/page hit.

Acceptance: non-active packs cannot supply or delay results; same-input/mode
candidate ordering matches the corresponding enabled-pack control; Space matches
the visible highlight; midword switching does not insert text. Existing source,
core and pinned Rime checks precede Android builds. Measure warm typing query/delivery latency with declared configuration; retain
negative results. Cold loading, mode-switch duration and display frame latency
remain unmeasured; do not infer those from worker callback timing.
Phone sessions restore preferences and IME and verify display OFF.
