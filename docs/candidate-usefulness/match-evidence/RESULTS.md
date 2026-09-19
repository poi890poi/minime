# Development loop: reading-match evidence

## Problem and cause

The same `incomplete` flag represented both omitted letters within matched
syllables and forward predictions. `ReadingUnitIndex` lost the distinction by
calling `completing`; English prefix completions omitted the flag entirely.
Equal-score supplemental duplicate paths also retained whichever provider ran
first. This prevented downstream policy from reliably distinguishing these cases.

## Changes accepted

Reading-unit and first-glyph matches now retain `abbreviated=true` when their
matched units omit letters. `incomplete` continues to mean untyped letters, so
existing fully-typed acceptance guards remain conservative. English completions
set `incomplete=true`. Equal-score duplicate paths to the same supplemental text
retain unit evidence. Copies, paired forms and combined paths preserve the
distinction. No new object fields, binary format, source entries or score weights.

This is a prerequisite for better ranking, **not a measured improvement in visible
candidate quality**. The next ranking experiment must demonstrate its own gain.

## Measurements and rejected approach

Baseline is `2363722`; corpora are existing regression data, not fresh holdouts.
Raw recovery is excluded from Chinese alternative ranks. English rank includes
raw recovery; its whole-word rank is therefore not a completion success measure.

| Check | Baseline | Accepted change |
|---|---:|---:|
| Chinese episodes | 31,527 | 31,527 |
| Candidate text/span/order differences | — | 0 |
| Space-output differences | — | 0 |
| Intended whole phrase in first eight, labeled tasks | 11,531 / 24,244 | 11,531 / 24,244 |
| English word/prefix episodes across Chinese and English modes | 6,144 | 6,144 |
| English inventory/order/Space differences | — | 0 |

The rejected trial cleared `incomplete` on abbreviated matches. Optional initial
aliases then entered the fully-typed promotion/default path: 3,578 Space choices
changed and first-eight whole-target coverage fell to 10,861 / 24,244. Useful
first-eight slots fell from 30,623 to 29,953 / 193,940. Candidate/span inventories
were unchanged: this was a policy regression, not a retrieval loss. The existing
first-glyph test also caught a consumer interpreting `!incomplete` as full spelling.
The trial patch, per-case differences and aggregate results are retained under
`rejected/`. That behavior was withdrawn rather than weakening the gates.

The accepted core suite passes 370,428 assertions, including the new test failing
against baseline at initial/mixed classification. Assertions measure contracts,
not accuracy. Host key processing p50/p95/max: 0.03 / 0.54 / 75.36 ms. Pinned
desktop Rime processed 13,014 rows, 11,272 uncached queries; native IPC plus decoder
p50/p95/max: 2.332 / 4.625 / 156.167 ms. These are separate timing boundaries,
not phone touch latency or evidence of a speedup. No phone/build/release claim.

## Reproduce

Run `tools/test-core.ps1` and `tools/test-desktop.ps1`. Run
`ChineseRecoveryEvaluation docs/chinese-recovery/corpus.tsv.gz OUTPUT core packs`
and `MixedEnglishEvaluation OUTPUT` using the compiled core classes. Compare to
`artifacts/language-priority/chinese.tsv` and `english.tsv` with
`tools/compare_priority_invariants.py` and `tools/report_mixed_english.py`.
Evidence hashes and logs are in this directory. Conversation and essay limitations
from the original corpus remain; unchanged output is not evidence of good quality.
