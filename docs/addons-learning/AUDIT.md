# Composition continuity, traceable add-ons and phrase learning

Baseline 6ddbfe9 / 0.5.6. Separate work into independently reviewed slices.

1. Buffer defect: KeyboardView uses `preferred != 0` both to separate raw recovery
   from the candidate row and to decide whether the floating buffer exists.
   A completed decoder reply that changes the default to literal hides nonempty
   composition. Test Chinese/literal/empty replies and asynchronous pending states
   before changing visibility. Buffer existence must depend on composing state;
   candidate selection/default behavior and fixed keyboard height must not change.
2. Optional sourced Taiwan and Japanese dictionaries: prefer existing openly
   licensed data, retain upstream identity/license and entry provenance, and keep
   each add-on separate from the base model with default-off switches. No scraped
   definitions, lyrics or book content. Missing/uncertain readings must be reported,
   not invented. Japanese should use bounded exact reading lookup and explicit
   candidate selection; it must not turn Pinyin/English into a Japanese decoder.
3. Optional repeated-phrase learning: observe only MinIME-owned accepted reading/
   output segments, never arbitrary editor text. Require repeated input, bound
   storage, exclude private/secure/literal fields, clear pending chains on edits,
   field/language transitions and punctuation boundaries. Keep existing explicit
   candidate learning independent. No automatic cloud transfer. Propose portable,
   versioned, encrypted sync and conflict/deletion semantics separately.

Core tests and desktop Rime evaluations precede Android integration for candidate
or acceptance changes. Add-on-off and learning-off output must match baseline.
Use broad independent conversational regressions and source-derived coverage
separately; data membership checks are not language-quality accuracy. Keep
negative results and coverage gaps. Device verification only on RFCR91GWXLX;
restore IME/preferences and sleep/verify display after every session.

User clarification: Taiwanese uses Pe̍h-ōe-jī (POJ) throughout, not a ts/ch
substitution applied to Tâi-lô. Prefer original source POJ fields, with POJ vowels,
nasalization and tones in output, input conventions, help and export metadata.
The short Taiwanese phrase pack is separately optional and explicitly selected.

Wikipedia evaluation exposed a feature-design weakness: dropping duplicates also
dropped the add-on's visibility benefit when Rime already had the name deep in its
candidate list. Revise enabled-pack behavior to promote matching names at slot 3
(after raw and two existing choices), retaining the exact Space default and the
relative order of unpromoted candidates. Off remains baseline-identical. Test this
general source-membership rule on a second fresh Wikipedia sample; do not add or
reweight any individual missed Wikipedia title.

User correction: never handpick production entries. The earlier 38-name hiking
list, 15 POJ IDs and Japanese cultural-name seed set are rejected and removed.
SHINE AAC's constitution says “Prefer data pipelines over hand tuning.” See
https://github.com/poi890poi/shine_aac/blob/main/docs/PROJECT_CONSTITUTION.md .
MinIME AGENTS.md now makes the stronger user instruction explicit. Source filters
and upstream metadata select every production entry; structural test fixtures do
not influence those filters. Replace the article-by-article work with Rudy Map's
published POI snapshot, category hierarchy and name/pronunciation fields. Track
source version, hash, licence, rule and counts at dataset level. Keep ODbL-derived
geography separate and independently switchable. Replace the custom kana converter
with established MIT-licensed WanaKana for build-time aliases. Validate the new
source baseline before further UI changes or release.
