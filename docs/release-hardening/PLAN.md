# Release hardening — 2026-09-22

Baseline fd728d5; feature freeze. No GitHub CI, source refresh in production,
per-word exceptions, new automatic constructions, or unmeasured release approval.

First loop: evaluation expansion and a candidate-ranking bug investigation.
The existing source-duplication guard protects complete Chinese readings and
single glyphs, but not multi-glyph abbreviated readings. A static optional pack
may therefore promote an already attested low-ranked base homophone solely because
the same text occurs in another source. Reproduce with synthetic homophones before
editing. If proven, extend that guard only to whole-input, attested abbreviated
base matches. Preserve forward-completion treatment, novel entries, focused
Taiwanese/Japanese modes, explicit learning, Space, privacy and source inventories.

Runtime may use typed input, accepted context, existing dictionaries and explicit
preferences. Labels, complaint examples, expected ranks and benchmark text never
enter production. No weight fitting. Compare one guard change with baseline.

Freeze new evaluation before any candidate query: use the pinned MozTW donated
g0v chat file as a single evaluation-only source document, never training data.
Its editors removed context and shuffled lines; no conversation/session split is
possible. Therefore this is chat-derived excerpt evaluation, not a fresh
conversation holdout. Select 512 eligible lines by salted hash; derive full,
initial and alternating spelling using only unambiguous McBopomofo readings.
Include complete excerpts and two hash-selected 2–6-character spans per excerpt.
Report rejected readings and exact source overlap. The reading oracle shares
dictionary lineage and biases toward readable known characters. No frequency
or decoder output controls selection. Keep existing essay/encyclopedic data
separate; never pool it into a conversation-accuracy number.

Acceptance for the duplicate-ranking fix: synthetic invariant restored; no
candidate identity/span loss, no Space change, no English-mode change; preserve
focused-language contracts. Require nondecreasing intended first-choice and
first-eight access plus useful-slot counts within each labeled genre on new
excerpts and the reused broad corpus. Record every gain/loss, not only aggregates.
Reject the candidate if these gates fail; retain the test evidence. Record host
lookup time separately; no phone latency claim from core timings.

After core and pinned desktop checks, build and verify one signed candidate.
Phone window requires the acknowledged SHINE handoff and the shared mutex.
Restore prior IME/preferences and verify display OFF even after failures.
Validate editor/lifecycle/visible behavior; synthetic events do not certify human
touch accuracy. Human trials, 10,000-key/mode latency tails, Android 16/16 KB runtime
and rights clearance must remain NOT MEASURED or BLOCKED until actual evidence.

Release evidence must identify exact commit, binary hashes, version and signing
certificate. A successful local build does not clear the unresolved UD rights item
or authorize publication. Update the current gate report without rewriting old
release reports as though they tested this build.
