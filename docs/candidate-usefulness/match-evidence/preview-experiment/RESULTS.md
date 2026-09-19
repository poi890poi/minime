# Development loop: prefer covered readings for the Chinese preview

Decision: **withdraw**. The rule is not in production. Baseline `9875177` remains.

Problem hypothesis: a forward completion may occupy the first Chinese preview
while a later candidate covers all source reading units. Try exchanging these two
Chinese positions while preserving English positions and the raw Space default.
The plan was frozen in `../RANKING-PLAN.md`; no scores, sources or quotas changed.

The synthetic test fails on baseline and passes on the trial, proving the code
changes the intended case. However, that does not establish practical benefit:

| Evaluation | Result |
|---|---:|
| Chinese episodes | 31,527 |
| Chinese candidate-order changes | 0 |
| Intended first/eight-choice gains or losses, 24,244 labeled tasks | 0 |
| ASCII English candidate-position changes within those Chinese episodes | 0 |
| English word/prefix probes across both modes | 6,144 |
| Chinese-mode list changes among English probes | 5 |
| English target first-five/eight gains or losses | 0 |
| Space changes in either corpus | 0 |

Results remain unchanged separately for 8,000 encyclopedic tasks, 11,220 essay
tasks, 24 authored conversation tasks and 5,000 source-retrieval tasks. These
datasets were previously evaluated; none is a fresh holdout. An unchanged target
metric cannot tell us whether the five changed Chinese lists are better.

Cause of the negative result: the proposed preview branch was not responsible
for any of the measured Chinese candidate-order gaps. Distinguishing reading
evidence is still correct, but using it here adds policy without demonstrated
benefit. A passing synthetic test does not justify keeping the rule.

The trial passed 370,460 core contract assertions. Host key processing p50/p95/max
was 0.03 / 0.54 / 107.86 ms; this is not a touch-latency measurement or a speedup.
No desktop-native rerun, Android build or phone run was needed after rejection;
the retained production code already passed the preceding loop's desktop gate.
The final core suite is rerun after restoring the accepted production source.

Next investigation: context and source-frequency ranking among candidates already
retrieved, with natural conversation evidence separate from essay results. Freeze
the next data split before comparing models. Do not increase candidate budgets or
promote dictionary packs on the strength of these negative experiments.
