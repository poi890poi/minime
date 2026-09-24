# Broader grammar evidence still cannot determine mixed-language intent

September 25, 2026. **Reject this replacement after the mixed-mode pipeline
screen.** The grammatical-coverage and engine-contract checks pass, but a
previously available English prefix loses first-eight coverage and its Space
output changes. Production assets and runtime remain unchanged.

## One declared source change

The [frozen proposal](PRODUCTIVE-GRAMMAR-PLAN.md) adds source-defined pronoun and
suffix evidence to the whole Wiktionary contraction inventory. The full pronoun
graph has 15 categories, 1,656 memberships and 1,221 unique pages, identical in
two passes. Snapshot SHA-256:
`2d5bdf0595170391a33e877c793afa4606d90ec1071286ee4b9a6d6e0984a51a`.
ASCII mainspace filtering retains 769 pronouns. Structural extraction from the
pinned MIT spaCy table finds six suffixes; no suffix/word whitelist was supplied.

The union has 5,002 possible forms. Intersecting with the **existing** accepted
apostrophized vocabulary or twice-observed context unigrams retains 81 flags,
versus 52 old flags. No generated vocabulary enters the dictionary. All bare-word
protection bytes and lexical/context frequencies remain unchanged. Metadata hash:
`196992eb4a12312d2990ed77b2a02c69ba8a2bdbbe634a63e737829ac2708ed3`.
The method was implemented and hashed before reference evaluation; it was not
modified to repair observed losses.

## Independent references and their limits

All three older GUM splits were previously exposed. A new upstream revision,
`1fe635509c649e376dfb449d528424ab78f4eaee`, provides 257 documents. Before
extracting labels, the [holdout procedure](GRAMMAR-HOLDOUT-PLAN.md) excludes 216
previous document IDs and one new ID sharing a normalized 20-token span with
previous GUM/EWT/MASC material. It retains 40 documents: one conversation, eight
court transcripts, nine essays, twelve letters, one news article and nine
podcasts. No document was selected for a contraction or a favorable output.

The frozen document manifest hash is
`1add51de8a8ceba147dd2a8d007795e11c8fe902c621fc1bd95dcf464ecb926e`.
This excludes known text overlap, not shared authors/speakers or paraphrases.
One conversation and one news document are small samples. The GUM distribution
includes noncommercial/per-document licensing constraints; it remains local
evaluation evidence, never a production source. No source text or document list
is included in this report. These documents are now exposed evaluation data,
not a fresh holdout for a future revised proposal.

| AUX-bearing reference | Eligible occurrences | Old included | Proposed included |
|---|---:|---:|---:|
| Reused EWT dev | 207 | 204 | 204 |
| Reused EWT test | 233 | 232 | 232 |
| Reused GUM test | 206 | 197 | 198 |
| New documents, all genres | 639 | 629 | 632 |
| New conversation | 35 | 33 | 33 |
| New essays | 80 | 79 | 79 |
| New podcasts | 318 | 314 | 316 |

There are no lost AUX-bearing occurrences within any source/genre. On the new
documents, non-AUX verbal inclusion rises 34 to 44 of 44, while nonverbal inclusion
changes 3 to 2 of 135 (one gain, two losses). These are **surface inclusion counts**,
not prediction accuracy or independent word counts. Nonverbal inclusion retains
possessive ambiguity; a grammar label does not establish the user's intent.
Full genre/category and unique-surface counts are in `productive-grammar/`.

## Engine and mixed-mode outcomes

All 81 proposed flags pass three casing styles in two modes: 486 engine episodes.
There are 366 restorations with successful Backspace/raw-selection recovery and
120 protected bare spellings. These prove engine contracts, not appropriateness
of automatic acceptance. Two productive-rule fixtures and two holdout-boundary
fixtures pass alongside the existing source-reader/reference fixtures.

A fresh paired pipeline run records matching configuration receipts, declaring
only `spelling` changed. All 3,072 English-mode episodes preserve complete ordered
inventories, ranks and Space. Chinese-mode English complete and missing-last
conditions also remain unchanged. In 1,024 half-prefix episodes, first-eight
reference coverage falls **459 to 458**; three inventories change and one Space
result changes. There is no compensating gain in the affected source/genre.

The inspected divergence is `uns`, a half-prefix of the reference word `unsafe`:
the proposal inserts `un's` ahead of ordinary completions, changes Space from raw
`uns` to `un's`, and moves the reference from rank 8 to 9. Two `ima` prefix cases
also gain `I'ma` before ordinary completions, without changing Space. These
examples diagnose all changed ranks/defaults; none is patched or promoted.

The causal boundary is precise: `englishSpelling` grants mixed-mode grammatical
status; `englishApostrophes(raw, false)` uses it as the filter; composition then
puts restoration ahead of other completions and may prefer it on Space. The
source's broad dialectal/historical membership plus existing lexical attestation
does not establish sufficient intent for that priority. English-only mode ignores
these flags, explaining its unchanged outputs.

The frozen coverage gate fails. Stop before broad/chat, core/native, Android or
phone admission checks for this version. Do not claim those unrun gates passed.
Retain the source and negative result, without adding per-word exceptions or
frequency thresholds chosen from these examples. A future design must explicitly
separate lexical availability from automatic mixed-language acceptance and obtain
independent intent evidence. Context-count replacement and source rights remain
separate open release items.
