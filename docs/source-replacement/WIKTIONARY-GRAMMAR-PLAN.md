# Audit Wiktionary's English contraction inventory

September 25, 2026, before downloading the inventory or evaluating it. Discovery
and isolated data evaluation only. Original Wiktionary entry text is offered under
CC BY-SA 4.0/GFDL; imported quotations can have separate terms. This audit collects
community-authored category membership/title/revision metadata, not quotations,
definitions, examples, images or prose. No production source is admitted yet.

Start at `Category:English contractions` and recursively enumerate all subcategories
using the documented categorymembers continuation protocol. Follow all child
categories, not a selected word list; detect cycles, bound category/request counts,
and fail closed on missing responses or incomplete continuation. Snapshot twice
and require identical page/category membership before treating the collection as
complete. Preserve raw responses, request URLs, retrieval times and SHA-256 pins
locally. Category revision alone does not pin its dynamic membership.

Filter titles only by the existing ASCII-letter/internal-apostrophe shape, with
case and curly-apostrophe normalization. Report every excluded shape/category
count. For an isolated metadata proposal, intersect the complete inventory with
the unchanged accepted base lexicon or twice-observed English unigrams. Preserve
the complete bare-word protection block. No additions from evaluation differences,
frequency tuning, handpicked pages, source combination or runtime-policy change.

An explicit contraction category can still contain dialectal, historical or
ambiguous entries. Membership is lexical evidence, not automatic-typing intent
or commonness. Inspect the systematic extraction and source limitations before
proposing production use. Retain attribution/source identities in the source
offering; do not collect a manually curated per-word URL list.

Run the frozen independent grammar-reference screen first, with identical sources
and genre/category boundaries. Reject source-only replacement if AUX reference
coverage falls within a source/genre. If it survives, run fresh matched-receipt
English and broad/chat Chinese comparisons, then source-derived restoration/raw
recovery checks. No English-mode change or unoffset Chinese first-eight/Space
reference loss within source/genre is acceptable. A later fresh holdout and
packaged verification remain required. Report nonverbal/ambiguous cases separately;
do not call every non-AUX annotation a false positive.

Sources checked September 25:
- https://en.wiktionary.org/wiki/Category:English_contractions
- https://en.wiktionary.org/wiki/MediaWiki:Wikimedia-copyrightwarning
- https://en.wiktionary.org/wiki/Wiktionary:Copyrights
- https://www.mediawiki.org/wiki/API:Categorymembers

This does not clear the separate EWT context-count rights dependency. Keep all raw
inventory and reference spellings local until source/distribution review is complete.
