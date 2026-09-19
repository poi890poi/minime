# Reassess the extra-eight prefix retention addition

Withdraw c19a7d5's union of eight longest and eight highest-score prefix words.
Restore the earlier bounded list; retain source-backed prefix lookup, first-glyph
access, partial selection and the existing preview policy. Do not add a filter,
new weight or a phrase exception to rescue the rejected expansion.

The prior decision used an insufficient acceptance gate: greater any-rank access
with unchanged Space and whole targets. PRODUCT_REQUIREMENTS already asks quality
additions to demonstrate first-choice and first-eight benefit. The latest change
did not establish that benefit. The new slot audit makes the omitted cost visible.

Across 24,244 labeled reused composition episodes:

- Extra candidate occurrences: 86,049. Only 478 are useful prefixes of the intended
  text; 85,571 are off-target for that task. These are occurrences across queries,
  not distinct dictionary words or proof of linguistic nonsense.
- First-eight useful slots: 30,623 → 30,592 out of 193,940 displayed slots in each
  variant. Whole-target first choice stays 8,604; whole-target first-eight stays
  11,531. A useful phrase is visible in 14,941 episodes in both variants.
- Episodes with any useful visible choice increase 18,647 → 18,654, and mean longest
  useful text changes 1.81682 → 1.81707 glyphs. These small changes do not establish
  a meaningful efficiency benefit. No new threshold was tuned to these results.
- The previously reported 377 gained target-prefix episodes remain valid expanded
  retrieval evidence, but do not establish net typing benefit.

Do not hide the positive full-list result: task-compatible slots rise from 46,596
of 13,971,915 to 47,074 of 14,057,964 (about 0.3335% → 0.3349%). Episodes with a
useful multi-glyph choice anywhere rise 19,401 → 19,750. Expanded-list precision
therefore increases slightly; first-five and first-eight precision decrease.
Neither rate is the proportion of linguistically meaningful dictionary entries.
The rollback follows the unmet visible-choice acceptance gate, not a claim that
every precision metric worsened or that all added alternatives are invalid.

Restoring the previous limit knowingly relinquishes those expanded-list gains.
That is preferable to treating an unproven addition as the permanent baseline.
The underlying sentence-coverage and ranking gap remains; this rollback does not
claim Google-level quality or solve all irrelevant candidates.

Preserve the rejected method's corpus, reports, phone evidence and code history.
Its six recovered-case fixture is archived under docs/prefix-access/evidence;
the optional Android test requiring the withdrawn feature is removed from the
active suite. Existing stored-prefix selection integration remains active.

Phone comparison and final validation are recorded in README.md. The first
observation plan wrongly tried Space inside Google's expanded list, where that
control is absent. Its candidate snapshots remain valid; acceptance is unavailable.
The corrected plan collapses both lists before Space, with the same frozen inputs.
