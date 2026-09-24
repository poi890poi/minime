# Profile shared lookup before choosing an optimization

Frozen September 25, 2026, after the combined phone diagnostic and before this
desktop profile. Production remains unchanged. Taiwanese provider work reaches
52.10 ms on the phone; its aggregate counters locate almost all provider time
inside add-on lookup. Code inspection suggests traversal, repeated candidate
trimming/sorting and allocation, but none is yet established as the cause.

Use the existing shared-core classes with the exact production `addon-poj.bin`.
Verify its hash against the tested APK. Preserve its paired-form metadata, pack
selection and all lookup limits. Do not rebuild from a differently ordered TSV
or turn on other languages. Load and initialize outside the recording window.

Freeze the lookup inventory before measurement: every prefix of all 224
Taiwanese queries in the existing 720-query timing corpus, retaining source,
genre and condition; plus every lowercase ASCII spelling of lengths one, two
and three (26 + 676 + 17,576 = 18,278). Keep the exhaustive structural probes
separate from corpus prefixes. Do not select by elapsed time or candidate output.
This is reused development data and synthetic structural coverage, not an
accuracy benchmark, fresh holdout or representative language distribution.

Run one initialization pass and three recorded passes of the fixed inventory
without JFR, then repeat in a separate JVM with the standard JDK 17 profile
recording enabled only around lookup passes. Keep the same heap and ordering.
Record corpus/model/source/JDK hashes, query identities, counts, timing, complete
ordered candidate fingerprints and aggregate checksums. Fingerprinting and disk
output stay outside each timed lookup. Require identical fingerprints across
passes and profiled/unprofiled runs; differences block optimization decisions.

Inspect CPU execution samples, allocation samples and GC events. Report event
counts, sampling limits and the functions owning sampled work; sample shares are
not exact wall-time decomposition. Keep raw JFR and per-query inputs local.
Publish only aggregates and hashes. Record unprofiled latency separately; HotSpot
timing is not ART or phone latency. The profiler may perturb scheduling and GC.

Choose a single optimization only after this evidence. Preserve candidate text,
order, scores, reading spans, flags and paired alternatives. No per-word cache,
promotion, new quota, reduced dictionary, score adjustment or short-input policy
is admitted by this plan. Any change to shared indexes needs broad all-language
equivalence, core and pinned desktop Rime checks before an Android build, followed
by separately acknowledged unhooked phone comparisons and memory checks.

This is local work and needs no phone ownership. Chinese font-filter internals
remain a separate investigation; the provider profile cannot explain them.
No GitHub CI, new source data, Play action or release package is involved.
