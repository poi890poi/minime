# Unchanged letter-board layout experiment

September 24, 2026. Type: performance; baseline runtime b47bb2a. Candidate
callback render p95 remains about 11 ms in the preceding diagnostic. Code
inspection finds that KeyboardView.render sets fresh layout parameters on the
entire letter-board container before checking whether its layout changed.
Android therefore receives an unnecessary child layout request on candidate
refreshes. This is a causal work hypothesis, not yet a measured UX improvement.

Trial skips that assignment when the existing width and pixel height already
match. Comparing dimensions also preserves density changes independently of
the existing layout key. Preserve all
sizes, keys, hit rectangles, candidate presentation/identity, ordering,
acceptance, pending composition, learning and 8 ms lookup coalescing. No new
cache, scheduling or language rule. Risk: a missed size transition; exercise
portrait/landscape configuration, numeric/symbol/main panels and expansion.

First verify shared core and pinned desktop. A deterministic Android check
must reproduce the unnecessary board request on baseline, stop it on trial,
and verify required dimension changes still apply. Existing paging/stability
tests protect candidate gesture and selection ownership. Compare unchanged
TouchLatencyTest unhooked replays A1/B1/B2/A2 with identical APK assets and test
APK, using the existing frozen eight development queries in four modes at
150/60 ms. Record actual cadence, raw frames, ready candidate observations and
submissions before next release, not only conditional p95. Require a repeated
benefit without missing raw input or repeated material raw-text regression;
otherwise reject or retain only as an unlanded experiment. No threshold tuning.

These reused inputs are not fresh language holdouts, physical touch measurements,
or vocabulary accuracy. Keep release gates unchanged. Phone access requires
explicit SHINE reservation plus mutex, restore APK/settings/IME and verify OFF.
Retain negative and contradictory results and commit independently.
