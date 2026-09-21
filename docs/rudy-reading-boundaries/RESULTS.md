# Rudy reading boundaries — 2026-09-21

## Problem and cause

Baseline: `7efab7f`. Existing source words can disappear for different reasons.
Tracing 175 source-present misses in the previous Chinese audit found 161 outside
the base dictionary's bounded unit-candidate set even under exhaustive matching,
12 requiring future syllables, and two affected by compact upstream geography
readings. Raising traversal budgets is not supported by that evidence.

The Rudy compiler preserved compact upstream Pinyin tags as single reading units.
Of 3,144 tags, 3,057 have no separators. The common matcher therefore could not
abbreviate their individual syllables. McBopomofo-derived geography readings
already retained their boundaries; this was specifically the upstream-tag path.
The archived diagnosis scripts and `retrieval-misses.json.gz` preserve the trace;
their original artifact paths refer to the pre-change evaluation and pack.

## Change

Recover boundaries only when exactly one segmentation:

- uses the existing licensed syllable inventory;
- respects every supplied separator;
- has one syllable per Han glyph.

Keep the original tag, add the separated reading, and apply the compiler's
existing initials-alias rule. No letters, names, scores, runtime candidate limits
or source snapshots change. Ambiguous readings are left alone. The new helper
does not read expected test answers or word frequency.

3,034 tags have a unique result; 110 remain unresolved or ambiguous. Deduplication
leaves 6,048 additional aliases. All 54,969 original rows and all 19,373 names are
preserved. The full pack now has 61,017 rows. No constructed output text is added.

## Retrieval improvement and losses

The frozen corpus covers every upstream tag, plus 2,359 unique supported
name/reading cases whose boundaries can independently be obtained from existing
McBopomofo word readings or unambiguous units. There are 2,360 supported upstream
tags; one duplicate name/reading query collapses when generating these conditions.
The expected readings come from source data, not the new segmentation helper.
They are not an independent pronunciation oracle or a fresh linguistic holdout.

Each number below counts cases where the intended name appears among the
geography pack's **up to eight results**, before composition merges other sources.

| Input condition | Cases | Before | After | Gained | Lost |
|---|---:|---:|---:|---:|---:|
| Original full upstream tag | 3,144 | 3,144 | 3,144 | 0 | 0 |
| Supported full spelling | 2,359 | 2,359 | 2,359 | 0 | 0 |
| Initial of every syllable | 2,359 | 15 | 2,153 | 2,139 | 1 |
| Alternating full syllable / initial | 2,359 | 212 | 2,283 | 2,071 | 0 |
| Alternating initial / full syllable | 2,359 | 16 | 2,251 | 2,236 | 1 |
| Explicitly separated spelling | 2,359 | 2,359 | 2,359 | 0 | 0 |

Initial-only first-choice hits rise from 9 to 1,245; mixed first-choice hits rise
from 138 to 1,917. These are source-retrieval results, not conversational accuracy.
First-choice gains and losses for every condition are in `summary.json`.

The two top-eight losses are `yds` → 雲戴山 (rank 3 to absent) and `jshan` → 尖山
(rank 6 to absent). Recovered names with the same shortened spelling compete
under the unchanged pack limit. They remain available by full spelling. No names
were promoted individually to hide these losses.

## Broader quality audit

31,527 frozen Chinese composition episodes include 24,244 labeled intended-text
tasks and 7,283 mechanical/glyph probes. All are reused regression data.

| Labeled genre | Cases | First-eight whole-target gains | Losses | Space changes |
|---|---:|---:|---:|---:|
| Encyclopedic | 8,000 | 1 | 1 | 0 |
| Essays | 11,220 | 2 | 0 | 0 |
| Authored conversation | 24 | 0 | 0 | 0 |
| Known-source retrieval | 5,000 | 0 | 0 | 0 |

Across all 31,527 episodes, 601 candidate lists change. First-choice target hits
and Space outputs do not change. First-eight whole-target availability rises
from 11,531 to 11,533 of 24,244 tasks. The loss is `cz` → 潮州, rank 3 to 9:
another source-backed name now receives the existing geography preview slot.
The two previously missing 八仙山 queries become retrievable.

Precision is also measured: target-compatible slots among the first eight rise
from 30,623 to 30,625 out of 193,940 displayed slots. The denominator includes
every displayed alternative, while compatibility means the intended whole text
or a selectable prefix of that text. Other legitimate alternatives count as
off-target for this specific task; this is **not** a nonsense-word rate.
Across full lists, 733 newly exposed candidate occurrences are off-target and
two are the intended whole target. Adding valid names does not make every
alternative useful for the current intent. Conversation evidence is especially
small and provides no basis for claiming broad conversation improvement.

The original English probe omitted optional packs. A second before/after run
explicitly enables both Taiwan and geography packs: 1,024 source-sampled words,
three spelling conditions, two modes = 6,144 episodes. English mode's lists,
ordering and Space are identical. Chinese mode has 42 list changes; all first-five
target hits and Space outputs stay unchanged, but `exis` → `existed` moves from
rank 8 to 9 because 二溪山 is now available. Full English spellings retain first
place in all 1,024 Chinese-mode tasks. The no-pack control also remained identical.

**Decision:** accept the source-boundary repair with these recorded ambiguity
tradeoffs. The new alternatives have source spellings that match the input;
they are not generated nonsense or leakage into English mode. No unrelated
runtime ranking policy is changed to compensate for individual targets. This
does not meet a hypothetical zero-displacement guarantee for Chinese-mode partial
queries, and it is not evidence that the general ranking gap has been solved.

## Performance and footprint

Three alternating before/after process pairs, OpenJDK 17 on the development host,
`-Xmx1g`, geography pack alone, 11,262 distinct queries per run. Each query is
measured on its first lookup within that process; repeated corpus queries reuse
the result and are not counted again. Candidate lists reproduce identically in
all three runs for each version. Runtime/JIT/GC noise is included. These figures
exclude Android touch dispatch, composition merging and rendering.

| Measurement | Before | After |
|---|---:|---:|
| Uncompressed asset bytes | 5,202,694 | 5,656,172 |
| Approximate retained JVM heap, median | 16,864,264 B | 18,572,992 B |
| Load time, median | 1,026 ms | 1,008 ms |
| Lookup p95, range across runs | 0.387–0.447 ms | 0.392–0.422 ms |
| Lookup p99, range across runs | 0.897–1.467 ms | 1.003–1.026 ms |

Cost: +453,478 asset bytes (0.43 MiB) and approximately +1.63 MiB retained desktop
heap. Post-GC heap deltas are estimates, not Android memory measurements. No
speedup is claimed from these overlapping distributions; the test found no
material pack-lookup slowdown. Raw samples are in `resource-samples.json.gz`.

## Validation and reproduction

- `tools/test-core.ps1`: passes 887,604 contract assertions. This count varies
  with data-derived probes and is not a language accuracy score.
- `tools/test-desktop.ps1`: passes 13,014 inputs / 11,272 uncached native queries
  against the pinned desktop Rime evaluator. This is not a fresh Google Zhuyin
  comparison or phone test.
- `tools/test_pinyin_boundaries.py`: exhaustive synthetic segmentation oracle,
  separator constraints, ambiguity rejection and no spelling repair.
- `tools/test_sources.py`: 19 tests pass; source lock and provenance checks pass.
- Compiler rerun reproduces the pack, manifest and unavailable-reading list.
- Full tags preserved, old rows retained, name inventory unchanged, added rows
  retain Rudy upstream attribution.

Run Python with UTF-8 and use JDK 17, as in the local test scripts:

```powershell
python -X utf8 -m unittest discover -s tools -p test_pinyin_boundaries.py
python -X utf8 -m unittest discover -s tools -p test_sources.py
python -X utf8 tools/sources.py check
python -X utf8 tools/compile_rudy_names.py
tools/test-core.ps1
tools/test-desktop.ps1 -Output artifacts/rudy-reading-boundaries/desktop.jsonl
java -Dfile.encoding=UTF-8 -Xmx1g -cp core/build/manual dev.minime.core.RudyBoundaryEvaluation app/src/main/assets/geography.tsv docs/rudy-reading-boundaries/inputs.tsv.gz artifacts/rudy-reading-boundaries/after.tsv
java -Dfile.encoding=UTF-8 -Xmx1g -cp core/build/manual dev.minime.core.ChineseRecoveryEvaluation docs/chinese-recovery/corpus.tsv.gz artifacts/rudy-reading-boundaries/chinese.tsv core packs
java -Dfile.encoding=UTF-8 -Xmx1g -cp core/build/manual dev.minime.core.MixedEnglishEvaluation artifacts/rudy-reading-boundaries/english-packs-after.tsv app/src/main/assets/geography.tsv
```

Before evaluations use the geography asset from `7efab7f` with the same evaluators.
The broad Chinese baseline is the unchanged empty-context result from
`artifacts/match-evidence/chinese.tsv`. Reports are generated by
`tools/report_rudy_boundaries.py`, `tools/audit_candidate_usefulness.py` and
`tools/report_mixed_english.py`. Frozen input lineage and per-case results are
stored alongside this report. All work ran locally; no GitHub CI or phone use.

## Next loop

Separate source-backed ambiguity from unsupported guesses in the remaining
ranking losses. The larger 161-case candidate-cap bucket needs evidence about
which candidates are displaced, not a blanket limit increase. Broader natural
Taiwan conversation evaluation remains necessary before changing context scores.
