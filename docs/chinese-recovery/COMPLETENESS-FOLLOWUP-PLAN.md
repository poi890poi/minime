# Preserve alternatives while preferring one complete match

September 25, 2026. The original all-complete-before-incomplete experiment is
rejected, before building an Android payload. Its generated tests and core suite
pass, but conversation first-eight useful slots fall 4,852 -> 4,762 and essay
whole-target coverage falls 1,249 -> 1,238. English mode is identical. All
candidate identities/spans remain, so this is displacement, not missing data.

Diagnosis: 96 conversation losses are useful partially consumed phrases,
displaced by fully spelled single-glyph prefixes. Five such phrase gains and
one net whole-target gain do not compensate. Grouping every fully typed path
ahead of every completion gives even low-frequency homophones that advantage.
Complete-reading support improves 313 -> 337 of 405 Space choices, but that
reference check does not establish that every full homophone is useful.

Freeze a smaller intervention before editing: select the highest-score,
whole-input, fully typed Pinyin path as one preferred alternative. Give only
that identity priority over completion frequency; preserve all other score
ordering and all partial-input ordering. Explicit learned choices remain ahead
of this preference. Preserve a full path when the same text also has an
incomplete path, without favoring all full homophones as a class. This adds no
score, cutoff, special spelling, or new candidate. Native candidates have
unknown completeness and retain their own order; do not claim them to be exact.

Reuse the original frozen data, admission gates and exclusions. This is a
development follow-up prompted by observed displacement, not an independent
holdout. Keep the rejected patch and aggregate evidence reproducible. Reject
again if genre-level whole/compatible first-eight coverage regresses; do not
tune against individual losses. Only a surviving rule proceeds to native,
performance and actual phone evaluation.
