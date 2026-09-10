# Interaction refresh — 2026-09-11

Design changes, separate from Japanese conversion and branding fixes.

Fast switch: the bottom language key changes from English ↔ current mixed mode
to Chinese ↔ last explicitly selected Taiwanese/Japanese mode. Before either
is selected (or while that pack is disabled), Chinese ↔ English is the fallback.
English-only remains in the picker and idle shortcut. Store last focus separately
from current mode, migrate from the previous mixed mode, retain it when Chinese
or English is selected, and preserve it across temporary pack disable. Literal
editor policy remains separate. Switching reinterprets owned input without
committing it. Tests cover restart, fallback, pack disable, composition and height.

Palette design: expose horizontally scrollable category tabs with a selected
state, retain the complete category chooser, show subgroup hierarchy and preserve
content page when dismissing a chooser. Restrict page swipes to the character grid
so they do not steal category scrolling. Overall keyboard height stays fixed;
private navigation must never read or write remembered pages or recents.

Branding fix: add a thin teal contour to the cream face only in transparent colour
marks and the light-background feature graphics. Approved solid and adaptive icons,
face curves, hat, smile and palette remain unchanged. Rebuild the full suite and
visually inspect on white and dark surfaces.


Delivery: MinIME 0.8.2 (version code 30), debug APK installed during integration
and packaged as `MinIME-0.8.2.zip`. All 11 distinct targeted phone checks passed
across corrected test runs: 2 mode preferences, 3 palette navigation, 2 symbol
ordering, Japanese joined input, two-family mode switching, fixed height and
category swipe. The two fixture issues and animation/visibility assumptions are
recorded in the component reports. No blanket full-device-suite claim.

APK SHA-256: `100641195f6d0d7f63642ff0990a0f4258131e54a6eb34536ed1872837c4383a`.
ZIP SHA-256: `7d32dd9eb999f0690e4d07a2dc77a6490342b9c3e0aa042f8957fb37cce443b4`.
Public tunnel download was retrieved and byte-verified. Android build/lint,
source asset integrity and packaged provenance checks passed. This debug delivery
does not clear the outstanding Google Play release gates.

See [Japanese evidence](../japanese-continuity/README.md),
[switching](switching.md), [palettes](palettes.md), and
[branding](../../branding/README.md).
