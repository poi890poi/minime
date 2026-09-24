# Cache the complete existing code-point support decision

September 24, 2026. One-variable performance trial on accepted runtime 1ea175a.
Untraced candidate application has a 10.48 ms p95, while the separately hooked
Chinese callback includes a 15.17 ms font-policy p95. Those percentiles cannot
be added. Source inspection shows CandidateGlyphs caches hasGlyph results but
repeats Unicode validity, whitespace, format and variation-selector checks for
every code point of every candidate, even on a glyph-cache hit.

Move those same decisions into the existing bounded 8,192-code-point cache.
Do not add a string cache, increase its bound, drop candidates, move work across
threads or change any font/Unicode rule. Invalid code points cache false;
whitespace/format/selectors cache true; other code points cache the same Paint
hasGlyph result. Paint remains a private copy of the candidate font. This changes
which ignored/invalid code points occupy cache entries, so eviction behavior is
a measured risk; outputs must remain identical for unchanged font capability.

Use the old implementation unchanged as a device oracle over complete source-
derived sampled strings, Unicode/control/surrogate boundaries, warm repeats and
more than 8,192 distinct code points to force eviction. Check both support results
and the bound; no word-specific exceptions. Measure old/new whole batch cost in
alternating order with identical Paint and inputs. Confirm existing glyph and
composition/acceptance contracts. Shared core is unchanged from verified 1ea175a.

Only if parity holds and the mechanism improves, run paired unhooked A/B and
B/A typing tests with the same assets and fixture, collecting thermal/power
state before and after each. Preserve all modes and missing-frame counts.
Require repeatable end-to-end benefit without material raw-frame regression.
Reject on mixed/inconclusive results; archive the patch and revert runtime.
This diagnostic input is reused development material, not release coverage or
physical touch certification. Use acknowledged phone windows, mutex, original
state restoration and verified display OFF for every session.
