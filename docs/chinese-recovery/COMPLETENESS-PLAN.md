# Test completeness ordering independently of pronunciation frequency

Frozen September 25, 2026, before runtime edits. Baseline: `21557f4`, including
the admitted phrase-derived pronunciation counts. Data and model weights stay
fixed. This is an experiment, not an admitted release change.

Hypothesis: when explicit spelling evidence competes with a frequency-driven
completion, the built-in Pinyin decoder loses that evidence at two sorting
boundaries. Test one rule: preserve whole-input before partial-input ordering,
then explicit learned votes, then fully typed before incomplete paths, then
existing score/ties. Dictionary deduplication must preserve the full path before
composition reorders it. Do not delete completions or change acceptance spans.
Apply only to Pinyin Chinese candidates; preserve Zhuyin and focused/English
mode policies. Do not add syllable-length cutoffs or per-input exceptions.

Rime transport reports consumption and construction, not spelling completeness.
Unknown native paths must not be labeled incomplete or claimed to be exact.
Native order remains authoritative among its own candidates; the trial may
change the built-in fallback ordering. Measure that merged path separately.

First reproduce with generated source entries whose exact path has lower
frequency than a completion. Include duplicate identities, initials without a
full path, learned completion choices, partial acceptance, and source order
ties. These are mechanism tests, not language accuracy.

Evaluate all frozen complete-reading MOE probes (405), Chinese recovery episodes
(31,527), edited conversation probes (4,608), mixed English probes (6,144), and
the pinned desktop native evaluator. Existing exposed data is development
evidence, not fresh holdout evidence. Separate genres, full/initial/partial
conditions, actual Space, and first 1/5/8 target-compatible and whole-target
coverage. Complete-reading support alone cannot admit the rule: it is aligned
with the hypothesis and does not establish the user's intended word.

Reject adoption if exact source identities or suffix acceptance are lost,
English-mode outputs change, or first-eight whole/compatible coverage declines
in conversations or essays. Inspect every changed Space default, with one-key
syllabic readings and abbreviated phrases explicitly reported. No weights or
corpus labels may be adjusted to pass. If rejected, revert runtime edits and
retain the aggregate negative result. If supported, assess query cost with
paired runs, run the complete core suite, then obtain a fresh phone reservation
for production-payload integration and Google comparison before admission.
