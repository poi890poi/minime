# Caps, punctuation, symbols and emoji — 2026-09-06

User-requested behavior corrections and missing features. Shift currently toggles
one-shot state on every tap; only a hold locks capitals. Add timed double-tap lock,
one-tap unlock, a visible locked state, and tests for slow taps and intervening keys.
Keep upward capital slides and editor/lifecycle resets.

Punctuation currently uses the same input path as Latin technical tokens. Bottom
comma/period can remain composing until another acceptance key. Observe legacy
Pinyin taps, slides, holds, Latin/Chinese composition and symbol pages before
choosing explicit punctuation commit and width rules. Preserve URLs/code, raw
recovery, literal Space, and secure-field policy. New emoji insertion must commit
each entire Unicode sequence in one operation, without splitting variation
selectors, skin tones or ZWJ sequences. Avoid recents persistence in private fields.

The user now requests emoji, overriding the original initial-version non-goal.
Add a discoverable picker and a broader categorized symbol inventory using Unicode
characters and system rendering, with return-to-letters and Backspace. No Google
code, visual assets, or proprietary data will be included. Validate real phone
touches, composition boundaries, category navigation, deletion and device cleanup.

Additional explicit request: a single-tap Chinese/English switch. This overrides
the original persistent-mode avoidance. Add an always-visible EN/中 key in normal
text fields. Accept owned composition before switching; English mode uses literal
input and ASCII punctuation, and Chinese returns to the chosen Pinyin/Zhuyin
layout. Preserve the preference across fields without overriding URL/password/
numeric editor restrictions. Switching back resets punctuation to the mode default.

Verification exposed test lifecycle failures: instrumentation restarts its target
package and Samsung can revert to the prior IME during startup. Select the test
keyboard after the activity and dictionary are ready. Picker runs also returned
unexpectedly to letters. Native-popup focus/input restart was initially suspected,
but a later screenshot proved the coordinate-based test could hit Home while the
keyboard resized. The exact cause of each earlier popup failure was not isolated;
do not attribute them all to product lifecycle behavior. The test now waits after
holds and activates clickable choices directly. Categories and punctuation remain
inline as the chosen UI for these new features, avoiding extra windows entirely.

After all fifteen keyboard checks passed, the older direct-editor deletion test
intermittently asserted before the dispatched key returned. It now waits for
window focus before dispatch and observes the expected text within a bounded
three-second interval. AndroidEditor deletion code is unchanged.

The runner now saves preferences outside the instrumented process, bounds the run
to 180 seconds, and restores preferences, previous IME and display sleep in cleanup.
This protects the user's settings even if an instrumented test hangs or crashes.
