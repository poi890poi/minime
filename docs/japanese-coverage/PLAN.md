# Japanese basic vocabulary and characters

The user reports common Japanese phrases missing as well as single kana and
common kanji. Baseline assets are 0.7.2; ranking-only behavior is `fb85a39`.

Diagnosis: `append_everyday` imports only JMdict senses tagged expression or
interjection. Its pinned common-only source has 22,637 records and 22,972 common
kana forms, but ordinary nouns, verbs and adjectives never pass that filter.
Expand to all source-common readings and compatible source-common spellings.
Retain reading and sense applicability restrictions; skip unsupported aliases.
No handpicked words, fabricated phrases, inflections or case-specific promotions.
This fills lexical coverage, not Japanese sentence conversion.

Existing original spelling outputs, POJ and Taiwan entries stay byte-for-byte
equivalent as sets. Reuse the pinned JMdict/WanaKana sources, retaining complete
source provenance. Source-common markers are imperfect and not modern spoken
frequency. KANJIDIC's top-500 rank rule and kana extraction are documented in
`../mode-priority/JAPANESE-PLAN.md`; keep that extension isolated in its own asset.

Freeze complete/prefix/initial query conditions for all eligible common source
readings before generated candidates are evaluated. Record complete inventory,
retrieval limits and source exclusions. This is source-derived retrieval, not
independent language accuracy. Reuse the separately frozen English conversation,
English/Chinese essay and Japanese/Taiwanese retrieval mode corpus to measure
collateral changes. Test current core and pinned desktop Rime before Android.
Measure runtime lookup/application and phone worker latency with distinct bounds.

New vocabulary can worsen ambiguity, first-row coverage and startup memory.
Report those losses, preserve source/native baselines and gate all new candidates
behind the existing Japanese pack/mode. Missing sentence-level Japanese
conversation/essay references remain an explicit limitation.

The initial shared eight-result import was rejected: existing Japanese expression
half-prefix availability fell from 97 to 40/128 as ordinary vocabulary took the
same retrieval budget. Isolate source-category `everyday_vocabulary` in its own
eight-result index. Preserve the original eight expression/name/culture results;
merge complete matches before incomplete matches. The separate character source
adds at most eight, so Japanese results remain bounded at 24 before deduplication.
This is a source-category growth rule, not a phrase/ID promotion or fitted weight.
Keep the rejected raw outputs and re-evaluate all original conditions.
