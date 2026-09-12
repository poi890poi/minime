# Improve recall and precision together

September 12, 2026. The source-only experiment is rejected as a shipping solution.
It removed all assembly, but reserved full-spelling recall@8 fell from 674/1530
to 244/1530 with add-ons. Its native patch and Java changes are preserved only as
an experiment in `source-only/implementation.patch`; production paths are restored.
No updated release was published. Screenshot publication remains pending.

The frozen corpus contains 3–16-glyph contiguous prose spans, not exclusively
dictionary words. Many lost references are legitimate multi-word clauses.
Dictionary expansion alone cannot cover all such clauses. The missing capability
is a model of word combinations during decoding, in addition to phrase lookup.

Rime already offers a Grammar interface in Poet. Without a grammar component it
uses entry weights and a constant boundary penalty; with one it scores transitions
during beam search. Reuse that ownership boundary rather than add another Java
composer or after-the-fact candidate filter. A compact Taiwan-language model is
the candidate design; it has not yet passed the quality/source gates.

## Acceptance

- Increase recall@8 and recall over the full bounded candidate list, plus intended
  first-choice hit rate/MRR, against the same released-policy baseline. Report
  exact, initials, mixed and incomplete input separately; no aggregate may hide a
  material condition or genre regression.
- Report recognized stored phrases separately from multi-word clause conversion.
  A dictionary-only test cannot establish sentence-conversion quality.
- Measure false automatic conversions on literal English and incomplete input,
  along with candidate stability, latency, memory and binary size.
- Reference mismatch is not proof of nonsense. Semantic precision needs an
  independently sampled, blinded review of naturalness and phonetic relevance;
  retain disagreements and abstentions. Do not label all alternative homophones
  incorrect or report assertion counts as accuracy.
- Training/source audits, development and document/conversation holdout roles
  remain separate. Existing Taiwan.md material is consumed regression evidence.
  Add natural Taiwan conversation evaluation before a release-quality claim.

## Existing solutions and data candidates

- McBopomofo is the retained Taiwan dictionary foundation; its published algorithm
  is unigram path search, so simply adopting its name does not add contextual
  grammar. https://github.com/openvanilla/McBopomofo/blob/master/algorithm.md
- Rime's Octagram interface is an existing integration option. Its distributed
  Hant model describes automatic open-web processing without sufficient Taiwan
  provenance for production approval. Do not assume Traditional means Taiwan.
  https://github.com/lotem/librime-octagram
  https://github.com/lotem/rime-octagram-data
- `taiwan-corpora/twngrams` is a new audit candidate: word 1–4-grams with host
  support counts and published pipeline/limitations. Counts are web-host spread,
  not spoken occurrence probabilities. Audit source admission, leakage, boilerplate,
  phrase boundaries, vocabulary coverage and reuse rights before any promotion.
  https://huggingface.co/datasets/taiwan-corpora/twngrams
- Chewing's curated dictionary is another source/model comparator, now maintained
  on Codeberg. Its priorities are corpus-derived; no manual priority tuning.
  https://github.com/chewing/libchewing-data

Sequence: audit/pin data → compare one source or decoder change at a time in the
desktop core → freeze the surviving design → new independent holdout → Android
latency/integration → unchanged-input screenshots. A clean image cannot substitute
for passing the joint recall/precision requirement.

## Initial data audit

Pinned twngrams revision `2931fe68b3008f87071cbaa28248162d0d596324` contains
1,033,916 rows (35,149 unigrams; 604,187 bigrams; 292,666 trigrams; 101,914
four-grams), all with at least 40-host support. 33,108 of its 35,149 word tokens
already occur in the base dictionary. This points toward combination modeling,
not simply importing another word list.

A strict source-support gate also fails the joint requirement. Of 1,324 reserved
full-input constructions, 476 exactly matched the source reference. Requiring
every word boundary to have n-gram support retains 101 outputs, of which 74 match
(73.3%, versus 36.0%), but loses 402 of the 476 correct constructions. This is
another precision/recall tradeoff, not a solution. `ngram-source/audit.json` records
all variants, including the poor initials/mixed results. No gate or dictionary
entries from this audit enter production.

The dataset's author documents residual website boilerplate and loan templates.
Its counts measure independent-host support, not occurrence probabilities, and
source-document overlap cannot be ruled out. The data remains a pilot; its CC0
notice is a publisher claim about aggregate counts, not clearance of all underlying
web content. The frozen tables, full card, notices and hashes are retained.

## Concrete decoder proposal

Use one phonetic word graph and Rime's existing contextual Grammar interface.
Known phrases enter as whole edges. A compact, smoothed Taiwan word-context model
scores combinations **while exploring alternatives**, so an unsupported early
choice does not eliminate a plausible path before ranking. Unknown transitions
back off to shorter context rather than trigger a blanket deletion. Produce a
bounded n-best list with consistent full-input consumption; use the same scoring
result for displayed order and acceptance. Incomplete phonetics add uncertainty
to the same search instead of invoking a second word-joining implementation.

First compare the maintained McBopomofo/Chewing unigram baselines with a contextual
model through that existing interface. Keep the scoring model and data isolated
from the UI. Test source data, smoothing/search and n-best width independently;
select parameters on development data only, then freeze. Do not feed expected
phrases into search, duplicate corpus n-grams as fake sentences, or claim web-host
counts directly measure spoken probability. Eliminate redundant Java construction
only after the surviving single decoder matches its useful recall.
