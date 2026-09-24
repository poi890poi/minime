# Accepted-runtime application profile

September 24, 2026. Diagnostic only, collected after rejecting the unrelated
[board-layout guard](../LAYOUT-RESULTS.md). App runtime b47bb2a (existing
query-local sort keys and 8 ms decoder delay), APK SHA-256
1c43b60e9a76e97d9917b9f13545d95dafb5c67498eaa8db42fae1e73c9ca72b.
Test APK SHA-256
4e4d1901d575bf453b6a00f3f3c5bc21a297b36e419d61a45c36022b581a0197.

ApplicationProfileTest replays prefixes from the same eight frozen development
queries with real Chinese/Taiwan/geography candidates and LocalLearning. Providers
run before the measured application callback; rendering and font filtering are
excluded. Sampling runs at 1 ms with a 32 MiB buffer. One warm-up and three
recorded applications per distinct prefix. No natural user text is profiled.

[Parsed profile](profile.json) contains 37,973 main-thread records and
5,632,656 microseconds of sampled thread-clock attribution under applyCandidates.
The five largest direct-child shares are:

| Operation | Share of sampled application time |
|---|---:|
| CandidateOrder.sort, including key preparation | 48.43% |
| conversionInput | 16.04% |
| ArrayList.removeIf | 13.98% |
| Stream anyMatch | 8.97% |
| partial | 3.87% |

Each percentage divides that direct child's attributed time by the sampled time
under applyCandidates. It is **not a share of real typing latency**, CPU use,
or vocabulary errors. Profiler overhead and this artificial repeated callback
prevent direct speed comparisons with the older pre-sort-key profile. It is not
evidence that the accepted sort-key change regressed.

The newly prioritized opportunity is conversionInput. Source inspection shows
that it calls englishCompletions(raw, afterLatin).isEmpty(). That method scans
the matching dictionary range, constructs candidates, maintains a top-24 heap
and sorts it, even though this caller needs only an existence answer. The input
decision can also be queried multiple times in one application. This is observed
control flow; the share that can be saved is not yet isolated.

Next proposed experiment: an exact completion-existence query that stops at the
first eligible dictionary entry, sharing eligibility semantics with full
completion. Compare its boolean result to the unchanged completion list over
the full source vocabulary/prefixes, case/apostrophe conditions and both context
policies before using it in conversionInput. Preserve all ranking and Space
decisions; never approximate existence with a spelling heuristic. Benchmark
locally, verify byte-identical desktop output, then repeat phone latency. Keep
this separate from sort/learning changes so any effect has one cause.

Raw [sampled trace](application.trace.gz), [per-query diagnostic costs](application-cost.tsv)
and [passing instrumentation log](instrumentation.txt) are retained. Reproduce the
parser with tools/report_application_trace.py after decompressing the trace. Input
asset SHA-256 cd50e119257521710a601f279f35c28c85b969a1f4a0ad9918011edc57344603.
This is reused development data, not a fresh language holdout or release gate.

Phone session 206afe93-1112-47c4-8977-2f7c7a5d869a held the shared mutex and
restored the original APK, preferences/learning and Samsung IME with readback;
display OFF was verified. Explicit release was sent to SHINE after all clients
finished. Seven total sessions in this window: two expected baseline assertion
failures, 12 trial integration passes, four typing replay passes and this one
profile pass. No phone work remains queued. No production, release or CI change.
