# Precompiled dictionary experiment

The build freezes the same attributed TSV data into a versioned data-only model.
Reading indexes, word lists, and string references load directly. Runtime ranking
and probabilities do not change. `CompileModel` checks query/score parity and
records input and output SHA-256 hashes. Both TSV and binary loads pass the full
1,133-assertion core suite. TSV sources remain in the repository and test APK;
the production APK contains the compiled model, notices and Unicode emoji catalog.

Samsung SM-G781B, Android 13, September 7, 2026, one paired instrumentation run:

| Loader | Load | Approximate retained heap |
|---|---:|---:|
| Binary | 2,246 ms | 104,734,032 bytes |
| TSV, including context | 7,377 ms | 135,017,968 bytes |

Binary ran first, TSV second in the same process. JIT, allocator and GC state can
affect these numbers. Query timings in the adjacent raw reports are not a clean
algorithm comparison: the format is identical semantically and the second run
benefits from warmed conversion code. These measurements exclude input dispatch
and keyboard rendering. They do not establish end-to-end latency.

The cost is storage: the benchmark model was 48,780,946 bytes and the final model
is 48,782,028 bytes after adding source-derived contractions. The debug APK
is approximately 18.5 MB, compared with the reviewed prototype's 3.74 MB APK.
The startup/heap benefit is accepted for this iteration; a more compact binary
format remains possible. No Java object deserialization or downloaded model is used.

Chinese lookup now runs on a coalescing worker. Revision checks reject outdated
results and commit barriers preserve ordering when Space precedes queued typing.
This removes expensive conversion from the main input thread; it does not make
an individual ambiguous query instantaneous.
