# Development loop: context backoff

Baseline: `5b704a4`. Behavior/model-estimator experiment, not new training data.
The current character model chooses a two-character context when any count exists
for it, otherwise a one-character context. Its smoothing prior is always unigram.
Thus a sparse long context can discard positive evidence in the shorter context.

Hypothesis: interpolate each available context level with the previous level,
retaining the existing smoothing mass 20, unigram prior, score clamp and boundary
weight. Change only this estimator. No new words, source refresh, exceptions,
search-budget changes, or per-example score tuning. The source counts stay frozen.

Runtime may use only accepted context, typed reading and existing source counts.
Expected words generate input conditions and score results after decoding; they
never enter scoring. Tests assume the preceding source characters have already
been accepted correctly. This is a conditional ranking test, not free-running
conversation accuracy or a test of automatic sentence construction.

Freeze before running: existing 800 encyclopedic boundary regression cases under
full, initial and mixed readings; all distinct authored conversation scenarios
already present in pinyin-fresh-holdout.tsv; all dictionary-covered substrings of
up to six Han characters after a nonempty prefix in 64 hash-selected Taiwan.md
documents. Use every eligible span, not selected words. Reading aliases come from
the existing source dictionary. This biases toward known vocabulary and is not an
independent pronunciation oracle. Conversation scenarios are authored and small.
All sources were previously evaluated; there is no fresh holdout claim.

Land only if intended first/eight-choice coverage improves without genre-level
regression, no candidate inventory/consumed-span losses occur, empty-context
ranking and English are unchanged, and standard core/native gates pass. Inspect
individual gains/losses and full/initial/mixed conditions. A positive synthetic
test alone is insufficient. Reject the estimator if broad quality regresses;
do not tune its constants after seeing the labels. Report host lookup timing;
require repeated paired runs before asserting a speedup or slowdown. No new
retained structures or binary-format change. Phone testing is reserved for a
subsequent integration loop if this survives.
