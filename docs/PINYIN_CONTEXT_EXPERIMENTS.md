# Pinyin context experiments

Only UD Chinese GSD training data feeds the packaged context model. Evaluation
labels never enter runtime assets. The corpus is encyclopedic, so its relevance
to conversational typing is limited.

Broadly adding character context likelihood regressed the established 不對 case
to 部隊. Restricting it to assembled phrases passed core contracts but regressed
fresh phrase probes from 16/24 to 14/24 first-choice hits (top five stayed 17/24).
Both approaches were rejected. The `*-rejected.tsv` files retain their outputs.

The accepted variant adds only the difference between contextual and context-free
character scores at a word boundary, weighted by 0.5. Thus dictionary word priors
remain intact and empty-context results are unchanged. The boundary holdout uses
400 dictionary-covered word tokens from the beginning of the independently held
out UD Chinese GSD test split, with preceding three Han characters and the highest
frequency dictionary reading. This is a full-reading, dictionary-covered word
test; it does not measure arbitrary conversational sentence or abbreviation quality.

Before: 322/400 top one, 390/400 top five. After: 332/400 top one,
391/400 top five. Desktop median query time was 1.37 vs 1.38 ms; p95 was
4.84 vs 5.82 ms. This modest aggregate improvement does not imply every context
improves or that ranking matches Google Zhuyin. See the adjacent before/after TSVs.

Core verification: 1,057 assertions pass, including unchanged mixed-language,
technical, privacy, abbreviated acceptance and explicit literal recovery contracts.
Android integration and final paired comparisons remain separate gates.
