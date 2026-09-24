# Keep the composition buffer in the IME window

Frozen September 25, 2026, after the popup ablation and before implementation.
Type: performance change with Android view/lifecycle impact.

Cause supported by one-variable diagnostic: suppressing only the floating buffer
eliminates all 729 window relayouts during the frozen Chinese replay. Mean queued
delivery wait falls from 8.996 to 1.408 ms, with complete 665-request accounting.
Removal is not shippable because users need the visible, tappable raw spelling.

Replace the buffer's PopupWindow with a 24 dp transparent host immediately above
the unchanged keyboard inside one input-view container. Retain its wrap-content
background, font, text, ellipsis, accessibility label and raw acceptance action.
The host stays the same height while only the inner annotation's visibility and
width change. Use public onComputeInsets: content/visible boundaries remain the
keyboard's top, and touchable region contains the keyboard plus the visible
annotation rectangle. Blank pixels beside/above the buffer must pass through to
the editor. No extra preference, decoder change, font change, dictionary or new
floating-window mechanism. Preserve the existing keyboard body measurements.

Android supports independent content/visible/touchable insets; changing content
insets can resize the app. See the [Insets contract](https://developer.android.com/reference/android/inputmethodservice/InputMethodService.Insets)
and [service API](https://developer.android.com/reference/android/inputmethodservice/InputMethodService#onComputeInsets(android.inputmethodservice.InputMethodService.Insets)).
This design keeps the reported content boundary stable. The wrapper's actual
platform behavior still needs device verification; documentation alone is not a
pass. Keep the trial in an isolated source overlay until admitted.

Before timing, require real visible-key tests for raw-tap acceptance, unchanged
keyboard and editor geometry across buffer growth/clear/panel switches, blank
host touch-through, hidden-IME/private-field cleanup, and rotation/navigation.
Exercise existing candidate-stability contracts and four-mode smoke. Verify
ordinary release assets, no diagnostic hooks, and core/native equivalence.

Screen uninstrumented A-B-B-A on the existing four-mode workload, with the same
test APK, cool starts, settings and cadence. Candidate gains must repeat. Reject
repeated raw/Space p95/p99 regression over 2 ms in any mode/cadence, any missing
raw/Space frame, or any correctness/geometry/touch-through failure. Report tiny
sample tails as screening results, not population estimates. If passed, extend
the frozen language shards and prescribed large sample; no release gate waived.
Do not alter rejection thresholds after looking at results.

Compatibility risks: configuration recreation, IME hide/show, system navigation
insets, transparent window background, editor resize/pan and accessibility window
ownership. Explicitly check the phone's Android integration, then Chrome/Keep;
Android 16/16 KB and human touch gates remain separate.
