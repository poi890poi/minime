# Candidate usefulness audit — 2026-09-19

MinIME still has a substantial phrase-efficiency gap to Google Zhuyin. More
retrievable dictionary matches did not establish a better visible candidate list.
The extra-eight prefix expansion was withdrawn in `9d3f650`; see
[the decision and contrary evidence](DECISION.md).

## What the measures mean

- **Whole-target access:** the specified intended text is available as one choice.
- **Useful phrase:** that whole text or a multi-glyph prefix that helps enter it.
- **Useful glyph:** a valid first-glyph recovery, reported separately from phrases.
- **Target-compatible precision:** useful slots divided by shown candidate slots.
  An alternative homophone can be a real word and still be irrelevant to this task.
  This is not a count of linguistically valid words or a general nonsense detector.
- **Search burden:** off-target choices before the first useful choice. Missing
  useful choices are unavailable, not zero-effort successes.

Core prefix labels require a nonzero consumption span shorter than the raw input;
whole-target labels require whole-input consumption. Phone screenshots establish
textual compatibility only, not those consumption semantics. Raw spelling recovery
is excluded from slot precision consistently, although it still uses real screen
space in MinIME. Accessibility-visible edge items can be partly clipped.

The frozen inputs, hypotheses, ownership boundaries and limitations are in
[PLAN.md](PLAN.md). The main core audit reuses 31,527 episodes, of which 24,244 have
intended-text labels. Mechanical rows are excluded from precision. These previously
evaluated corpora are regression evidence, not fresh holdouts. Genre and input
condition results remain separate in the JSON reports. Labels never enter runtime.

## Actual phone comparison

The corrected plan tests identical input on both keyboards, captures the collapsed
row and first expanded viewport, collapses the list, then presses Space. It contains
12 authored conversation and 12 essay regression cases, frozen before observations.
Reference: Google Zhuyin `2.4.5.164561151-arm64-v8a`, code `2451413`.
MinIME uses the restored eight-prefix runtime, default local decoder, Rime off,
and fresh MinIME preferences per case; Google's learning state is not reset.

| Result on 24 specified tasks | Google Zhuyin | MinIME |
|---|---:|---:|
| Whole intended text first in collapsed alternatives | 16/24 | 2/24 |
| Whole intended text somewhere in collapsed alternatives | 16/24 | 3/24 |
| Useful phrase in collapsed alternatives | 20/24 | 12/24 |
| Any useful choice in collapsed alternatives | 20/24 | 18/24 |
| Target-compatible collapsed slots / all shown slots | 38/105 (36.2%) | 28/101 (27.7%) |
| Mean longest useful collapsed text, glyphs per task | 3.13 | 1.46 |
| Useful phrase in expanded first viewport | 21/24 | 15/24 |
| Any useful choice in expanded first viewport | 23/24 | 23/24 |
| Off-target choices before first useful expanded choice | 33 over 23 available tasks | 68 over 23 available tasks |
| Space actually commits whole intended text | 16/24 | 2/24 |
| Missing acceptance observations | 0/24 | 0/24 |

Expanded-list recovery equality hides a large efficiency gap: users still scan
more alternatives and accept shorter pieces in MinIME. Fixed first-eight core
metrics are not a substitute for the real pixel-limited row.

Examples below illustrate mechanisms; all 24 results are retained in
[phone-final.json](phone-final.json), including failures to match the target.
None is a production dictionary exception.

| Input → intended text | Google collapsed lead | MinIME collapsed lead |
|---|---|---|
| `wxhj` → 我想回家 | 我想回家, 我想, 危險 | 文獻會, 我, 為 |
| `nyl` → 年雨量 | 年以來, 難預料, nylon | nylon, 年雨量, 湳雅里 |
| `yundongjiuba` → 運動酒吧 | 運動就把, 運東就把, 運動 | 運動家, 員, 運 |
| `sxiagzhong` → 三峽國中 | 上下各種, 上下, 剩下 | 三峽國中, 是, 時 |

Google also offers off-target constructions and English alternatives. The first
24 cases show Latin text in three Google collapsed rows and one MinIME row; they
do not prove higher overall English frequency in MinIME. They do expose English
priority on `nyl`. A separate targeted English-interference comparison is described
below; never combine its selected failure stratum into a population rate.

### Targeted English/Chinese collisions

Hash-select 16 distinct inputs from the known MinIME English-first stratum, before
observing Google. This deliberately studies interference, not random typing.
Both providers completed all 16 cases; no actions or acceptance observations are
missing. [English-phone results](english-phone.json) retain the complete rows.

| Collapsed row on 16 targeted Chinese tasks | Google | MinIME |
|---|---:|---:|
| English completion leads alternatives | 0/16 | 16/16 |
| English prefix-extension slots | 0 | 31 |
| Whole intended Chinese text visible | 11/16 | 6/16 |
| Space commits intended Chinese text | 4/16 | 0/16 |

Google exposes two Latin spelling alternatives (`Dr`, `ES`), and has English
completions farther down expanded lists. Thus the difference is prominent
speculation and space allocation, not banning English. MinIME's Space still uses
its independent literal default here; a leading English alternative is not proof
that Space inserted that English word.

| Input | Google first alternative | MinIME first alternative |
|---|---|---|
| `sy` | 所以 | system |
| `tai` | 太 | tail |
| `mingl` | 命理 | mingled |
| `taig` | 泰國 | taiga |

The complete sampled list includes both cases where Google's first choice matches
the intended target and cases where it does not. This confirms excessive English
promotion for this failure stratum; it does not estimate how often arbitrary user
typing enters the stratum. Production word-level exceptions are prohibited.

The first observation plan incorrectly tried Space inside Google's expanded list,
where no Space key is available. Its 24 Google acceptance observations are missing,
not successful. [phone-before.json](phone-before.json) retains that failure. The
corrected plan retains the same inputs and adds a collapse action. Runner `OK`
alone is never interpreted as all per-case observations passing.

## Core experiments and negative results

**Withdraw the extra-eight expansion.** Across 24,244 tasks it adds 86,049 candidate
occurrences, including 478 useful phrase prefixes and 85,571 off-target choices.
Useful first-eight slots decrease 30,623 → 30,592; whole-target first choice stays
8,604. Useful phrases anywhere increase 19,401 → 19,750, and full-list precision
increases slightly. That expanded benefit is real but does not satisfy the visible
choice gate. See [core-summary.json](core-summary.json) for all genre/condition
groups and [DECISION.md](DECISION.md) for the rollback rationale.

**Reject removal of the existing phrase preview.** An isolated override disabled
only the longest-prefix display promotion. Useful first-eight phrase availability
falls 14,941 → 13,305 tasks; useful slots fall 30,623 → 27,557 out of 193,940 slots.
One-glyph useful slots rise 12,935 → 13,336, which does not compensate for losing
phrase access. Keep the existing preview. The experimental source patch, hashes,
full [report](no-preview/core-summary.json) and changed rows remain archived;
the override never enters the APK.

**English prefix priority needs separate evidence.** In the Chinese core tasks,
764/24,244 have Latin alternatives in their first eight, totaling 2,415 Latin slots:
2,412 prefix extensions and three apostrophe restorations. All 764 put a Latin
alternative first after raw recovery. Ordinary edit-distance English correction
is confined to English mode. The relevant Chinese path is dictionary prefix
completion followed by unconditional Latin-first interleaving whenever raw input
remains the Space default. A prefix existing in the English dictionary does not
establish that the user intends its completion.

The isolated `chinese-first.patch` changes one presentation rule: Chinese goes
first in each existing pair when Chinese mode has neither a known English word
nor preceding Latin context. Retrieval membership and source order within each
language remain unchanged. On the Chinese tasks, Latin-first alternatives fall
764 → 250, whole-target-first access rises 8,604 → 8,724, and whole-target first-eight
access rises 11,531 → 11,550. These are candidate ranks, not Space completion rates.

The English safeguard audit hash-selects 512 unique words from each pinned EWT
and GUM source set and tests whole words, missing-last input, and half prefixes in
both modes. These are isolated word probes from reused text, not conversation
accuracy or genre-level estimates. Ranks include explicit raw recovery here.
English mode has identical full candidate ordering and Space output in all 3,072
episodes. Chinese-mode whole words remain available as raw recovery and Space is
unchanged in all 3,072 episodes. However, Chinese-mode English completion first-eight
coverage drops 739 → 728 of 1,024 missing-last episodes, and 477 → 444 of 1,024
half-prefix episodes. First-five coverage stays unchanged in both conditions.
The tradeoff must be explicit; a Chinese-only metric would have hidden it.

Decision: **retain this experiment, do not ship it yet**. It demonstrates the
interleaving mechanism, but does not establish a net bilingual efficiency gain.
The patched class exists only under artifacts; production English ranking is
unchanged. The source-role/genre word-probe groups are retained separately in the
report; shared vocabulary makes those groups overlap. No reused test split is
claimed as a fresh holdout.

The targeted Google study supports Chinese-first presentation in these collisions,
but cannot settle the measured English-prefix loss: it intentionally contains no
English-target tasks. A blanket pair reversal also retains the same speculative
membership and leaves 250 English-leading Chinese tasks. The next comparison must
include Chinese and English intentions, preceding-language context and actual
visible width; evaluate completion evidence and default selection together.
Do not use the mere existence of some English prefix extension as sufficient
evidence of English intent, or add new language-specific quotas fitted here.

Reports: [English interference](mixed-english-before.json),
[Chinese-first experiment](chinese-first/core-summary.json), and
[English safeguards](english-priority-summary.json). No model weights, dictionary
entries or handpicked promotions are fitted to these tests.

## Verification and remaining work

After the prefix rollback, local core checks passed 356,178 behavioral assertions;
this is not language accuracy. The pinned desktop evaluator processed 13,014 rows
with 11,272 uncached native queries. IPC plus decoder p50/p95 was 2.323/4.526 ms;
this excludes Android rendering and touch latency. The debug and instrumentation
APKs built locally. The corrected phone comparison has 48 observed provider/case
records and no action failures. Each session restored prior IME and MinIME
preferences and verified display OFF, under acknowledged ownership and the shared
mutex. No hosted CI was used.

The additional interference study has 32 observed provider/case records, also with
zero action failures and verified cleanup. Raw observations, representative
screenshots, checksums and non-private cleanup evidence are in [evidence](evidence).

The remaining quality work is phrase coverage and useful ordering, not maximizing
candidate count. Preserve first-glyph recovery, test remaining-input consumption,
and audit English interference against English-positive tasks. A source-backed
but off-target phrase is not automatically useful; a composed sentence is not
automatically useful either. Require independent text/context evidence and report
both retrieval misses and visible distractors before adding new construction or
ranking mechanisms. These results do not establish Google-level quality.
