# Expanded index memory failure and fix

The first Android test build failed while constructing ReadingUnitIndex arrays:
OutOfMemoryError, 268,435,456-byte growth limit, less than 1% free after GC. Both
pair UI and timing tests failed because they shared the failed load future. The
finally guard restored the previous IME and settings/learning and verified OFF.

Cause: the TSV builder created a fresh Candidate/output/pack string for every
source alias and retained every temporary prefix/unit map while allocating compact
indexes. The full Taihoa inventory exposed this existing loader ownership issue.
It is unrelated to mode candidate scores or Android keyboard height.

Fix: share immutable candidates using all relevant source metadata (pack, output,
abbreviation and pair source), pool output/pack strings during parsing, clear
builder pools and release each temporary source map as its index is built. Keep
row order and duplicate references, so budgets and tie ordering do not change.
No vocabulary removal, increased Android heap limit or ranking changes in this fix.

Desktop all-pack retained heap: 195,214,976 -> 153,965,848 bytes. The old 0.7.3
inventory used 96,245,784 bytes. Approximate JVM heap after GC, not Android RSS.
These one-run load times (5.4 -> 6.1 seconds) do not establish a load speedup.

All 23,532 final mode outputs before/after pooling match exactly. Core: 32,328
mechanical assertions. Rebuilt APK passed both originally failing phone tests
in 113.737 seconds, including four pair variants, unchanged composition/bounds,
one-tap English return and 1,116 measured callback timings (six modes).
IME/preferences were restored and mScreenState/mActualState verified OFF again.
A post-session memory query found no process (the restoration guard stops it),
so no Android retained-memory number is claimed.
