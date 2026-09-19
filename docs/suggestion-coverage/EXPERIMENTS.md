# Candidate-capacity experiment

Development experiment, declared after baseline inspection and before variants:
compare candidate capacities 64 and 128 against 24, retaining the identical 2048
search-state budget, score model and traversal. This tests deeper scrolling access,
not a new ranking model. Require no previously reachable target loss; inspect all
first-eight changes and compare uncached lookup latency before choosing a limit.
Reserved families remain uninspected until the implementation choice is fixed.

## Stored phrase-prefix selection

The 18 frozen phone cases exposed a behavior gap: the reference offers stored
phrase prefixes alongside glyphs while an unmatched longer input is composing.
Reuse the existing source-syllable traversal; retain two source words per reached
terminal and at most eight phrase prefixes, with the unchanged 2,048-state budget.
No entries are concatenated. A prefix must preserve its consumed span and requires
explicit selection; it cannot become the Space default. There is no preference,
user-data, or binary-format migration. The packaged model must be regenerated.

Rejected first variant: insertion among the leading candidates reduced whole
target first-eight access from 11,531 to 11,337 of 24,244 labeled episodes. It
improved prefix access but needlessly displaced full matches. Preserve six whole
choices plus the established two glyph positions when they already fill the page.
Retain the rejected comparison in pipeline-unprotected-results.json.

The independent validator then found a source-boundary collision: b'b returned
the two-syllable stored word 弊案 after consuming only b'. The compact alias bian
shared its candidate list with the one-syllable bian node. Construct the syllable
trie from original source readings; retain compact aliases only in the exact
lookup map. The synthetic xian / xi'an collision regression checks both loaders,
invalid one-unit spans, valid mixed spans, and unchanged exact compact lookup.
No word-specific exception or frequency change is involved.

Regression findings fixed before acceptance: normal/private paths must apply the
same static score ordering; technical strings must not gain phrase-prefix spans;
the first two glyph positions must survive phrase-preview insertion. First-glyph
tests now explicitly select glyphs; the new phrase-prefix tests cover multi-glyph
selection. Add-on relative-order checks retain all candidates and separately
compare glyphs and phrases because their access positions intentionally differ.

Final acceptance uses the same frozen retrieval matrix and the 31,527-episode
composition corpus, independent source-span validation, original Space behavior,
no lost targets, and existing cross-language contracts. These repeated datasets
are regression evidence, not a new holdout for linguistic quality.
