# Hold the complete glyph-decision cache at the mechanism screen

September 24, 2026. **Do not land yet; runtime reverted.** The trial preserves
font-support results, but its isolated saving is small relative to the remaining
callback tail. Keep the patch and measurements for a future sufficiently powered
comparison. This is inconclusive for typing, not a measured typing regression.

The device oracle preserves the old implementation. All 21,459 support decisions
agree across 3,023 fixed-hash source strings, Unicode/control/surrogate boundaries,
warm repeats and a 9,000-code-point eviction sweep. The 8,192-entry bound remains.
Both the parity/cost test and existing malformed-text/missing-font sentinel pass.
Core and dictionary bytes are unchanged; only classes3.dex differs in the trial.

Eight alternating-order measured batches after two warm-ups each test 4,153
strings. Old median is 2.694 ms; trial median is 2.151 ms, a **0.543 ms difference
for the entire 4,153-string batch**, not per key or per candidate. Two rounds are
slower in the trial. Full rounds, mean/max, source/binary IDs and environment are
in [mechanism evidence](glyph-cache-mechanism/summary.json).

The planned unhooked A/B typing comparison is **not run**. This is an explicit
stopping decision after the mechanism screen: the small warm-cache saving does
not account for the measured cold-font tail, and the short noisy eight-query
replay cannot establish a reliable end-to-end effect of this size. No claim
that this trial makes typing faster, and no lowering of release budgets.

The more consequential design question is why off-screen candidates undergo
eager font checks before the first candidate row can appear. Any proposal to
defer that work must prove identical visible order/default acceptance, preserve
every readable candidate on expansion, cancel stale work and avoid new flicker.
Do not silently cap vocabulary or weaken the unreadable-glyph rule.

[Frozen trial plan](GLYPH-CACHE-PLAN.md), [complete held patch](held-glyph-cache.patch).
Original APK/preferences/IME restored, display OFF verified after environment
reads, and the reservation explicitly released. Thermal status is zero before
and after this mechanism test. Accepted runtime remains 1ea175a.
