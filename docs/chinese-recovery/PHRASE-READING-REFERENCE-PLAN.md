# Reference check for phrase-derived reading shares

September 24, 2026, before new Google observations. Evaluation-only phone work;
production runtime/assets stay unchanged. The variant passed its declared
405-query complete-reading screen. Broad results still include rank tradeoffs.

Freeze a diagnostic sample from the 405 paired outputs: all four newly unsupported
complete-reading Space choices, eight hash-selected newly supported choices,
eight changed choices supported on both sides, and eight unchanged controls.
Exclude queries already observed in the earlier 24-query reading-reference
session from the three sampled groups. Keep all four adverse cases even if
previously observed. Seed: `phrase-reading-reference-20260924`.

This is stratified failure diagnosis, not representative accuracy or a fresh
corpus holdout. Compare accepted MinIME and Google on the phone, then join the
isolated trial's core outputs. Require phone/core baseline agreement before
interpreting trial differences. Preserve Google spelling corrections and existing
learning; do not clear its data. Inspect each typed screenshot to verify the input.

In particular, an unsupported complete reading can be a valid spelling
completion: `gu` -> a glyph read `guo` is not a corrupt dictionary entry. The MOE
metric tests the complete-syllable interpretation only. Report completions
separately; do not ban them or change estimator weights to fit these probes.

Use the acknowledged exclusive phone window and shared lease. Restore original
APK, preferences, learning, prior IME and viewport; sleep and verify display OFF
on success or failure; explicitly release. No tablet, Play or hosted CI work.
