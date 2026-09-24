# Candidate page integration evidence

Shared core passes 2,008,883 contract assertions. The standalone page test uses
364 deterministic generated cases and the old eager filter as its oracle. The
actual-engine comparison covers early and late selection, Space/Enter, suffix
editing, correction undo, modes, pending/stale results and editor resets.
These totals are equivalence checks, not language accuracy. The pinned desktop
13,014-input output is unchanged, SHA-256
`72ed6f69cf0a838ec9c03a4d8de2a3e94e5991531b344ae1f173d95bff2d6bfe`.

Phone integration finishes **12 tests** in session
`c035f65d-5805-480d-a724-e91b71117f87`: paging, nine stability checks, expanded
English/Chinese choices and Taiwanese paired output. The extended paging check
rejects unsupported fixture entries, verifies bounded initial font work, pages
an old result while a new query is pending, resolves the new query, reaches the
final candidate inside the viewport, selects it, and verifies reset on new input.

Earlier attempts failed the final viewport assertion after replacement:
`676b060e-951b-45b9-a90f-c8dcfddc1049`,
`57db2ae8-ef06-4f1c-9701-5e8115960794`, and
`b568027a-4bd4-4ab3-876c-23d6985af9b5`.
The captured geometry shows a measured tail at x=100392 with width 402, content
width 100797, scroll x=93357, and viewport width 960. It was allocated but not
visible. The test's old loop stopped at allocation and assumed one subsequent
fullScroll reached the replacement's new right edge. Disabling smooth scrolling
alone did not fix it; that hypothesis is not a demonstrated cause. The corrected
programmatic reachability check scrolls within a fixed 40-attempt budget until
the real target intersects the viewport, then retains the original visibility
and acceptance assertions. No runtime change was made between these attempts.

This does not measure physical swipes, justify automatic scroll jumps, or prove
all replacement-layout behavior. Those remain separate from reachability.
All failed and passing sessions used the phone mutex, restored the original
APK/preferences/IME, and verified display OFF after environment collection.
