# Taskmaster source replacement pilot

September 24, 2026. Audit/evaluation only; no production data approval. Target:
the English EWT-derived context counts whose underlying-text distribution rights
remain unresolved. Chinese source quality/rights are a separate decision.

Pin google-research-datasets/Taskmaster revision
d92cb6af3005f1dc09c39e75e7daf4a04905e00b, TM-1-2019 only. Its file-specific notice
licenses this authored dataset under CC BY 4.0, and describes both crowdsourced
self-dialogues and two-person Wizard-of-Oz dialogues. Preserve the notice and
exact hashes. Other Taskmaster versions are outside this pilot.

Acquire self-dialogs.json and its official train/dev/test ID files. Audit every
record structurally, duplicate conversation IDs, utterance coverage, punctuation,
domain balance and split overlap. Never handpick phrases or assign word scores
from complaint examples. If the official splits overlap, fail rather than repair
silently. Only training IDs may contribute to a future proposed count table.
Keep dev/test as separate post-training evaluations; record inspection/exposure.

Freeze extraction before querying model outputs: use the existing English
tokenization and sentence punctuation boundaries, within each utterance only.
No context crosses turns or speakers. Lowercase lexical counts; preserve internal
apostrophes. Keep the existing minimum two observations for nonempty contexts.
Evaluate old versus candidate counts on existing independent English regression
and essay data plus disjoint dialogue splits. Report genre/domain separately,
whole-word/next-word recall and off-target slots, apostrophe coverage, asset size
and latency. Do not treat task-specific conversations as general English usage.

Limitations: self-dialogues are authored by one worker playing both roles,
prompted tasks rather than spontaneous conversations; six service domains can
strongly bias next-word suggestions. Two-person data has different collection and
editing, and is not admitted by this initial plan. No claim of Taiwan Mandarin
coverage. Existing source gate stays unresolved until exact replacement assets,
metadata, source ledger and independent quality evidence have been reviewed.

## Initial structural audit and leakage rule

The full file has 7,708 conversations: 6,168 train, 770 development and 770 test.
IDs are disjoint and exhaustive, with 169,469 total nonempty utterances. However,
normalized complete-dialogue text has two duplicate training records and one
shared train/test dialogue. Official IDs alone do not establish independence.
No model has been trained or evaluated on this pilot.

Before any training: deduplicate complete normalized (speaker, text) dialogue
sequences within training, keeping the lexicographically first source ID, then
exclude every training dialogue whose full signature occurs in either evaluation
split. Apply this to the complete source, never to hand-selected IDs or model
errors. Preserve all exclusions and reasons in the manifest. Do not claim this
removes shorter shared phrases or instruction/domain overlap. This rule is
declared after structural inventory but before extraction or model outputs.

The current [manifest](manifest.json) records raw split counts, hashes and the
known overlap; it is not a decontaminated training export. Reproduce with audit.py
using original files cached under artifacts/source-audit/taskmaster.

Source: https://github.com/google-research-datasets/Taskmaster/tree/d92cb6af3005f1dc09c39e75e7daf4a04905e00b/TM-1-2019
