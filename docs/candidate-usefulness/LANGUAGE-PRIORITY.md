# Land a language-priority correction

Type: behavior correction in shared candidate presentation. The defect is using
raw-preserving Space acceptance as a proxy for English intent. Recorded phone
evidence shows English prefix completions leading all 16 sampled Chinese/English
collisions while Google leads with Chinese in all 16.

Contract, before implementation: in Chinese mode, without preceding Latin context,
prefer an existing whole-input Chinese alternative over an English extension as
the first alternative. Raw remains available and its Space default is unchanged;
explicit contractions, selected defaults, custom mappings, literal/secure fields,
technical tokens, English mode and other focused modes retain their semantics.
Capitalized Latin input and Latin typed on the Zhuyin board also retain their
existing English priority; these are explicit alphabet/casing evidence.
Use only selected mode, preceding accepted context and existing candidate metadata.
No target text, reference outputs, word exceptions, fitted scores or new corpus.

Move only the first eligible Chinese alternative to the lead if English currently
leads. Do not reverse every pair, change retrieval membership, or make a prefix
selection consume the whole buffer. Other alternatives retain relative order.
This removes the causal English-intent assumption without replacing it with
another blanket cross-language reordering rule.

Acceptance: paired core outputs must preserve candidate membership and Space,
improve Chinese leading alternatives and preserve intended English first-five and
first-eight coverage on the existing positive probes. English-focused outputs
must remain identical. Exercise preceding English context, mode switching, literal
and private fields, explicit mappings and partial-span fallback. Existing reused
conversation/essay results remain separate; do not claim fresh holdout accuracy.
Core and pinned desktop gates precede Android builds. A linear list scan adds no
model, dictionary memory or network work; time it separately from query/IO and
do not infer touch latency from desktop runs.

Known limit: this corrects priority, not phrase-source coverage or all irrelevant
English membership. It can move the first English completion down by one position;
the selected Chinese mode justifies that local priority. Report exact measured
displacement rather than rejecting any tradeoff or calling it universally free.

## Results and decision

Land the correction. This differs from the retained blanket pair-reversal
experiment: promote just one eligible Chinese alternative and keep the remaining
ordering. That closes the demonstrated priority gap without its first-page losses.

| Reused labeled Chinese tasks (24,244) | Before | After |
|---|---:|---:|
| English leads alternatives | 764 | 4 |
| Whole intended Chinese text is first alternative | 8,604 | 8,779 |
| Whole intended Chinese text in first eight | 11,531 | 11,531 |
| Useful first-eight slots / all slots | 30,623 / 193,940 | 30,623 / 193,940 |

This is a priority improvement, not an increase in first-eight recall or slot
precision. The first-choice gain is 169 encyclopedic and six essay tasks; the
24 authored conversation and 5,000 source-retrieval tasks are unchanged. All
genre/condition results remain in [the core report](language-priority/core-summary.json).
Three of the four remaining English leaders are explicit apostrophe restoration;
the other has no eligible whole-input Chinese alternative.

Across all 31,527 episodes, including mechanical probes, candidate text/consumption
multisets, first-five membership, first-eight membership and Space output are
identical. 872 full-list orders change (760 labeled tasks). See
[the per-input invariant check](language-priority/invariants.json).

Across 6,144 isolated English word/prefix episodes, there are zero individual
first-five or first-eight losses and zero Space changes. All 3,072 English-mode
episodes have identical full-list ordering. Chinese-mode intended-word ranks can
move down by one place, but no measured first-page target is displaced. Separate
source-role/genre word-probe groups, including the old dev/test roles, are in
[English safeguards](language-priority/english-priority-summary.json). These
previously exposed isolated words are not fresh holdouts or conversation accuracy.

Broad evaluations use lowercase Pinyin/English and a Pinyin board. The final
capitalization/Zhuyin guards do not change those inputs; their additional boundaries
are verified in the core regression. No label or Google candidate is read by the
runtime. No dictionary, threshold, weight, candidate quota or word exception changes.

Local core passes 356,307 behavioral assertions, including preceding-Latin context,
mode switching, raw acceptance, capitalization, privacy, literal fields and partial
spans. Existing contraction, custom mapping and async-lifecycle regressions also
pass. Assertion counts are not language accuracy. Desktop per-key p95 is 0.47 ms
in this run, versus 0.49 ms in the earlier baseline; these are not paired speedup
measurements or Android touch latency. The new rule is one bounded linear scan
of existing choices, with no dictionary/model memory increase.

The correction preserves conservative literal Space defaults. Therefore better
first Chinese alternatives must not be reported as improved automatic acceptance.
Sentence/phrase coverage and irrelevant lower-ranked alternatives remain work
for source and contextual-evidence improvements. This commit closes the confirmed
leading-language defect without claiming to close all Google Zhuyin gaps.

## Phone result

Reran the same frozen 16 collision inputs against Google and MinIME, with no
missing observations or action failures. In MinIME, English-led collapsed rows
fall 16 → 0; intended-target first alternatives rise 0 → 4. All six previously
visible intended Chinese targets remain visible, and none is newly retrieved.
There are no per-case acceptance changes. English extensions remain reachable;
this change fixes priority rather than silently dropping dictionary entries.

Examples: `sy` now leads with 所以 before system; `tai` with 台 before tail;
`mingl` with 命令 before mingled. The first two still illustrate differing Chinese
homophone choices and source coverage from Google. This is not proof that every
new first candidate is the user's intended word.

Phone observations exercise the lowercase Pinyin priority change before the final
capitalization/Zhuyin guards. Those guards do not affect these frozen inputs;
their additional paths passed final core and pinned desktop checks and the final
APK build. No phone touch-latency claim is made. The final desktop run processes
13,014 rows, with 11,272 uncached native queries, IPC+decoder p50/p95 2.339/4.499 ms.

Sessions `32558511-2d87-4466-a91e-136675436cc2` and
`f6326ba2-1de1-4354-a461-a4b585af365d` restored prior IME and MinIME preferences,
verified display OFF after evidence collection, and explicitly released the phone
window to SHINE. [Observed rows](language-priority/phone.json) and the archived
evidence retain the results and limits.
