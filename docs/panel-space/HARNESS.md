# Comparative editor ownership

The first panel study failed when an editor lost focus during IME switching.
Its ownership assertion ran on Android's main thread, so it killed the process
instead of reaching the case-level handler that records unavailable observations.
The retained baseline/instrumentation.txt records this harness failure.

The harness now samples attachment, window focus, editor focus and active input
ownership on the main thread, allowing at most 1.5 seconds for switching to settle.
It asserts on the instrumentation thread. No ownership gate is removed.

The corrected isolated plan returns to letters before holding comma for emoji.
Google has no direct emoji action in its symbols panel. Both providers' Pinyin
and English flows completed (four observed records) in 32.331 seconds. A second
paired run completed in 32.890 seconds. These are observations, not a parity score.
The earlier incomplete cases remain negative evidence.
