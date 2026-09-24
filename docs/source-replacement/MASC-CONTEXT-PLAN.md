# MASC original-text context pilot

September 24, 2026. Offline evaluation only, before counts or predictions. This
is a possible replacement for English EWT context counts, not production admission
or a rights clearance. The POS-tag alignment and grammatical-contraction inventory
remain separate. Reuse the authenticated publisher Git snapshot pinned in
`masc-publisher-manifest.json`; never use the unauthenticated ZIP as authority.

## Frozen extraction and roles

Use every original text in the pinned snapshot except the publisher's entire
`written/email/spam` category. This category exclusion follows intended keyboard
use, before model results; no individual document, word or complaint selection.
Retain source genre counts and exclusions. This pilot is American English, not
evidence of Taiwan Mandarin usage or present-day phone conversation coverage.

All admitted MASC documents are training candidates. Existing GUM train/dev cache
documents labelled conversation or essay are development comparison material,
already used by the Taskmaster and earlier MinIME evaluations. No GUM document
trains counts. There is no fresh final holdout in this pilot; a surviving proposal
needs one before production admission. Do not split sentences within a document
or rename reused development results as a fresh test.

Before counting, normalize tokens with the current English tokenizer, preserving
internal apostrophes and mapping curly apostrophes to ASCII. Exclude a complete
training document if its token sequence shares any consecutive 20-token span with
any evaluation document. Also deduplicate complete normalized training documents,
retaining the lexicographically first publisher path. Record every document-level
exclusion and hash. This conservative rule does not establish independence of
shorter expressions, domain conventions or shared upstream authorship.

Count each nonempty raw line independently, with the existing punctuation resets,
unigrams and one/two-word contexts. A newline can be a speaker boundary or a soft
line wrap; resetting there deliberately avoids creating cross-turn context but
may lose valid wrapped-text context. Preserve that limitation. Keep original
counts and the existing threshold of two observations for nonempty contexts;
no corpus blending, reweighting, smoothing or word-level corrections.

The publisher's separate master CSV is not an exhaustive document/speaker map:
it has 362 rows, 330 distinct title values, missing fields, and is not UTF-8.
It was authenticated against its Git blob and inspected as Windows-1252 only.
Do not infer complete provenance or turn boundaries from it. Raw corpus text
bytes remain the separately verified UTF-8 publisher files.

## Comparison and admission boundary

Reuse the unchanged shared Java ContextModel and ContextSourcePilot harness.
Compare accepted production counts, this pilot, and empty output on all eligible
next-token positions in the frozen GUM conversation/essay documents. Preserve
document, sentence and token identity; freeze complete input hashes before queries.
Report first 1/3/8 reference hits with numerators and denominators, nonempty output,
candidate slots, table rows and bytes separately by genre. A non-reference word
can still be sensible English; this is reference recall, not semantic precision.

Reject as a sole context replacement if either genre loses top-3 or top-8 hits.
Do not iterate mixtures or thresholds against these results. A pass authorizes
only further evaluation of exact/partial typing, safe apostrophe restoration,
independent holdouts and resource cost. The spelling-metadata dependency and
English underlying-text rights gate remain open until exact replacement assets
and applicable notices have been reviewed. No app build is justified by this
context-only screen alone.
