# Quality loop — new chat evidence, rejected duplicate-promotion trial

Baseline fd728d5. No production ranking change is retained from this loop.

## Broader evaluation

Added 4,608 frozen input conditions from 512 hash-selected MozTW chat-derived
excerpts: full spelling, initials and alternating full/initial syllables for each
excerpt and two short spans. Source provenance is in [SOURCES.md](SOURCES.md).
This source is independent of production text but its reading annotation shares
McBopomofo lineage. It is edited/shuffled chat, not intact conversations.

Of 8,882 source lines, 590 fail the declared Han-only/length condition and 5,916
have ambiguous or unavailable readings; 2,376 are eligible, from which 512 are
selected without consulting model output. Those exclusions are substantial and
prevent claiming representative population coverage. Only 263 of 1,536 target
references occur as exact entries in the reading source. Full-excerpt availability
must not be mistaken for the ability to complete text incrementally.

Baseline whole-target access among the first eight:

| Target unit | Input conditions | Whole target available |
|---|---:|---:|
| Complete chat excerpt | 1,536 | 70 |
| Short span from chat excerpt | 3,072 | 540 |

These counts establish a large whole-phrase limitation, not a nonsense rate.
The first eight are ordinal candidates, not a measured phone viewport. Human
selection effort and semantic naturalness remain unmeasured by this corpus.
No part of this file enters production vocabulary, model counts or tuning.

## Problem, cause and attempted change

The existing optional-source guard prevents duplicate entries from promoting a
low-ranked full-reading homophone. A synthetic regression reproduces that the
same guard does not cover a multi-glyph abbreviated reading: the sixth phrase
moves to third solely because an optional source duplicates it. This is evidence
of inconsistent ranking policy, not by itself evidence that changing it improves
real language quality.

The one-variable trial extends the guard to attested abbreviated base candidates
consuming the entire input. It changes no source, score, limit, default, focused
language behavior or generated text. Expected targets are used only after lookup.
The exact trial and failing synthetic regression are retained in
`rejected-duplicate-guard.patch` and `duplicate-before.log`.

## Measured result and decision

| Evaluation | Cases | First-eight whole-target gains | Losses | First-choice gain |
|---|---:|---:|---:|---:|
| New complete chat excerpts | 1,536 | 0 | 0 | 0 |
| New short chat spans | 3,072 | 1 | 1 | 0 |
| Existing encyclopedic tasks | 8,000 | 15 | 43 | 0 |
| Existing essay tasks | 11,220 | 0 | 1 | 0 |
| Existing authored conversation | 24 | 0 | 0 | 0 |
| Existing source retrieval | 5,000 | 0 | 0 | 0 |

Across the 24,244 labeled broad tasks, first-eight target coverage falls from
11,533 to 11,504 and target-compatible slots fall from 30,625 to 30,596. Slot
compatibility means whole intended text or a usable prefix of it; a valid
homophone may still be off-target. New chat first-eight totals stay equal, while
one target falls out of the first five. Neither genre shows a first-choice gain.

All candidate identities/spans and Space choices stay unchanged on 31,527 broad
episodes and 4,608 chat conditions. Among 6,144 English word/prefix probes with
optional packs enabled, English-mode output stays identical. Chinese-mode English
first-eight access gains four references with no loss. That does not compensate
for the Chinese regressions. Results and individual changes are retained in the
`chat`, `chinese` and `english` subdirectories.

**Reject.** The frozen first-eight/useful-slot genre gates fail. The experiment
also shows why a superficially cleaner rule is insufficient: the base frequency
order is not a dependable replacement for every existing dictionary preview.
The trial and its failing test have been removed from active production/tests;
do not silently treat the synthetic assertion as a release requirement.

Core contracts pass for the trial (887,638 assertions) and the restored baseline
(887,604 assertions); the difference is test/data-derived probe count, not model
accuracy. Both pinned desktop runs complete 13,014 rows and 11,272 native queries.
Desktop tails include outliers above 100 ms and concurrent benchmark contention;
these runs are correctness gates, not a phone/performance acceptance PASS.

## What remains

No general conversation-quality improvement is claimed. The release stays under
evaluation. Preserve the null-construction baseline and improve contextual
evidence rather than substituting another uncalibrated promotion rule. Current
Taiwanese/Japanese conversation corpora remain useful regression evidence with
their previously documented limitations; this loop does not recertify them.

Reproduce with `tools/make_release_chat_corpus.py` (frozen plan/source),
`ChineseRecoveryEvaluation`, `tools/report_release_ranking_trial.py`,
`tools/audit_candidate_usefulness.py` and `tools/report_mixed_english.py`.
Apply the archived patch only in an isolated experiment. Do not regenerate a
different corpus and retain this report's numbers. The source/evaluation ledger
pins inputs, notices, reports and raw changed cases. No hosted CI was used.
