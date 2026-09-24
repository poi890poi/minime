# Modern-reading coverage and unsupported-reading check

September 24, 2026. Independent reference metric, fixed before decoding this
reference-derived query set. It evaluates pronunciation support, not semantic
conversation precision, frequency or a fresh holdout.

Use all single-Han records and supplied Pinyin readings from the unchanged MOE
concise archive in MOE-READING-AUDIT-PLAN.md. Remove tone accents for the Pinyin
board while preserving umlaut as v; retain every supplied heteronym. Group by
complete reading. Generate every resulting distinct query, without source-model
scores or complaint examples. Inputs and reference sets are frozen separately.
Decoder input contains only the query; expected glyph sets are used after lookup.

Compare the accepted production TSV and the corrected set-parser source-prior
TSV through the same current shared engine with Taiwan/geography packs. Record
Space agreement with the reference set, coverage of reference glyphs among the
first eight whole-input candidates, unsupported single-glyph readings among the
first eight, unclassified phrase/literal/prefix slots, and all per-query gains
and losses. A glyph absent from the reference dictionary is unclassified, not
incorrect; absence of this reading for a covered glyph is a reference mismatch,
not proof that the reading is universally invalid. More than eight legitimate
homophones can exist, so not every correct glyph can lead simultaneously.

The source archive has been structurally inspected and prior Google/source probes
overlap some queries. These are independently authored pronunciation labels but
not an untouched final holdout. Do not substitute source priorities for reference
labels, use MOE entries in production, tune weights to these results, or erase
older corpus regressions. Keep the initial set-parser-only experiment distinct
from the earlier discarded-row bug and the separate log-unit hypothesis.
