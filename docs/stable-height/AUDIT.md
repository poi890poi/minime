# Stable IME height

Type: layout bug fix and panel sizing policy. Baseline a9fa9da / 0.5.1.
Cause: WRAP_CONTENT root sums a GONE/VISIBLE 24 dp phonetic row and variable
status text, plus independently sized keyboard/palette contents. Candidate intent
changes therefore resize the IME and the editor above it; switching palettes,
privacy or layouts produces further jumps.

Contract: a fixed 24 dp header, 48 dp candidate strip, and four QWERTY row-heights
(59 dp portrait, 34 dp landscape). System navigation insets remain additive.
Use the existing Chinese-composition QWERTY height as the stable budget. Reserve
the header at idle; show raw spelling or status there. Keep QWERTY and bottom-row
geometry stable. Fit four-row layouts and expanded candidates inside the same
body. Emoji/symbol palettes use two rows with existing paging to fit usable keys;
all entries remain reachable. No dictionary, candidate ordering, acceptance,
settings schema or gesture recognition changes. Orientation can change the budget;
ordinary typing and panel transitions cannot.

Verification: reproduce the old behavior with actual Android view measurement;
check full/partial composition, confirmation, expansion, language/layout/palette,
private/loading states, portrait/landscape, larger font and navigation inset.
Exercise visible IME/editor bounds and symbol/emoji selection on the authorized
phone. Preserve selection/scroll/slide/caps behavior and verify no panel is clipped.
Restore phone preferences and original IME, sleep display, verify Dozing.

Baseline Android assertion: composing ni expected height 852 px, actual 924 px
(density 3, 24 dp difference). The first harness threw the assertion on the main
thread, terminating instrumentation; it now propagates failures to the test thread.
Phone restoration completed and Dozing was verified after that failure.

The all-configuration measurement passes after fitting palette selectors/navigation
and disabling baseline alignment between differently styled buttons. The first
real-window test incorrectly required Space width to remain equal on expansion;
the existing expanded layout adds Delete and changes horizontal widths. The test
now asserts its vertical bounds, retaining the existing horizontal control policy.

The focused run found two test assumptions. The emoji helper stopped at 16 pages;
with 12 instead of 24 entries/page it stopped before Taiwan. It now traverses
until Next is disabled (with a catalog-sized safety bound), and selection passes.
Raw-row bounds were read before waiting for the asynchronous phrase candidate:
telemetry captured the prior literal-strip node, while the screenshot showed raw
correctly above candidates. Wait for that phrase first, then measure raw; retain
the same raw-above-strip assertion without adding a timing delay.
