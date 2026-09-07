# Comparative review contract — 2026-09-07

Scope: extensive review/testing of installed Google Zhuyin 2.4.5 and MinIME 0.2.1.
Pinyin and English have equal priority. This is an evaluation/tooling change;
production typing behavior and language data remain frozen during the review.

Use fresh paired sequences for spelling/completion, initials and mixed Pinyin,
Space/Enter, punctuation, capitalization, gestures, candidate acceptance, editing,
language switches and editor restrictions. Replay semantic keys through visible
UI controls in the same synthetic native editor on the authorized SM-G781B.
Record editor text, composing span, selection, visible labels, action result,
per-case availability/errors and screenshots for selected cases. Compare only
valid matched cases. A runner pass means observations completed, not parity.

Preserve earlier reference evidence and distinguish repeated controls from fresh
probes. Candidate visibility is not exhaustive dictionary recall; target matching
and agreement with Google are separate metrics. Google learned history/settings
cannot be reset or inspected without changing user data, so rankings are a sample
of the installed configuration. MinIME uses an isolated preference fixture restored
after each batch. No expected output or observed Google candidate enters runtime
ranking. UI automation timing is not end-to-end keyboard latency.

Only named synthetic editor content is read. Scripts back up MinIME preferences,
bound runs, restore the previous IME and sleep the display even after failures.
Do not test the tablet, clear Google's data, install proprietary assets into MinIME,
or send synthetic text to contacts/services. Keep tests out of release artifacts.

Deliver raw paired evidence, reproducible plans, aggregate coverage, a prioritized
gap table with examples, intentional compatibility differences, and untested areas.
Use existing source and release checks for platform/privacy/configuration gaps;
do not infer tested behavior from static source alone.

Outcome: retain the production baseline. Completed 90 paired scenarios and 12
targeted paired rechecks; 87 scenarios have completed observations for both
providers. Preserve three incomplete comparisons and rejected setup trials.
The report separates acceptance policy, model recall/ranking, presentation,
editor actions and lifecycle boundaries. Fresh 22-test Android suite and isolated
benchmark passed. APK identity is unchanged; final IME and display state verified.
Only observation tooling and documentation changed. See GOOGLE_MINIME_GAP_REVIEW.md.
