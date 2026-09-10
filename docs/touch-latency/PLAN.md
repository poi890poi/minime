# Touch response diagnostic

Type: test/tooling. Measure the real installed IME without changing its decoder,
gestures, layout, settings defaults or production timing code. Baseline: 5bcb90f
(0.7.6). The separate POJ importer investigation must not change this baseline.

The instrumentation injects touchscreen DOWN/UP through Android into visible keys.
It records the UP event timestamp, editor text callback, editor draw/frame submission,
and fresh candidate draw/frame submission. DOWN-to-pressed-frame is separate from
release-to-text; dwell is not decoding latency. Observers run only in instrumentation.
The input sequence is frozen from the existing latency corpus before measurement.
60 ms and 150 ms key intervals exercise rapid and ordinary typing. Missing or
superseded feedback remains missing, never zero or a successful held candidate row.

`registerFrameCommitCallback` measures submission to the swap chain, **not** physical
display presentation. These are injected-event-to-submitted-frame diagnostics, not
contact-to-photon measurements or certification of section 28. They include Android
input dispatch, but exclude digitizer acquisition. The editor shares the test app's
process; Chrome's cross-process editor path remains unmeasured. A bounded sample
cannot certify the 10,000-key/mode or real-human hit-rate requirements.

Only RFCR91GWXLX is authorized. The runner saves/restores settings and learning,
restores its prior keyboard, sleeps the display in finally, and verifies cleanup.
Keep input method restoration and display cleanup active on any measurement failure.

POJ audit: distinguish source omission, importer rejection, exact retrieval and
candidate truncation. Read all existing pinned POJ sources. Preserve attested spelling
and tone; do not invent tone permutations or infer frequencies from complaint words.
