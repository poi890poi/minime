# Completeness ordering: broad grouping rejected

September 25, 2026. Baseline `21557f4` includes the admitted conditional
pronunciation counts. The [frozen plan](COMPLETENESS-PLAN.md) tests spelling
completeness separately from source frequency. **The first trial is rejected.**
The broad grouping is not in production. The separately tested
[single-preference follow-up](COMPLETENESS-SINGLE-RESULTS.md) is admitted later.

The cause is reproducible with six generated dictionaries: a high-frequency
completion outranks a low-frequency exact spelling, and composition sorts away
a dictionary-only fix. The trial prioritizes all complete paths at both owners.
Generated tests preserve identities, initial-only retrieval and learned choices;
the full core suite passes 1,770,117 assertions. Those are mechanical contracts,
not language accuracy.

| Frozen evidence | Baseline | Broad grouping |
|---|---:|---:|
| Reference-supported Space, 405 complete-syllable probes | 313 | 337 |
| Supported complete glyph slots among the first eight, same probes | 1,535 | 1,828 |
| Whole target in first eight, 4,608 conversation episodes | 610 | 611 |
| Target-compatible slots in first eight, same conversations | 4,852 | 4,762 |
| Whole target in first eight, 11,220 essay episodes | 1,249 | 1,238 |
| Target-compatible slots in first eight, same essays | 12,268 | 12,258 |

First-eight slots are candidate occurrences across episodes, not unique words
or successful predictions. Compatibility means the whole intended text or an
explicitly selectable prefix of it; off-target alternatives are not automatically
nonsense. The reference supports pronunciation, not intended homophone choice.
All corpora are previously exposed development evidence, not fresh holdouts.

No candidate identity or acceptance span is lost across the 31,527 broad and
4,608 conversation episodes. All 6,257 currently available reference glyph/
reading pairs remain available. English-mode outputs in the 6,144 mixed-mode
probes are identical. Yet 96 useful partial-phrase slots disappear from the
conversation first pages, against five partial-phrase gains and one net whole
gain. Fully spelled glyph prefixes displace useful abbreviated phrase prefixes.
Completeness is not sufficient reason to promote every homophone.

The full core suite cannot catch this quality regression by itself. No Android
build or native/device admission was warranted after the genre gate failed.
The experimental patch is preserved in `completeness/all-complete.patch`,
relative to the baseline; it is not applied to production. Aggregate reports in
`completeness/all/` retain denominators, conditions and input/output hashes. Raw
corpus text and per-query outputs remain local. The generated mechanism probe
is `CompletenessOrderProbe` (`frequency` for baseline, `complete` for the trial).

The [follow-up plan](COMPLETENESS-FOLLOWUP-PLAN.md) tests one preferred full
alternative without regrouping the remaining candidates. Its tradeoffs and
Google check must be assessed independently before any admission.
