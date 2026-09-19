# Candidate usefulness audit — 2026-09-19

Type: evaluation and acceptance-policy correction. Baseline runtime ecab07b;
compare retention c19a7d5 with its parent behavior 96db0d6 through saved complete
candidate inventories. Do not choose another ranking rule from these labels.

The previous acceptance gate emphasized reachability and let first-eight losses
through. Reassess it using both recall and precision for a specified intended text.
A dictionary-backed alternative is not necessarily useful in that task; an
off-target homophone is not automatically nonsense.

Report separate measures with denominators:

- Whole-target top-one / first-eight / any-rank success per labeled episode.
- Target-compatible precision: whole target or a proper target prefix with a
  partial span, divided by displayed candidate slots. Exclude explicit raw recovery
  but count Latin/other off-target suggestions. This is task-conditioned precision,
  not a linguistic judgment of every alternative.
- First useful rank, longest useful visible prefix in glyphs, and how many
  off-target suggestions precede it. No useful candidate is reported as unavailable,
  never as zero search effort.
- Whole targets and multi-glyph prefixes separately from one-glyph recovery:
  a list of correct first glyphs must not substitute for phrase usefulness.
- Previously reachable targets displaced below the first eight; added slots that
  do/don't help the specified target; distinguish unknown linguistic quality from
  provably invalid source spans or fabricated sequences.

Reuse all 31,527 composition episodes, reporting the 24,244 labeled episodes by
genre/input condition; leave unlabeled mechanical rows out of precision. Reuse
the 42,000 suffix-condition rows as retrieval stress evidence, not conversations.
The saved composition inventories enable per-slot precision and clutter analysis;
the suffix reports only store previews, so do not infer expanded-list precision
from those files.

Phone: use all 18 existing frozen reference cases plus six hash-selected remaining
authored-conversation rows (seed candidate-usefulness-20260919). Freeze 24 cases
before observations: 12 conversation, 12 essay; same input for both keyboards.
Capture the collapsed row, first expanded viewport and actual Space acceptance.
Count missing observations explicitly. Preserve raw screenshots/UI transcripts.
Google learning is not reset; MinIME starts fresh per case. This is a paired
experience audit, not a controlled accuracy or speed superiority claim.

Runtime only receives current input, existing source scores and allowed learning.
Expected text, labels and Google outputs never enter production dictionaries or
ranking. No phrase exceptions, weights or production entries may be selected from
this audit. If coverage gains lack usefulness evidence, mark the recent change
unproven or revise it; do not call additional dictionary matches better prediction.

After the extra-eight decision, run one isolated ablation of the earlier forced
longest-prefix preview: remove only that display promotion, retain retrieval and
all existing scores. Compare against 96db0d6 on the reused composition corpus.
Measure phrase versus glyph usefulness and useful text length as well as precision;
more correct one-glyph choices cannot stand in for efficient phrase selection.
This is a diagnostic ablation, not a fresh validation set or permission to fit
new preview weights. Keep production unchanged unless joint evidence supports it.

Use local core/desktop checks before any new build. Existing debug APK can be
observed without changing runtime. Phone requires explicit SHINE acknowledgement
and shared mutex, with prior IME/preferences restored and display OFF verified.

English interference follow-up: the existing mixed-mode interleave always puts
Latin first when raw input is the Space default. Test one presentation change:
in Chinese mode, with neither an English word nor preceding Latin context, put
the Chinese member first in each existing pair. Keep candidate membership,
within-language order, Space, English mode, known English words, apostrophe
restoration and focused modes unchanged. No learned weights or word exceptions.
Measure the same Chinese tasks and independently sourced English words/prefixes
from the existing conversation-ranking inputs (reused development/evaluation,
not a fresh holdout), at first 1/5/8 and full-list target rank. A Chinese gain
alone cannot justify an English completion loss; record the tradeoff before
deciding whether to land. This is a mode-priority behavior experiment, not a
claim that all English prefixes are wrong. It adds no dictionary or lookup work
beyond testing existing English/context evidence. No phone speed claim.
