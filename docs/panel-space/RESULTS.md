# Keyboard header and palette space

Version 0.5.5 removes the always-reserved 24 dp header. Idle keyboard controls now
start at the top edge. Chinese spelling is a selectable, content-width annotation
above the candidate row; it does not resize the editor or move the letter keys.
Symbols and emoji hide the candidate/language toolbar, put category controls at
the keyboard top, and reuse its 48 dp for a third grid row (18 entries per page).
The bottom action row stays fixed. Portrait content is 284 dp, landscape 184 dp,
plus the system navigation inset. No ranking, model, acceptance or gesture rule
changed.

## Evidence

The frozen isolated-plan.json observes idle, composing, symbols, letters, comma
hold to emoji, and return to letters in Pinyin and English on both IMEs. All four
records are observed in observed/ and final/. Google's categories replace its
toolbar and its emoji panel has three rows. MinIME's top changes from y=1332 to
y=1404 on the 1080x2400 phone, while Google's top is y=1407. Google reports an IME
bottom of 2256; MinIME reports 2400 including the 144 px navigation region. These
different window boundaries must not be mistaken for different usable heights.
Every final recorded stage retains the same respective IME window bounds.

Representative screenshots:

- [Google emoji](final/parity-google-pinyin-panels-step-4.png)
- [MinIME emoji](final/parity-minime-pinyin-panels-step-4.png)
- [MinIME spelling chip](final/parity-minime-pinyin-panels-step-1.png)
- [MinIME idle](final/parity-minime-pinyin-panels-step-0.png)

The paired final images use the completed layout implementation immediately before
the metadata-only version bump. Final regressions and human-input runs use 0.5.5.

Both new baseline layout regressions failed: idle controls were offset 72 px and
palette categories 216 px from the keyboard top. The first implementation passed
12/13 checks but placed the popup at the keyboard bottom because coordinates were
relative to the parent window. The retained first-fix-regressions.txt records the
actual bounds. Screen-relative popup layout fixes that cause; raw selection and
hide/restart then both pass (22.089 seconds). This uses Android's documented
[screen-coordinate PopupWindow mode](https://developer.android.com/reference/android/widget/PopupWindow#setIsLaidOutInScreen(boolean)).

Final targeted run: nine tests pass in 45.700 seconds. Coverage includes the two
new space regressions; contained layout and stable height across portrait,
landscape, font scales, all panels, numeric/private modes and insets; spelling
popup panel/editor ownership; palette paging/selection; optional recents and
private input; URL/password/numeric fields; punctuation hold selection; and fixed
attached IME/key bounds throughout typing and panel switches. The earlier
candidate scrolling check also passed with the floating chip.

The frozen human-input suite passes on 0.5.5: eight matrix/candidate-stability
tests (22.240 seconds), development replay (39.276 seconds), and four holdout/
caps/slide/delete tests (52.905 seconds). All 12,896 required geometric cases and
185 composing checkpoints across 20 varied replay tokens pass. The separate 208
exploratory touches outside the intended key still miss it; these are reported
as errors, not counted as passing tolerance cases. These checks measure touch
and composition preservation, not phrase prediction accuracy. Evidence, fresh
run IDs and artifact hashes are retained under human-input/.

## Packaging and limits

Debug, test and unsigned release builds pass, as do asset provenance, four-ABI
packaging checks and 16 KB ZIP alignment for both app APKs. Lint has zero errors
and 17 warnings. The new warning is Gravity.LEFT in physical screen-coordinate
popup placement; changing it to logical START would change that coordinate frame
under RTL. Existing warnings are unchanged. Model SHA-256 remains
1d1b9a2d5379eeb0099b38cb48695a22541300c765a0dd2d5851c25b049572f1.

Hardware verification is limited to RFCR91GWXLX, Android 13 ARM64. Synthetic
layout matrices cover landscape and larger fonts; they do not substitute for
human usability or TalkBack review on other devices. No decoder or language-data
changes required repeating the desktop ranking corpus. Google's palette visuals,
category design and symbol-entry acceptance still differ: Google commits a
Chinese candidate when opening symbols, whereas MinIME retains the composition.
This change closes the space allocation gap, not complete visual/behavior parity.

Every test restores the prior Samsung IME and MinIME settings/learning files,
then sleeps the phone. Each run verifies Dozing; always-on-display is unchanged.
The ZIP contains only the installable debug-signed APK; unsigned release is for
packaging validation. package.json records artifact identities.
