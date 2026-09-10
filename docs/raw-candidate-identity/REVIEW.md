# Raw/lexical candidate identity fix

## Pre-change impact and acceptance

Type: shared-core bug fix; diagnostic/test documentation accompanies it.
At baseline ab296aa, a source-attested supplemental output identical to typed raw
text is dropped during composition merging. Literal raw remains, but pack,
paired Han, focused learning and lexical Space acceptance are lost. The frozen
source audit reproduced 546/546 POJ collisions, including 388 paired outputs.

Owner: CompositionEngine.applyCandidates supplemental insertion. Preserve the
literal slot-zero identity and retain the distinct supplemental identity in the
ordinary candidate list. Apply the rule to every supplemental pack, without word,
language-specific or tone-specific exceptions. Do not change source data,
lookup budgets, relative dictionary order, pairing policy, stored preferences,
private-field rules or English correction. Equal raw custom literals remain
collapsed; base/English lookup paths are reviewed for equivalent loss.

Risks: identical visible text could select the wrong identity, train raw instead
of focused preferences, change Space spacing, or hide the Han alternate. Verify
snapshot selection, tap/hold/Space, both Han/POJ primaries, pairing/learning off,
no-learning and secure fields, explicit raw preference and mode isolation in core.
Android views already distinguish literal, pack and pair identities; verify real
touch dispatch with a same-text fixture. Raw recovery must stay unannotated.
No data migration, setting, source refresh, or new runtime index is needed. The
extra retained result is bounded by the existing supplemental lookup budget.
This is not a performance optimization or a vocabulary accuracy claim.

## Red evidence

RawCandidateIdentityRegression fails on the baseline merge with:
`one lexical identity survives raw collision in poj expected [1] actual [0]`.
The fixture is invented mechanics data, never a production vocabulary promotion.
The prior exhaustive source audit is retained separately as before-results.

## Results and review of other languages

The final shared-core suite passes 35,984 assertions. The new tests first failed
for missing lexical identity, then exposed a second failure: legacy default
selection compared raw and focused lexical preferences using the same text key.
Focused defaults now use the existing FOCUS language namespace exclusively;
explicit raw recovery remains independent. An older ModePriorityRegression
assertion encoded the raw-only behavior and was corrected to assert both distinct
identities. A discarded same-text custom literal is filtered before deduplication
so it cannot reserve the lexical candidate's text.

The frozen 1,092-query source replay retrieves and displays all 546 raw-collision
POJ outputs after the fix (zero before). All 546 marked controls remain visible;
control default-output equality stays 471/546, which is not a correctness label.
Dictionary lookup is unchanged. No new dictionary entries, frequency promotion,
or invented tone variants were introduced. The 388 paired collision outputs
can retain their existing source Han metadata; mechanics tests verify both primary
forms and all acceptance paths, pairing off, learning off, private/secure fields.

Exhaustive shipped-file collision inventory (language-inventory.json):

| Source path | Rows inspected | Distinct input/output collisions |
| --- | ---: | ---: |
| Chinese base (Pinyin and Zhuyin each) | 171,708 | 0 |
| Taiwan names/terms | 35,667 | 0 |
| Geography | 54,969 | 0 |
| Japanese words | 43,921 | 0 |
| Japanese characters | 1,279 | 0 |
| POJ | 244,131 | 546 |

The English lexicon has 44,127 rows. Exact words deliberately use the literal raw
candidate: englishCompletions excludes the identical key and supplies literal
completions only. Raw selection already retains English acceptance, spelling,
spacing and learning; there is no Han pair or focused dictionary identity to lose.
Core English isolation and contraction/casing tests pass. Synthetic same-text
fixtures exercise all four supplemental packs to protect the shared merge rule,
but they do not claim that ASCII Japanese output ships. Chinese native Rime still
emits Han output for Latin phonetics. Pinned desktop Rime evaluation completed the
existing 628-uncached-query English/Chinese development corpus; the log retains
IPC+decoder timings, which are not Android typing latency or a speedup comparison.

The baseline inputs are production-source retrieval probes, not independent
conversation/essay quality evidence or commonness labels. Source omissions,
partial search budgets, annotation coverage and homophone ranking remain separate
limitations. This fix does not certify those broader language-quality problems.

## Reproduction

Run scripts from the repository root. inventory.py creates the frozen target
selection in artifacts/poj-unmarked; inputs.tsv.gz preserves the evaluated set.
Compile UnmarkedProbe.java against core/build/manual, then run it with the same
classpath plus its output class directory. It reads current generated assets and
writes results.tsv. report.py summarizes it; archive.py records source inventory
and compresses the preserved before/after results. Before-results are captured
from baseline ab296aa, not regenerated by running the current fixed core.
Core verification: tools/test-core.ps1. Desktop verification:
`tools/test-desktop.ps1 -Corpus docs/suggestion-latency/native-inputs.tsv`.

## Android verification

The offline debug app/test build and lint pass (zero errors, 17 existing warnings).
On authorized phone RFCR91GWXLX, PairedCandidateViewTest (7) and
CandidateStabilityTest (9) pass: 16 tests in 24.858 seconds. New cases dispatch
real touch down/up and long press on the same-text POJ candidate, and accessibility
click on its Han-primary representation. They verify phonetic tap, Han hold and
Han accessibility selection through the actual view and shared engine.
The previous Samsung Honeyboard IME and saved MinIME settings/learning were
restored and read back; the display was put to sleep and OFF was verified.
No physical latency or human touch hit-rate certification is claimed.
