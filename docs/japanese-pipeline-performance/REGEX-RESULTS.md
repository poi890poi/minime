# Isolated regex reuse result

Fixed patterns move from per-call compilation into immutable class constants;
each lookup creates its own matcher. No regex text, flags, dictionary, scoring,
candidate budget or scheduling rule changes. No input text or matcher is retained.

Baseline 8f2003b versus regex-only APK: core 290,078 assertions passed;
13,014 pinned Rime inputs completed; 40,785 lookup metadata records and 5,120
deferred-core candidate/preferred records match. Phone native ordering matches
all 3,933 queries per session and all 192 displayed-winner acceptance results.

Phone whole-burst warm p95: baseline 50.55 ms, regex-only 48.22 ms, separate repeat
50.33 ms. Mean: 34.16 ms, 31.94 ms, 32.52 ms. Reuse reduces repeated setup but
**does not pass the repeated 50 ms pipeline gate**. This is an isolated supporting
optimization, not a complete fix. Native conversion remains test-only.

The follow-up experiment investigates Pinyin classification whose output is
discarded when Chinese is excluded. Raw timing records, APK identities, frozen
input lineage and full comparison are retained in this evaluation directory.
