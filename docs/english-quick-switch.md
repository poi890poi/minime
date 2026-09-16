# English / Chinese quick switching

Type: requested behavior change. Previously the saved quick-switch partner was
updated only for optional Taiwanese/Japanese modes. English already returned to
Chinese, but Chinese could then return to a stale optional language. The old
preference test explicitly expected that behavior.

Record the last explicitly selected non-Chinese mode, including English, using
the existing `last_focused_mode` preference. When leaving an English session for
Chinese, record English as the return partner too; this handles old persisted
English sessions without a separate migration or preference. Reading the target
remains side-effect free so rendering does not mutate preferences.

The service already uses the same preference policy for the button label and its
action. No separate display or dispatch rule is needed. Explicitly selecting 台
or 日 restores that mode's pair with Chinese. Disabled optional modes retain the
existing reversible fallback; re-enabling a formerly selected pack must not
supersede a later explicit English selection. Update settings guidance and the
product requirement to match this behavior.

Scope and risk: affects quick-switch destination after choosing English and when
returning from existing English sessions. No key geometry, dictionary, suggestion,
composition, candidate acceptance, punctuation policy or enabled-pack changes.
Existing preferences remain readable. Restart persistence and unfinished input
are the main adjacent boundaries to verify. No language-model benchmark is needed
for this preference-only change.

Verification plan: native preference tests for repeated English/Chinese pairs,
restart persistence, legacy saved English state, optional-mode pairs and reversible
disable. Live IME tests click the actual bottom key after each optional mode and
explicit English selection, checking the label, repeated return trip, composing
span, spelling and stable geometry. Reuse existing one-tap Pinyin/Zhuyin switching
and optional-mode interaction coverage. Build/lint locally; no GitHub CI. Phone
operations require acknowledged ownership, mutex and verified cleanup.

Verification result: local debug/test builds and lint passed (0 errors). All seven
selected native tests passed in phone session
`64d18e16-2857-40c1-a489-39e2c0bfeb9a`: four preference cases, the new live
English round-trip case, existing one-tap switching, and focused-mode geometry /
composition coverage. Prior IME and settings/learning were restored with readback;
actual display OFF verified and reservation explicitly released. The initial
approval-review rejection concerned an unrecognized nested lease; inspecting the
runner confirmed the lock precedes ADB and covers cleanup, and the retry was
approved without changing or bypassing the wrapper. The 0.8.8/code-38 release
archive is unchanged and does not contain this behavior update.
