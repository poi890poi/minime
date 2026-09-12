# Improve recall and precision together

September 12, 2026. Corrected decision: **no automatic construction is the null
baseline**. Existing construction has not earned acceptance merely by being in
the app. The previous rejection of the null baseline was unjustified: full-spelling
recall@8 of 674/1530 with construction versus 244/1530 without it measures a
coverage contribution, not net typing benefit or semantic precision. Retain these
measurements; withdraw the inference that construction must therefore remain.
The experimental patch is preserved in `source-only/implementation.patch`.
Production is still at the restored pre-experiment behavior; this correction
changes the acceptance contract, not runtime defaults. No updated release or
screenshot was published.

The frozen corpus contains 3–16-glyph contiguous prose spans, not exclusively
dictionary words. Many lost references are legitimate multi-word clauses.
Dictionary expansion alone cannot cover all such clauses. However, users can
enter clauses through successive stored-word selections. A whole-clause hit metric
does not measure that path or its cost. Contextual construction is a hypothesis
to test against that working baseline, not an established missing requirement.

Rime already offers a Grammar interface in Poet. Without a grammar component it
uses entry weights and a constant boundary penalty; with one it scores transitions
during beam search. Reuse that ownership boundary rather than add another Java
composer or after-the-fact candidate filter. A compact Taiwan-language model is
the candidate design; it has not yet passed the quality/source gates.

## Null baseline and comparison

The null disables automatic joining of separate entries in native Rime and Java.
It retains attributed stored-word/phrase lookup, full/initial/mixed/partial
phonetics, explicit incremental selection, literal input and user-confirmed
learning. Null does not mean an empty dictionary or an unusable keyboard.

Freeze this baseline with the same sources, lookup settings, UI and evaluation
inputs. Compare existing unigram construction and proposed contextual construction
as separate additions. Include the released build as a historical comparator,
not the acceptance authority. The prior source-only patch also changed ranking
and policy, so its aggregate result is not a clean one-variable estimate of
construction's contribution; rerun a controlled null comparison before deciding.

The burden of evidence is on each addition. If benefit is absent, inconclusive,
or does not justify its errors and resource cost, retain the null. Do not require
the null to preserve capabilities introduced by the feature under evaluation.

## Acceptance

- Increase recall@8 and recall over the full bounded candidate list, plus intended
  first-choice hit rate/MRR, against the frozen null baseline. Report
  exact, initials, mixed and incomplete input separately; no aggregate may hide a
  material condition or genre regression.
- Report recognized stored phrases separately from multi-word clause conversion.
  A dictionary-only test cannot establish sentence-conversion quality.
- Measure false automatic conversions on literal English and incomplete input,
  along with candidate stability, latency, memory and binary size.
- Measure completed-text success and input effort: keystrokes, selections,
  corrections and task time, including incremental word entry in the null.
  Whole-clause recall gains alone do not justify enabling construction. Predeclare
  meaningful quality gains and acceptable cost/error margins on development data,
  freeze them before holdout evaluation, and report paired uncertainty.
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

A strict source-support gate has not established benefit over null. Of 1,324 reserved
full-input constructions, 476 exactly matched the source reference. Requiring
every word boundary to have n-gram support retains 101 outputs, of which 74 match
(73.3%, versus 36.0%), but loses 402 of the 476 correct constructions. This is
another precision/recall tradeoff relative to existing construction, not evidence
that either construction or filtering deserves inclusion. `ngram-source/audit.json` records
all variants, including the poor initials/mixed results. No gate or dictionary
entries from this audit enter production.

The dataset's author documents residual website boilerplate and loan templates.
Its counts measure independent-host support, not occurrence probabilities, and
source-document overlap cannot be ruled out. The data remains a pilot; its CC0
notice is a publisher claim about aggregate counts, not clearance of all underlying
web content. The frozen tables, full card, notices and hashes are retained.

## Optional decoder experiment

If tested, use one phonetic word graph and Rime's existing contextual Grammar interface.
Known phrases enter as whole edges. A compact, smoothed Taiwan word-context model
scores combinations **while exploring alternatives**, so an unsupported early
choice does not eliminate a plausible path before ranking. Unknown transitions
back off to shorter context rather than trigger a blanket deletion. Produce a
bounded n-best list with consistent full-input consumption; use the same scoring
result for displayed order and acceptance. Incomplete phonetics add uncertainty
to the same search instead of invoking a second word-joining implementation.

First establish the null, then compare maintained unigram construction and a
contextual model as separate additions through that interface. Keep the model and data isolated
from the UI. Test source data, smoothing/search and n-best width independently;
select parameters on development data only, then freeze. Do not feed expected
phrases into search, duplicate corpus n-grams as fake sentences, or claim web-host
counts directly measure spoken probability. No construction implementation,
including the existing Java paths, is entitled to retention before demonstrating
benefit over null. Prefer the smallest addition that passes the joint quality,
typing-effort and resource gates; adding no construction remains a valid outcome.

## Controlled null results

The [controlled null experiment](../null-construction/README.md) now isolates
generation from ranking. Java construction adds deep-list coverage but no
first-eight or incremental-selection advantage in these replays, with a substantial
core processing cost. Native construction adds selectable clauses but has not
established net benefit across conditions. Neither earns default inclusion from
these results. Null stays the experimental default; production remains unchanged.
Google Zhuyin phone comparisons are retained as reference behavior, alongside
independent source targets and explicit timing/genre limitations.
