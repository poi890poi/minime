# Chinese candidate recovery audit — 2026-09-18

Status: local behavioral fixes and expanded regression evidence; **not release
approval or a claim of Google Zhuyin parity**. Baseline: `dbe7722`. No production
dictionary entries or weights changed. No complaint-specific words were added.

## Cause and scope

This is a bug fix plus test expansion. The default Java decoder only queried
stored whole-input entries and completions. Its sentence construction had already
been removed, but it had no replacement path for selecting the first character of
an unmatched longer input. The earlier first-character work covered the native
Rime adapter, while Rime is disabled by default. Narrow native/device examples
missed the default path.

`FirstGlyphIndex` now indexes source-attested single Han characters and their
reading prefixes. It segments the input to identify a first raw span, without
constructing any phrase text. Explicit selection consumes that span and preserves
the suffix. Whole-input identities win deduplication. The text and binary loaders
build the same index; there is no binary-format or preference migration.

Composition now orders whole-input choices ahead of partial choices before
selecting its default. A mixed row with only one whole phrase previously placed
the third prefix glyph ahead of the first two while reserving two phrase slots.
Insertion now uses the number of whole choices actually present. Prefix choices
cannot activate a later incomplete add-on as the Space default.

Risk boundaries: extra prefix choices can displace phrases from the first eight
positions; segmentation is ambiguous for unseparated initials; native and Java
scores still differ; the index costs memory and lookup work. No new glyph
frequency model, Android layout change, or generated multi-character construction
is part of this fix. English, Taiwanese, Japanese, Zhuyin, privacy and learning
contracts still run in the full core suite.

## Frozen data and denominators

`manifest.json` pins 31,527 episodes / 26,127 unique spellings:

| Group | Episodes | Interpretation |
|---|---:|---|
| All-source-syllable chains | 6,784 | 424 syllables × lengths 3/4/6/10 × full/initial/mixed/separated; mechanical contracts only |
| Existing glyph audit | 499 | Whole syllables and proper prefixes; no intended phrase labels |
| Encyclopedic regression | 8,000 | Existing Chinese GSD/other frozen rows; not natural conversation |
| Taiwan.md essays | 11,220 | Previously evaluated prose with document lineage |
| Authored conversations | 24 | Previously inspected scenarios, not a representative population sample |
| Stored phrase retrieval | 5,000 | Hash-selected dictionary phrases; seen-source retrieval, not generalization |

The 24,244 labeled episodes are the last four groups. None is a fresh holdout.
Targets never enter decoding or runtime ranking. Reports separate genre,
spelling condition and length. The historical filename `pinyin-fresh-holdout.tsv`
does not make those reused examples fresh again.

A separate `imprecision-manifest.json` freezes 3,560 deletion, adjacent-key and
transposition episodes / 3,177 unique spellings from the existing imprecision
corpus. These have no independent intended-output labels. They test recovery and
acceptance contracts, **not autocorrection accuracy**. Adjacent-key probes cover
horizontal QWERTY neighbors, not measured finger distributions or joined KALQ.

## Results

Counts below compare baseline → fix. “Empty” excludes the raw-input choice.
“First 8” means eight nonraw candidate positions, not eight visible phone cells.
“Target available” requires a whole-input match to the recorded expected text.
Different homophones may also be legitimate; this does not measure semantic
precision or prove the absence of irrelevant suggestions.

| Profile | Empty / 31,527 | Whole target available / 24,244 | Whole target in first 8 / 24,244 | Desired first glyph reachable / 24,244 |
|---|---:|---:|---:|---:|
| Java, packs off | 15,204 → 0 | 13,102 → 13,102 | 11,857 → 11,591 | 1,022 → 23,572 |
| Java, default Taiwan/geography packs | 14,996 → 0 | 13,241 → 13,241 | 11,835 → 11,531 | 1,022 → 23,572 |
| Rime + Java + default packs | 0 → 0 | 13,832 → 13,832 | 11,652 → 11,772 | 17,322 → 23,688 |

No previously reachable whole target was lost in these profiles. Java Space
acceptance did not change, with or without packs. The default profile still lacks
the expected whole phrase in **11,003 of 24,244 labeled episodes**; first-glyph
recovery does not solve phrase recall. Reserving first-glyph positions also pushes
304 previously early whole targets past position eight in net terms. This is a
visible access tradeoff requiring phone comparison, not an unqualified gain.

Rime Space changed in 188 episodes: 94 now retain raw spelling rather than
indirectly accepting a completion through a partial glyph; other changes follow
whole-input ordering and exact dictionary protection. Among all changed labeled
episodes, 47 become the reference text and 34 cease to be it. The reference's
first glyph in the first eight positions falls from 9,108 to 8,987 even while
overall first-glyph reachability rises. These native changes require review;
aggregate improvement does not erase the regressions.

Imprecision probes: empty lists 1,088 → 282, prefix recovery 0 → 3,033,
zero Space changes. The remaining empties include inputs without a recoverable
source-backed first reading. Inventing text to eliminate them is not a goal.

Detailed counts, changed Space records, and lost-target records are in the
`*-comparison.json` files. `evidence/` retains compact per-episode records with
the first eight outputs and a hash of the full candidate list; full raw TSVs
remain under `artifacts/`. No assertion count is presented as accuracy.

## Verification and performance limits

- `tools/test-core.ps1`: PASS, 355,734 behavioral assertions. Includes all 424
  syllables, 848 ordinary/private episodes, 948 repeated prefix-selection steps,
  every source homophone at an explicit first boundary, text/binary parity,
  stale taps, deletion and mixed-row order.
- The same new regression fails against the archived baseline classes at the
  first source-derived explicit first-syllable homophone check (`a'die'ji`).
  This establishes fail-before/pass-after behavior, not merely extra assertions.
- `tools/test-desktop.ps1`: PASS, 13,014 frozen rows / 11,272 native queries.
  Uncached native IPC + decoder p50 2.256 ms, p95 4.409 ms, maximum 139.773 ms.
- Expanded evaluator verifies suffix preservation, absence of constructed
  candidates, and no partial Space choice. The final default-pack and native
  runs also invoke Space and verify the committed text and cleared composition.
- Debug app and Android instrumentation APKs: local build passed. New device
  cases cover both decoders and source-derived 3/6/10-syllable buffers. They have
  **not run on the phone** in this session.

`phone-plans/` freezes 18 hash-selected conversation/essay comparisons before
observation, including full, initial and mixed spellings. Every case records the
original source row. An unavailable requested first glyph is a failed observation,
even when the instrumentation process exits successfully.

Per-profile lookup timings are in the JSON reports. They include Java lookup and
native IPC where applicable, but exclude add-on lookup, composition/ranking,
rendering and touch handling. Processes overlapped on the desktop; these are
diagnostic measurements, not a controlled speed comparison or phone latency.

Phone work was blocked before the first ADB command by automatic approval review,
which did not accept the other task's explicit reservation acknowledgement as
authorization. Direct user approval was requested. The unused reservation was
explicitly released; no device state changed. A new acknowledged reservation and
`tools/phone-lease.ps1` are required before operating RFCR91GWXLX. Restore prior
IME/preferences and verify display OFF at cleanup.

## Rejected source-weight experiments

The importer reads McBopomofo `phrase.occ` as whole-glyph counts and assigns the
same count to each pronunciation. Upstream also provides exclusions, curated
postprocessing and heterophony weights. These are plausible systemic sources of
ranking differences, not proof that replacing our weights is safe.

Audit source: [McBopomofo 3.1, pinned commit](https://github.com/openvanilla/McBopomofo/tree/e965b78296b1322d11ce672aaf626c5e65411881/Source/Data).
Existing BPMFBase, BPMFMappings and phrase.occ text matched the pinned source.
All 171,708 production rows mapped to the built upstream model; none was silently
dropped. `weights-manifest.json` pins inputs and variant hashes.

| Audit-only variant | Previously reachable targets lost | Whole target on Space | Desired first glyph in first 8 |
|---|---:|---:|---:|
| Replace all scores with upstream compiled scores | 19 | 8,539 → 8,508 | 14,092 → 14,321 |
| Apply only per-glyph alternate-reading penalties; preserve phrase scores | 6 | 8,539 → 8,551 | 14,092 → 14,360 |

Both fail the no-lost-target gate and are **rejected for production**. The second
changes 618 rows and loses abbreviated-query targets despite preserving phrase
weights; bounded candidate retrieval needs investigation before adopting source
priors. No weights were tuned to the evaluation labels. MOE school frequencies
and corpus absence are review signals, not universal rarity judgments.

The September 24 [bounded-retrieval follow-up](READING-PRIOR-RESULTS.md) confirms
that all 25,100 single-glyph reading pairs retain complete-spelling access. The
six older labelled losses are repeated single-character/initial-query episodes,
not six phrases. Across all 499 syllable/prefix queries, 196 removed glyph/query
pairs each match a source reading penalty; all removals disappear when only the
128-candidate cap is lifted diagnostically. The production rejection remains;
this clarifies its cause without authorizing unlimited lists or new weights.

To reproduce upstream audit inputs, obtain the pinned `Source/Data` tree and run
its `curation.builders.frequency_builder`, then
`curation.compilers.main_compiler` with its heterophony1/2/3, PhraseFreq,
BPMFMappings, BPMFBase, punctuation, symbols and macros inputs, then
`curation.compilers.postprocess` using `Postprocess.txt`. Run
`tools/audit_chinese_reading_weights.py <built-data.txt>` to create the audit-only
TSVs under artifacts. These transformed scores are not occurrence counts and
must not be copied into production data without source registration and new
independent evidence.

## Reproduction and remaining release gates

Use JDK 17, the pinned desktop Rime runtime, and Python 3. Run core and desktop
gates, then `tools/test-chinese-recovery.ps1 -Python <python-path>`. The wrapper
compares the two affected baseline classes against the current shared core and
uses the same native adapter/data on both sides. It fails on lost targets or
changed Java Space acceptance. Frozen corpora are checked in; generators are for
explicit corpus revisions, not regeneration during evaluation.

Before releasing: complete the authorized Google Zhuyin comparison and Android
integration runs; resolve or explicitly assess native default/rank changes;
obtain broader independently sourced conversational labels; investigate source
frequency/reading coverage without exceptions; measure phone latency and memory.
The current evidence establishes a recovery fix and exposes remaining quality
gaps. It does not justify declaring Chinese suggestions fixed.
