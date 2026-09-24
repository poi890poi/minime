# Stored-prefix Android integration completed

September 24, 2026. Test/tooling change, no runtime behavior change. The original
combined replay exceeded both 240 and 600 seconds without useful per-case status.
That did not establish an app deadlock. Six fresh test sessions now partition the
same ten frozen inputs by index modulo three and ordinary/private field policy.

**All six tests pass: 20 selection/editing episodes.** Each of the ten cases
completed exactly once in each policy. Assertions still check visible input,
expanded candidate selection, committed prefix, composing suffix span and one
backspace in the suffix. No wait limits, fixture entries or expected outputs
were relaxed. Per-case phase markers now expose the location of future stalls.

Instrumentation durations are 12.833, 13.433 and 22.452 seconds for ordinary
fields; 13.206, 13.433 and 20.892 seconds for private fields. These are test
durations, not touch or candidate latency. Fresh session setup changes lifecycle
conditions, so this result does not diagnose why the previous aggregate stalled
or certify prolonged-session behavior.

The app is the completion-existence trial, now committed as 1ea175a. Fixture,
app/test hashes, all phase timings and sanitized instrumentation evidence are in
[results.json](prefix-integration/results.json). Every guarded phone session
restored the original app/preferences/IME and verified display OFF. The exclusive
reservation was explicitly released to SHINE after all clients completed.

This closes the frozen prefix-selection integration gate. It does not close
the broader quality, latency, source-rights, platform or human-touch gates.
