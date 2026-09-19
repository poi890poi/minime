# Expanded suggestion coverage — 2026-09-19

Scope: Chinese glyphs/phrases, complete spellings, initials, mixed full/initial
units, multi-letter partial units and explicit separators. No production words,
frequency exceptions or source weights were changed. This is not a parity claim.

The frozen matrix contains 315,099 labeled retrieval cases, covering all 166,181
distinct stored reading/text pairs and 12,000 hash-selected phrase families with
partial forms. Development has 157,467 cases; reserved has 157,632. Split by text
identity keeps alternate readings together. These are seen-source retrieval
contracts, not conversation accuracy or an unseen-language holdout.

## Deeper partial retrieval

The single-entry matcher retained only 24 candidates (plus length-diversity
slots), hiding many correctly matched stored words. Its state budget is 2,048.
Independent exhaustive matching over 242 frozen development queries found all
1,721 high-ranked eligible matches; this did not support rewriting segmentation.
Capacities 64 and 128 were then compared with identical scores/traversal.

Reserved results, with the 128 choice fixed before inspection:

| Condition | Cases | Target available before | After | Target in first 8, before and after |
|---|---:|---:|---:|---:|
| Initials | 6,059 | 3,334 | 4,806 | 2,697 |
| Mixed full/initial | 6,059 | 5,410 | 6,010 | 4,200 |
| Partial letters in each unit | 6,059 | 5,147 | 5,890 | 4,333 |

"Available" means the exact stored target is returned with a whole-input span;
it can require scrolling. First 8 means decoder positions, not visible phone
cells. No previously available target was lost and no first-eight sequence
changed across either corpus. Increasing capacity improves access, not top-row
ranking. Ambiguous initials still do not retrieve every source homophone.

Development lookup p95: 24 candidates 0.255 ms, 64 candidates 0.262 ms, 128
candidates 0.297 ms. Reserved 128 lookup p95 is 0.270 ms. These are uncached
desktop dictionary calls; composition, rendering and Android touch latency are
excluded. Full distributions and candidate changes are in capacity-results.json;
compressed per-case evidence is under evidence/. No phone speed claim is made.

PartialDepthRegression fails at source alternative 25 on the baseline. It passes
with 128, exercising 96 alternatives under seven spellings without constructed
text. The full core suite passes 357,085 behavioral assertions; that number is
not an accuracy percentage. The prior pinned desktop baseline covers 13,014
rows / 11,272 native queries (p95 native IPC plus decoding 4.398 ms).

## Reference comparison

The 18 previously frozen Google/MinIME cases have now been observed on
RFCR91GWXLX. Google version: 2.4.5.164561151-arm64-v8a, versionCode 2451413.
Google exposes word prefixes as well as first glyphs. Default MinIME often only
offers first glyphs when the full sentence is absent. This motivates a separate
source-backed prefix-selection change, not resuming random sentence assembly.
An unavailable requested first glyph in the collapsed row is not proof that the
glyph is absent from Google's expanded list. The runner's completion status alone
does not establish parity; detailed step outcomes must be assessed separately.

Both baseline sessions restored the prior Samsung keyboard and MinIME preferences,
verified display OFF after collection, and explicitly released the shared phone.

## Stored phrase prefixes and source boundaries

The final implementation returns existing source words for the consumed beginning
of a longer input. Selecting one commits that word and leaves the unconsumed keys
composing. It never concatenates entries or lets a partial word own Space. The
first two glyph positions remain accessible; a prefix preview cannot displace a
full first page of six whole choices plus two glyphs.

The independent validator also caught a real indexing defect: stripped aliases
and source syllables shared terminal lists. For example, the bian node inherited
弊案 from bi'an, allowing an invalid one-unit prefix. The syllable trie now uses
original source readings. Exact compact lookup still supports both readings.
The collision test fails when only the original shared-map wiring is restored.
The text and packaged binary paths both pass. No production data or weights were
changed; Gradle regenerates model.bin from the corrected loader.

Across the full matrix, 1,448,582 returned phrase-prefix outputs pass independent
source-reading/span validation. The final retrieval audit loses no target versus
the 128-capacity version and preserves its whole-target first-eight counts.
See prefix-results.json and compressed per-case evidence. This is a correctness
check of source attribution and matching, not 1.4 million natural-language tests.

## Full composition results

The existing corpus contains 31,527 episodes, of which 24,244 have phrase targets.
Unlabeled glyph/mechanical cases test recovery and editing, not linguistic accuracy.
Targets enter scoring only after decoding. The evaluator actually presses Space
and selects the first glyph and phrase prefix, then checks suffix preservation,
stale selection rejection and Backspace.

| Genre | Labeled episodes | Whole target before | Whole target after | Target-consistent phrase prefix after |
|---|---:|---:|---:|---:|
| Authored conversation regression | 24 | 0 | 0 | 22 |
| Encyclopedic regression | 8,000 | 6,940 | 7,839 | 77 |
| Essay regression | 11,220 | 1,301 | 1,302 | 5,871 |
| Stored-source retrieval | 5,000 | 5,000 | 5,000 | 3,183 |

Whole target means the complete expected text is returned with a whole-input span
at any rank. A target-consistent prefix is a returned multi-glyph word at the
beginning of that expected text; it still requires further selection/typing.
These columns overlap and must not be added together as a success rate.

All previously reachable whole targets remain available: 13,241 → 14,141 of
24,244 labeled episodes. Whole-target first-eight count stays 11,531; all Space
defaults are unchanged. Target first-glyph availability increases 23,572 →
23,882, but its first-eight count decreases 14,000 → 13,583 because prefix-word
previews take a slot. The first two glyph access positions are preserved; lower
homophones can require expansion. This remaining tradeoff is not hidden by an
overall recall number. The rejected variant also displaced whole targets from
the first eight; its results remain in pipeline-unprotected-results.json.

The conversation set is small, authored and previously reused. Complete sentence
coverage remains missing in those 24 cases. Prefix recovery improves editing,
but does not establish Google-level conversational prediction. Broader independent
Taiwan conversation evidence and attributed phrase coverage are still needed.

## Verification and cost

The final core suite passes 356,178 behavioral assertions, including 424 source
syllables, 848 private/ordinary recovery episodes, asynchronous callbacks and
English/Taiwanese/Japanese isolation. Assertion totals vary with recovery paths;
they are not accuracy measures. The final pinned desktop run completes 13,014
rows and 11,272 native queries; native IPC plus decoder p95 is 4.535 ms.

Final reserved dictionary lookup p95 is 0.566 ms versus 0.270 ms for capacity-only.
On the 26,127 unique composition-audit queries, dictionary lookup p95 is 0.999 ms
versus baseline 0.466 ms; maximum 34.569 ms versus 16.434 ms. These are desktop
measurements, exclude rendering/touch, and do not prove a phone latency target.
The new path adds bounded output collection to the existing traversal: at most
2,048 visited states, two words per terminal, eight returned phrase prefixes.

All 18 phone cases produced visible observations for both keyboards. The initial
prefix APK verified eight of ten requested phrase taps and their exact suffixes;
two desired prefixes were outside the collapsed list. Other requested glyph taps
also failed when offscreen. See phone-results.json for every case, including
unavailable actions. Google's learning state was not reset and its order changed
between sessions, so these observations establish behaviors, not a controlled
ranking-accuracy comparison. The final APK has a separate asserted expanded-list
test for all ten eligible prefixes in ordinary and private fields.

The first integration attempt assumed expansion exposed every word immediately;
三年 was farther down the grid. That failed attempt is retained in
evidence/phone-initial-expanded.txt. The test now scrolls the real list and still
requires the same selected text, composing span and Backspace result for every
case. No target was dropped or replaced.

Final result: all 20 expanded-list cases pass (ten prefixes × ordinary/private
fields), as recorded in evidence/phone-expanded-pass.txt. The existing Android
initials/mixed-input and explicit-Rime recovery checks also passed in the initial
session. The final session restored Samsung's prior IME and MinIME preferences,
verified display OFF, and explicitly released RFCR91GWXLX. No phone operations
remain queued. The eight-minute instrumentation duration includes accessibility
queries, setup and test waits; it is not a typing-latency measurement.

Visual example for mingtianxiawu: [Google](evidence/google-prefix-example.png),
[MinIME before](evidence/minime-before-prefix.png),
[initial prefix APK](evidence/minime-prefix-example.png). This illustrates
access to 明天; it does not imply the complete 明天下午 phrase is present.

Per-episode compact evidence is committed under evidence/. Its manifest pins the
complete local candidate telemetry in artifacts/suggestion-coverage; the compact
files retain expected-target ranks, first-glyph ranks, Space, timings and all
phrase-prefix outputs. Reproduce with tools/test-core.ps1, SuggestionCoverageAudit
on the two frozen gzip matrices, ChineseRecoveryEvaluation on
docs/chinese-recovery/corpus.tsv.gz, and tools/test-desktop.ps1. The summarizers
and archive_suggestion_pipeline.py generate the reports without changing inputs.
