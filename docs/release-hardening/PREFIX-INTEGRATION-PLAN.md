# Finish the stored-prefix phone gate

September 24, 2026. Test tooling only. The existing complete ordinary/private
stored-prefix replay exceeds both 240- and 600-second sessions. Screenshots
show progress; this is not proof of an app deadlock. Each prefix uses repeated
accessibility traversal and UI-idle waits, so a silent aggregate test provides
poor evidence about the slow operation or whether every case completed.

Split the same frozen fixture by index modulo three, separately for ordinary
and no-personalized-learning fields. Six independently runnable methods cover
every existing case exactly once per field policy. Keep all input, hardware
apostrophe, visible expanded selection, composing-span and suffix-deletion
assertions unchanged. Emit case index/phase/elapsed progress using only fixture
text. No runtime, vocabulary, wait limit or pass criterion change.

Run each shard with the guarded device runner; preserve failures and report the
union of completed case IDs. A pass on one shard does not complete the gate.
If a particular phase stalls, investigate that phase rather than silently
reducing coverage or removing the assertion.
