# Prebuilt optional indexes

The split TSV loader was insufficient: the phone's first measured POJ load took
17,580 ms (Taiwan 3,605 ms, Japanese 3,131 ms). Warm reuse was 0.06–0.34 ms.
This negative result is preserved in the verification evidence. It is cold
initialization, not a per-key lookup measurement, but is still unacceptable for
switching to a focused language for the first time.

Reuse the base model's existing `BinaryModel`, `ReadingIndex` and
`ReadingUnitIndex` serialization. Build the exact same optional indexes before
packaging. A separately selected supplemental format retains pack, abbreviation,
and Han/POJ pair metadata; the base model's default format remains unchanged.
Source TSVs remain packaged for attribution/source inspection. No source rows,
duplicate identities, priorities, limits or candidate scores change.

| Pack | TSV cold ms, two trials | Prebuilt cold ms, two trials | TSV retained MiB | Prebuilt retained MiB |
| --- | --- | --- | --- | --- |
| POJ | 4,287 / 4,250 | 454 / 392 | 49.08 | 42.56 / 42.47 |
| Japanese | 1,156 / 1,248 | 328 / 335 | 9.67 | 8.95 / 8.99 |
| Taiwan | 1,173 / 1,181 | 221 / 228 | 9.62 | 8.87 |

These are fresh Java 17 desktop JVMs, with reversed trial order, using the same
corpus and protocol as `README.md`. Every one of 23,532 paired lookups preserves
candidate order, score, completeness, abbreviation, character status and paired
metadata. `prebuilt-results.json` and per-row signatures retain the evidence.
The same genre/data-role limitations apply: this demonstrates representation
parity, not language accuracy or expanded vocabulary coverage.

Core regressions also check deterministic binary round trips, duplicate-budget
preservation, full/prefix/initial/mixed queries, paired identity, and rejection of
bad headers, truncation and trailing bytes. Core suite: 35,052 assertions.
The pinned desktop Rime evaluator completes 628 queries (IPC-inclusive p50
5.02 ms, p95 9.83 ms, max 27.40 ms). Base packaged-model hashes remain checked.

The three prebuilt files total 24,285,599 uncompressed bytes. APK storage growth
and the final phone cold-load measurements are recorded in release verification.
This is a deliberate storage/initialization tradeoff; lookup budgets and the
base Chinese/English model ownership are unchanged. Geography retains its
existing separate loader.

The same phone loader test on the prebuilt APK measured Taiwan **362.6 ms**,
Japanese **251.6 ms**, POJ **461.5 ms** cold, and **0.09–0.18 ms** warm cache
access. These are individual loader measurements, not distributions of physical
mode-switch latency. Twenty Android integration checks passed in 44.502 s,
including scoped loading/eviction, persistence, asynchronous delivery, stable
candidate identities and real Japanese/paired POJ interactions. Prior keyboard
and preferences were restored and display OFF verified.

The APK grew from 69,914,858 to 76,936,211 bytes: 7,021,353 bytes (6.70 MiB)
for the prebuilt indexes and loader changes. Retaining the source TSVs supports
in-app source inspection. The measured cold-load and memory improvements justify
this storage tradeoff; no typing-latency PASS is inferred from it.
