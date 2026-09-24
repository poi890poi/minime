# Separate queued-result waiting from main-thread work

September 25, 2026. Diagnostic follow-up after rejecting asynchronous delivery.
No runtime policy change. The accepted 8 ms delay and ordinary Handler stay.

Use a diagnostic source overlay with the already tested bounded QueueTrace
timestamps plus Android Trace markers carrying request IDs only. Mark the
post-to-main interval asynchronously and the main result callback synchronously.
Use fixed marker names and numeric IDs; no spelling, editor context or candidate
text in trace markers. Check capture is enabled and retain cancellation/closure
states, missing markers and unmatched IDs. Keep all instrumentation and any
profileable manifest overlay out of ordinary release source sets.

Record a bounded local Perfetto system trace around frozen Chinese shard 0 at
both existing cadences first. Collect CPU scheduling/wakeup, process identity,
view/graphics frame work and only MinIME app annotations. No logcat, screen
recording, network, heap or user-content capture. Retain the raw system trace
locally because it can contain unrelated process identifiers. Publish only
MinIME aggregate observations and hashes. Do not upload traces to an online UI.

Require complete request-marker coverage and inspect trace loss/overrun statistics
before attribution. Join queue intervals to the same process's main thread;
intersect non-overlapping thread-state spans to report running, runnable, sleeping,
other and unknown time. Attribute overlapping frame/callback work separately,
avoiding double-counting nested slices. Keep both per-request distributions and
duration-weighted totals. Do not sum independently computed percentiles.

Running time would identify main-thread work; runnable time identifies CPU wait.
Sleeping is not itself proof of a synchronization barrier: unresolved/native
waits must stay unresolved. Locate the dominant main-thread slices before
proposing another production change. Trace overhead prevents comparison with
untraced runs from being called a speedup or release acceptance. If the first
capture is incomplete, fix capture integrity before extending to Taiwanese and
Japanese; do not select favorable request subsets.

Use official Perfetto tools, pin version/hash and validate the local interval
aggregation with overlap, gaps, nested slices and partial boundaries. Existing
core/native equivalence remains required before Android build. Verify unchanged
assets and active production sources. Any later runtime change needs a separate
one-variable plan and unhooked evaluation.

Obtain a new explicit phone reservation. Use the shared mutex throughout trace
setup, replay, stopping the owned trace session, pulling artifacts and cleanup.
Give the trace a hard duration bound; stop only its recorded process/session,
never other profiling tasks. Restore original APK/preferences/learning/IME,
verify display OFF, explicitly release and honor the acknowledged deadline.

References: [Android trace annotations](https://perfetto.dev/docs/getting-started/atrace),
[scheduler observations](https://perfetto.dev/docs/data-sources/cpu-scheduling),
[local analysis](https://perfetto.dev/docs/analysis/trace-processor), and
[profileable release diagnostics](https://developer.android.com/guide/topics/manifest/profileable-element).
