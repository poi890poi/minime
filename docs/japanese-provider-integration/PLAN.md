# Optional Japanese provider contract and phone admission

Baseline 66db06f. Type: optional provider integration and evaluation tooling.
Use the supported indexed/stable converter, never the rejected lexical predictor.
The native model/library is packaged only in an explicitly requested instrumentation
build; ordinary app builds retain their current dictionaries and behavior.

The shared core owns input eligibility and candidate merge. Only complete kana
derived from the existing pinned WanaKana transducer may reach conversion. Keep
attested complete lexical/character choices before conversion, conversion before
kana recovery and incomplete predictions, and preserve all fallback choices.
No reference labels, personal context or per-word rules reach the native provider.
Unavailable, malformed or failed conversion falls back. Obsolete work may finish
but must not deliver. Mode changes and disabling invalidate queued results.

Verify fake-provider contracts in core, then pinned desktop Rime regressions before
building Android. Freeze the prior 64-clause/1,311-key development timing stream
for JNI parity and phone measurements; these are consumed regressions, not fresh
quality holdouts. Compare phone and desktop ordered native outputs. Measure model
load, native query time, worker-to-main completion and memory separately; no
touch-to-display claim from a JNI timer. Run existing Android editor/keyboard tests.

Phone admission: native warm p95 <=10 ms, p99 <=20 ms; pipeline p95 <=50 ms,
no stale commits or English contamination. Record failures rather than loosening
gates. Positive ranking promotion still requires new independent quality evidence.
Reserve RFCR91GWXLX explicitly, use the shared lease, restore prior preferences
and IME, sleep and verify the display, then release explicitly. Never use tablet.

Phone follow-up: the first stream run found the same tied positions 7/8 swapped
once in each of three passes (1/1,311 unique inputs). A* insertion ties are stable,
but beam pruning still uses cost-only std::sort, whose equal-key order differs
between libstdc++ and libc++. Change only that beam sort to stable_sort. Keep the
original phone output and desktop reference; replay the identical inputs against
the new reference and audit the full consumed regression corpus for outcome changes.
The original pipeline screen measured whole-burst dispatch through delivery and
failed at p95 57.7 ms. Add cold/repeat cycles and last-key/dispatch/worker breakdowns
without erasing that failure; warm timing is a separate condition, not a changed
threshold. No ranking-quality promotion follows from these timing measurements.
