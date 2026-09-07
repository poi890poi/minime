# English as a primary layout — 2026-09-07

The user clarified that English and Pinyin are both primary layouts. This pass
corrects the explicit English mode's missing completions and reviews its commit
behavior. Keep single-tap switching, raw recovery, uppercase slides, Caps Lock,
ASCII punctuation and the existing mixed Chinese mode.

Cause: MiniMeService passes `policy.literal || english` into CompositionEngine.
The literal-field flag disables all dictionary candidates, including English.
The shared English completer also title-cases every uppercase prefix, losing an
all-caps prefix. Separate language choice from the editor's restriction, keep
Chinese candidates out of English mode, and preserve prefix case.

Space must retain exact typed spelling unless the user explicitly selects a
suggestion. Suggestions use the existing licensed English dictionary. No new
automatic correction or next-word language model is introduced. URL, password,
numeric and NO_SUGGESTIONS fields retain their restrictions; private fields must
not record choices. Observe the installed reference's English Enter and Space.

Verify explicit English candidates and rejection of Chinese alternatives, lower/
title/all-caps completions, no accidental identifier casing changes, punctuation,
real spaces, mode/field changes, direct fields and private learning. Run the core
regressions, build/lint, phone suite and artifact checks; restore app preferences,
the previous keyboard and display sleep after every phone run.

Observed reference: pronun exposes pronunciation/pronunciations, hello + Space
produces a real space, hello + Enter produces a newline immediately, and teh +
Space becomes tech in this configuration. The last behavior motivates an explicit
user preference before any automatic correction; this pass keeps exact spelling.
English Enter now acts in one press, while Chinese Enter remains two-step.

The all-caps completion regression failed before the fix. New mode tests separate
English from Chinese and restricted fields. Forced literal fields now also use
ASCII slide symbols even when the saved language mode is Chinese. Remaining
English gaps include typo correction, next-word prediction and automatic sentence
capitalization; the current dictionary is a prefix-completion baseline.

Reference data: [four English sequences](evidence/english-primary-observations.json),
replayed from docs/legacy-study/english-primary-plan.json. The successful focused
phone test covers lower/all-caps selection, real spaces, Enter, contractions,
slides, Chinese return, URL suppression and English mode across field changes.
One earlier immediate editor assertion saw empty text; the capture run passed,
and the test now awaits the editor commit within a bounded interval. This was not
treated as evidence for changing the app's InputConnection adapter.

Final build, lint, suite and artifact identity are in [VERIFICATION.md](VERIFICATION.md).
