# Excluded-language intent regression

Cause: query scoping disabled Chinese decoding in English-secondary modes, but
IntentClassifier could still label a Latin spelling CHINESE_PHONETIC. The English
completion gate then withheld valid English prefixes, despite Chinese being absent.

Only that inferred hint is discarded when Chinese is excluded and input is not
Bopomofo. Technical/URL/number and explicit field protections remain. Focused
candidate order and dictionary data do not change. The synthetic can/candy test
fails against pre-fix classes with the expected assertion and passes afterward.
Core: 32,338 assertions. The 3,922-condition six-mode audit preserves all Chinese,
English and focused-language coverage; English half-word reach improves. The exact
release then passes fixed-mode phone UI and six-mode callback measurements.
