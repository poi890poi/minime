# Palette category navigation

Intentional UI change. Visible, scrollable tabs expose sibling categories and
highlight the active category in teal. The all-category chooser remains available;
emoji subgroup choices retain their place in the hierarchy. Canceling either
chooser restores the content page. Returning to a visited category restores its
in-session page; existing persisted symbol-page behavior remains unchanged.

The 48 dp portrait selector row fits inside the existing fixed keyboard height.
Page swipes start only in the character grid, leaving category scrolling to its
scroll view. Private fields keep navigation/recents isolated from stored settings.

Phone verification: three navigation tests, two exhaustive symbol-order tests,
stable-height integration and a real category swipe. The swipe moves the category
strip without turning the character page or inserting text. Screenshots were
inspected for legibility and clipping. Overall keyboard bounds remained fixed.
This run used portrait Android 13; landscape physical UX was not re-audited.

Negative test-rig findings: initial coordinates were recorded during Android's
IME entrance animation (content moved 716 screen pixels). The probe now waits for
that animation. A subsequent assertion expected an intermediate tab to remain
visible after fling; it correctly scrolled offscreen. The final assertion checks
movement/visibility of the initially visible tab while requiring content bounds
and editor text to stay unchanged. No production timing workaround was added.
