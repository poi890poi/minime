# KALQ decision

KALQ was an omission from the first four-layout screen. It is now tested with the
same dictionary, assumed contact distributions and decoder policies. Retain both
adaptations for further evaluation; neither becomes a shipping layout from this
experiment alone.

At 60 mm width and 1.5 mm scatter, the equal-height adaptation produces fewer wrong
literal words in both conversation and essay samples. With forced word decoding,
its conversation result ties QWERTY; the essay result is slightly better. Under
2.5 mm scatter, however, its forced-decoder result is worse in both genres. The
40 mm adaptation performs better in the reported corrected-word conditions, but
spends an extra 10 mm of letter-region height. These are tradeoffs, not an overall
human-speed ranking.

Every reported error percentage in RESULTS.md is **incorrect words / word attempts
× 100**. Conversation denominators are 4,101 attempts (1,367 occurrences × three
noise seeds); essay denominators are 3,201. The same occurrences are reused across
conditions. Counts and denominators accompany the primary percentages.

The next useful comparison must include KALQ's internal Space keys and actual
segmentation, with equal total keyboard height including controls. Otherwise its
empty space-key regions are unrealistically absorbed into nearest-letter targets.
Then test learning, physical thumb reach, and correction-inclusive task time with
people. The present experiment cannot measure those properties.

Verification: 90 conditions completed; 5,400 aggregate rows reconcile with raw
document counts. QWERTY reproduces every previous per-genre result exactly. Original
harness, corpus, dictionary and protocol hashes match. No Android code, dictionary,
preferences, or phone state changed. Local source-registry and whitespace checks
cover the evidence commit. No GitHub Actions were used.

See [results with denominators](RESULTS.md), [geometry diagrams](index.html), and
the [frozen extension protocol](PLAN.md).
