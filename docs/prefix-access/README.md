# Access to shorter stored Pinyin prefixes

**Decision superseded:** the extra-eight retention addition was withdrawn after
the [joint usefulness audit](../candidate-usefulness/DECISION.md). The measurements
below remain historical evidence, not the current runtime acceptance decision.

Baseline: `96db0d6`. This follow-up retains shorter high-frequency stored words
that the previous eight-longest-prefix limit discarded. It keeps those original
eight and unions the eight highest existing source-score choices, at most sixteen
words. It reuses the same traversal, omission penalties and 2,048-state budget.
No dictionary entries, phrase exceptions, fitted weights, constructed sentences,
binary format, settings or learning policy changed.

The synthetic homophone-pressure regression fails against the baseline and passes
with retention, including binary/text parity, ordinary/private selection and exact
suffix preservation. It is a behavioral regression, not language accuracy.

## New frozen evidence

The corpus contains 6,000 source phrase families outside the previous 12,000
partial-form families, seven input forms each: full, initials, mixed, reverse
mixed, partial syllables, separated initials and separated mixed. Each gets a
deterministic two-syllable suffix. The 3,000-family development and validation
partitions were fixed before results. The vocabulary was already tested in full
source audits; this is new suffix-condition evidence, not unseen natural language.
The expected target and suffix are used only for evaluation, never decoding.

| Split, 21,000 episodes each | Target prefix available before → after | Target in first eight before → after | Leading target before → after |
| --- | ---: | ---: | ---: |
| Development | 12,957 → 13,086 | 8,482 → 8,434 | 8,345 → 8,345 |
| Reserved validation | 13,426 → 13,527 | 8,841 → 8,785 | 8,720 → 8,720 |

“Available” requires the exact expected text **and** the correct consumed span;
selecting it must preserve the expected suffix. First eight excludes raw recovery
and counts a fixed eight candidates, not a measured pixel-width row or grid page.
Leading means the first nonraw suggestion, not necessarily the Space default.
These columns overlap and are not added together as a success rate.

Both partitions lose zero previously reachable targets and change zero Space
defaults, whole-choice inventories/orders or first-two-glyph identities. Source
reading spans are checked independently against exhaustive source-unit matching:
268,302 returned prefix occurrences on development and 268,274 on validation.
All 13,086 + 13,527 available target prefixes were selected and their remaining
composition and rejection of a stale selection verified.

The cost is explicit: 48 development and 56 validation targets fall below the first
eight when additional shorter words enter the existing source-score order. This
change improves expanded-list access, not first-page target accuracy. The separate
source-score preview experiment reduced development first-eight targets to 619;
it was rejected and never evaluated on validation. See [decisions](EXPERIMENTS.md).

## Earlier composition corpus, reused regression

Run all 31,527 prior episodes, with 24,244 labeled episodes. A target-consistent
prefix below is a returned stored word at the beginning of the expected text;
it does not constitute a complete sentence prediction.

| Genre | Labeled episodes | Target-consistent prefix before → after |
| --- | ---: | ---: |
| Authored conversation | 24 | 22 → 24 |
| Essay | 11,220 | 5,871 → 6,236 |
| Encyclopedic | 8,000 | 77 → 80 |
| Source retrieval | 5,000 | 3,183 → 3,190 |

Total target-consistent prefix availability rises 9,153 → 9,530. Its first-eight
count drops 5,913 → 5,897. Complete targets remain 14,141 at any rank and 11,531
in the first eight, with no lost complete targets or changed Space defaults.
Expected first-glyph availability remains 23,882, while first-eight access drops
13,583 → 13,567. The added prefixes can displace lower-ranked glyph homophones.
The 24 authored conversations remain small, reused evidence; whole-sentence
targets are still absent in all 24. Do not infer Google Zhuyin parity from this.

## Cost and verification

Desktop Java dictionary lookup, excluding rendering, touch and acceptance:

| Query set | p95 before → after |
| --- | ---: |
| New development, 21,000 episodes | 0.818 → 0.793 ms |
| New validation, 21,000 episodes | 0.778 → 0.959 ms |
| Prior composition, 26,127 unique queries | 0.999 → 0.965 ms |

These runs do not show a reproducible speedup. Validation adds about 0.18 ms at
p95. JSON reports retain mean, median and maxima; desktop figures do not establish
phone touch latency. Output can grow by eight candidate objects; traversal limits
and the serialized model are unchanged.

Local core suite: 356,196 behavioral assertions passed. Pinned desktop Rime:
13,014 input rows, 11,272 uncached native queries, IPC + decoder p95 4.419 ms.
Counts of assertions and source spans are not language accuracy measures.

Phone plan: hash-select one newly recovered development case per condition that
has recoveries (six cases), then exercise ordinary and private fields. Letters and
expanded-list selection use visible controls. The two apostrophe-separated cases
use Android hardware apostrophe events: the current soft Pinyin board lacks a
separator key, and palette quotes commit literal text. That remains a distinct UI
gap, not a silently passed soft-keyboard test.

All twelve selections passed, including exact committed text, suffix composing
spans and Backspace editing. [Phone record](phone.json) pins the APKs, fixture and
session. The 293.208-second duration includes setup and accessibility waits, not
typing latency. The shared-mutex session restored and read back the previous
Samsung IME and MinIME settings/learning, verified display OFF and explicitly
released the phone. No phone operations remain queued.

The [ordinary](evidence/phone-ordinary.png) and
[private](evidence/phone-private.png) screenshots show the first case after
selection and Backspace: `lrlz` → `令人lz` → `令人l`. They are editing evidence,
not a claim that lower candidate ranks or learning are identical in both fields.

Decision: land retention as a bounded expanded-access improvement. Keep the
existing preview selection; reject the source-score preview variant. Ranking,
natural-conversation coverage and the soft separator path remain follow-up work.

## Reproduction and limitations

`make_prefix_access_corpus.py` reproduces the frozen corpus and pins. Compile core
with `tools/test-core.ps1`, then run `dev.minime.core.PrefixAccessEvaluation` with
the gzip corpus path and output TSV path. Run the baseline at `96db0d6` using the
same harness. `report_prefix_access.py` compares per-episode ranks and invariants.
`ChineseRecoveryEvaluation` with `docs/chinese-recovery/corpus.tsv.gz`, `core packs`
and `summarize_chinese_recovery.py` reproduces the earlier-corpus comparison.
`tools/test-desktop.ps1` runs the pinned native evaluator; all work is local.

`archive_prefix_access.py` stores gzip per-episode evidence and hashes under
`evidence/`; full composition inventories stay in local artifacts. Baseline
composition evidence remains in `docs/suggestion-coverage/evidence/`.
`make_prefix_access_phone.py` reproduces the six-case integration fixture from
development outputs, independently of any validation labels.

Larger Taiwan conversation sources were found and registered as evaluation holds;
no transcripts were acquired or imported. See [source audit](CONVERSATION-SOURCES.md)
for NCCU and Sinica access, annotation and licensing limits. Further progress on
conversational ranking still needs independent conversations and usable readings.
