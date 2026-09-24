# Candidate application without tracing

September 24, 2026. The new test-only entry point passes on accepted runtime
1ea175a, with the same frozen 46 prefixes and one warm-up plus three measured
callbacks each. **138 callback measurements: median 2.20 ms, p95 10.48 ms,
maximum 14.20 ms.** p95 means 95% of those measured applications take no more
than 10.48 ms. No method tracing or per-candidate timers run in this entry point.

This workload deliberately excludes provider execution, font compatibility,
rendering, Android event dispatch and display presentation. It is not typing
latency and cannot clear any of those budgets. Candidate counts reach over
1,600 in some prefixes; the raw table preserves every prefix, round and count.
The unchanged post-callback full English-completion timer is a separate cost.

The result shows application/ranking still has a meaningful tail but is much
smaller than the complete hooked callback. It does **not** establish a speedup
over the traced profile: removing measurement overhead is not a runtime fix.
Further micro-optimizations need an unhooked frame benefit before retention.
The next useful target is avoidable repeated font-compatibility work, preserving
exact glyph support and every candidate rather than truncating the list.

[Raw measurements](application-untraced.tsv), [summary](summary.json),
[instrumentation](instrumentation.txt), [environment and binaries](environment.json).
Both environment snapshots report thermal status 0. No thermal cause is proven.
The guarded session restored original APK/preferences/IME, verified display OFF
after environment reads, and explicitly released the reservation to SHINE.
No production change; source quality and release acceptance remain open.
