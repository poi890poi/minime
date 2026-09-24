# Fresh-document grammar holdout, before seeing outputs

September 25, 2026. The proposed productive grammar rule is frozen in
PRODUCTIVE-GRAMMAR-PLAN.md. Repository records prove **all three older GUM splits
have already been used**, so none can be relabeled fresh. A read-only upstream
revision check returned `1fe635509c649e376dfb449d528424ab78f4eaee`; the prior pin is
`34d01cb603867d0c085896e8286a0fe01229fa37`.

Acquire all three GUM files from that immutable newer revision into the local
audit cache. Record original byte hashes and URLs; do not publish source prose.
Before extracting grammar references, exclude every document ID found in any
older GUM split. Also exclude a whole new document if it shares a normalized
20-token sequence with any old GUM split, EWT train/dev/test, or the MASC publisher
texts already audited. Normalization is lowercase, curly-to-straight apostrophes,
then Unicode word tokens with internal apostrophes; no candidate-dependent filter.
Exact overlap is conservative and does not prove author/speaker independence.

Freeze every remaining document, with source genre and exclusion reasons, before
querying old/category/productive flags. Do not select for contraction presence,
word identity or favorable results. Use the unchanged multiword-expansion parser
only after the proposed method and document manifest are pinned. If there are no
eligible new documents or insufficient examples in a genre/category, report the
gap; do not substitute changed annotations of an old document as a fresh holdout.
Older inspected comparisons remain development diagnostics.

Report all narrow grammar categories separately. This corpus provides annotated
English grammar, not mixed-language typing intent or population frequency. It is
evaluation-only; not one word, flag, weight or frequency may enter production from
it. Verify the publisher's license/attribution records before any redistribution.
Raw text, document identity lists and per-surface results remain local; publish
aggregate counts/pins only. This does not clear corpus-count distribution rights.
