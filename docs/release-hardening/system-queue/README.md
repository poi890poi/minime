# Local scheduler diagnostic

These files are tooling and evidence, outside ordinary Android source sets.
`Trace.java` is a desktop fake for marker-balance testing, never an Android class.
Production remains the accepted ordinary Handler implementation.

`prepare.py` extends the earlier queue overlay under `artifacts/queue-stages`
with ID-only Android trace sections and a diagnostic profileable manifest.
It refuses to overwrite an existing overlay. Core/native verification precedes
the diagnostic Gradle build; use its generated `diagnostic.init.gradle` only for
this experiment. Verify assets and manifest/code differences, and sign with the
existing phone's test key. Do not distribute this fixture or use upload credentials.

Run `tools/test_system_queue_intervals.py` and `tools/test_system_queue_report.py`
through unittest discovery. Compile the desktop fake, `TraceContract.java`, the
generated QueueTrace and the earlier QueueTraceContract to check observer balance.
`ContextContract.ps1` checks deadline/ownership capture without touching the phone.

For phone capture, obtain an explicit task acknowledgement and hold
`tools/phone-lease.ps1` through all operations and cleanup. Start Perfetto only
after APK installation with `tools/test-release-device.ps1 -BeforeInstrumentation`.
The optional hook runs inside the existing restoration scope: an exception still
restores the original package/preferences/learning/IME. Trace process cleanup
belongs to the outer lease owner. Capture config travels through stdin because
the on-device tracer cannot directly read the shell's /data/local/tmp config.

The current `capture.pbtxt` streams ring buffers to a bounded file and flushes
producers periodically. Preserve its limits and use a unique owned output path;
verify storage headroom, stop only the recorded trace process, pull the trace,
remove owned remote files, verify display OFF and explicitly release the phone.
Earlier discarded-buffer configurations remain as rejected capture evidence.
See [Perfetto buffer guidance](https://perfetto.dev/docs/concepts/buffers).

The local official analyzer is Perfetto v58.2-add693d8b. `binaries.json` pins
its published Windows SHA-256 along with the app/test artifacts. Do not upload
raw traces to a web viewer. Export scoped rows with `export.sql` using its `query`
subcommand, then run `tools/report_system_queue.py` against that CSV and the
same session's candidate-queues.tsv. The reporter refuses attribution on missing,
duplicate, reversed or incomplete markers, trace loss/config errors, or ambiguous
main-thread identity. State intervals must be disjoint; gaps remain unknown.

Export `frame-detail.sql` separately and use `tools/report_system_queue_work.py`
with the complete integrity JSON, scoped CSV and detail CSV. Its tested sweep
attributes each instant exclusively to the deepest slice. It rejects ambiguous
overlaps and incomplete spans within measurement; a final open state after the
last queued request is outside the measured interval. `work.json` retains the
checked aggregate. `capture-session.ps1` archives the successful baseline runner;
its output directory already exists locally and it refuses to overwrite it.

Shares divide summed per-request state duration by summed queued-request waiting
duration. They are not typing accuracy, a fraction of all app runtime, or a release
latency result. Frame overlap is wall time and can include sleeping/blocking;
it must not be added to the disjoint state shares. Sleeping alone cannot identify
a synchronization barrier. The editor and IME share a process in this fixture.
