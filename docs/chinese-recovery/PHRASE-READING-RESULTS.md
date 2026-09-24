# Phrase-derived pronunciation shares

**Retain for integration checks; not yet a production refresh.** The isolated
model passes its predeclared complete-reading screen and improves agreement
with the new Google phone sample. It still has rank tradeoffs and has not been
tested as packaged data or through the native/core merge. Release is not cleared.

## Problem, cause and change

The importer assigns the same whole-glyph frequency to every pronunciation.
An uncommon reading can therefore inherit the entire frequency of a common
glyph. The curated priority lists cover only some readings and have independent
editorial/parser limitations. No complaint-specific exceptions are introduced.

`tools/phrase_reading_prior.py` estimates conditional reading shares from the
existing retained Taiwan phrase source: 100,149 positive-frequency multi-glyph
words and 101,179 distinct toneless reading tuples. Ambiguous tuples split a
word's evidence equally; repeated tone aliases do not multiply it. Each glyph's
original frequency is distributed across its existing readings. Missing evidence
uses a uniform distribution. Words and single-reading glyphs remain unchanged.

903 of 171,708 rows change only their numeric count. Six generated structural
tests verify conservation, ordering, duplicate-tone invariance, ambiguous tuples,
fallbacks and malformed data. All 25,100 original complete single-Han reading
pairs remain reachable. The 128-candidate limit removes 274 abbreviated-query
identities, each still available by full spelling. No dictionary entry is added
or deleted. 87 phrase/glyph observations involve 56 readings absent from the
single-glyph inventory; they are counted and excluded, not invented. 5,382 of
21,740 glyphs have multi-word evidence; 16,358 do not. Most absent-frequency rare
glyphs remain at zero. These are lexical phrase counts, not measured spoken
pronunciation frequencies.

## Measured results

| Frozen complete-reading screen | Production | Trial |
|---|---:|---:|
| Reference-supported Space choices, of 405 spellings | 305 | 313 |
| Supported whole-input glyph slots among first eight | 1,531 | 1,535 |
| Covered glyph slots without that complete reference reading | 265 | 232 |
| Available reference glyph/reading pairs, of 6,402 | 6,257 | 6,257 |

There are 12 supported Space gains and four losses. The last first-eight metric
includes valid spelling completions and is **not an invalid-candidate rate**.
The scorer separates phrase/literal, prefix-selection and uncovered-glyph slots.
No labels enter the estimator. This previously inspected MOE reference is
independent editorial evidence, not a fresh holdout or conversation accuracy.

| Broader regression | Cases | Whole target in first eight, before → trial | Target-compatible first-eight slots |
|---|---:|---:|---:|
| Encyclopedic | 8,000 | 5,285 → 5,275 | 7,440 → 7,510 |
| Essays | 11,220 | 1,249 → 1,249 | 12,088 → 12,268 |
| Authored conversation scenarios | 24 | 0 → 0 | 38 → 38 |
| Source-retrieval | 5,000 | 4,999 → 4,999 | 11,059 → 11,157 |
| Edited chat excerpts | 1,536 | 70 → 70 | 1,859 → 1,872 |
| Short spans from those excerpts | 3,072 | 540 → 540 | 2,946 → 2,980 |

Compatible slots equal the target or a selectable prefix of it. Other homophones
are not automatically meaningless. All 28 encyclopedic first-eight losses are
single-glyph cases; 18 have independently supported complete readings and ten
are partial or lack that complete reading. Thus ambiguous old labels do not
excuse every demotion. All old labels and losses remain recorded. Essays, chat,
source retrieval and mechanical conditions have unchanged Space choices. Broad
Space changes in 145 cases. First-eight whole-target retrieval and useful-prefix
access are different measures; the latter improves while the former loses ten
net encyclopedic cases.

All 6,144 English word/prefix probes retain their English reference ranks and
availability. English-mode inventories and Space choices are identical. Chinese
mode changes Chinese ordering and nine half-prefix Space choices; no English
reference is displaced. These are isolated words, not fluent-conversation tests.

## Google comparison and remaining cause

A new plan was frozen before phone observation: four adverse complete-reading
cases plus eight each of gains, changes supported on both sides, and controls.
Sampled groups exclude the prior 24-query reference session. All 28 Google typed
screenshots were visually checked; all 28 MinIME inputs and Space choices match
the core baseline. Both providers finish composition on Space.

| Google Space agreement | Production | Trial core |
|---|---:|---:|
| All 28 diagnostic cases | 5 | 12 |
| Eight complete-reading gains | 0 | 5 |
| Eight changes supported on both sides | 1 | 3 |
| Eight unchanged controls | 4 | 4 |
| Four adverse complete-reading cases | 0 | 0 |

Eight matches are gained and one is lost. This stratified diagnostic is not a
population hit rate, and the trial data were evaluated in core, not installed.
Examples generated by the frozen selection include `shua`: 率 → 刷, `xuan`:
還 → 選, and `xiao`: 學 → 小, agreeing with Google. `han`: 和 → 漢 loses
Google's choice. These observations are evaluation only, never exceptions.

The four adverse cases reveal a separate ranking issue. After the source prior
is reduced, `gu` can choose 國 (`guo`), `gua` 關 (`guan`), `cen` 曾 (`ceng`),
and `zha` 長 (`zhang`). These are valid incomplete-reading matches, not corrupt
entries. Google chooses fully typed readings in these cases. Test completeness
ordering as a separate shared-decoder hypothesis before combining changes;
do not tune the statistical model to these four examples.

Phone session `9d643e89-e4eb-4280-a9ec-9590f97d7568` completed 56 observations and
112 captures. Original APK/preferences/learning/prior Samsung IME were restored;
display OFF was checked by all three cleanup layers and the lease was explicitly
released. Raw captures, joins and reference text remain local.

The current production core passes 1,770,117 contract assertions. The pinned
desktop run completes 13,014 inputs / 11,272 native queries with output SHA-256
`72ed6f69cf0a838ec9c03a4d8de2a3e94e5991531b344ae1f173d95bff2d6bfe`, unchanged.
Those checks validate the current runtime and evaluation harness, not the trial
native merge, Android timing, physical touch or Play release. Aggregate receipts
and exact source/output pins are in `phrase-reading/`.
