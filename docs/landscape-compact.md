# Compact landscape labels — 2026-09-18

Type: landscape presentation change, separate from the Chinese recovery fixes.
The user offered compact symbols or a taller split layout. This implements the
compact direction; a split layout is not implemented.

Baseline: `8e4e9d6`. Landscape reserves 136 dp for keys plus a 48 dp candidate
strip. QWERTY letter rows are 34 dp; joined KALQ divides three such rows among
four letter rows, approximately 25.5 dp each. The old renderer stacked a 21 sp
letter and a 10 sp (QWERTY) or 8 sp (KALQ) symbol inside those short rows.

The new landscape renderer uses adjacent slots with IBM Plex Sans Condensed:
22 sp letters, 16 sp muted symbols, 8 dp separation and a 3 dp outside margin.
It uniformly scales the ink only when the actual key cannot contain the pair.
Using font-wide envelopes keeps punctuation aligned instead of moving each
symbol vertically based on its own ink height. While sliding, the alternate
character is centered and the hint hidden, as before.

The owning boundary is SlideKey drawing. Key positions, hit rectangles, symbol
mappings, gestures, language behavior, candidate acceptance, keyboard height,
numeric/Zhuyin boards and portrait 26/18 labels remain unchanged. There is no
new preference or stored-data migration. This improves use of horizontal space;
it does not claim to fix the short landscape touch targets.

Validation:

- Local debug APK and instrumentation APK builds passed (offline Gradle).
- The prior shared-core and pinned desktop tests still apply: no decoder, data,
  ranking, composition or acceptance code changed in this commit.
- Added `LandscapeKeyboardTest`: 192 rendered board configurations across
  QWERTY/KALQ, 480/600/760/960 dp widths, 1/1.3/2 font scales, Chinese/ASCII
  punctuation and upper/lower case. Checks actual Android ink visibility,
  clipping, non-overlap, fixed height, letter taps and accessible symbol actions.
- At the original commit, instrumentation compiled but had not run; phone
  approval was pending. The follow-up device validation below closes that gate.
- Inspected an approximate desktop font preview. It is not an Android screenshot
  or a pixel golden and does not establish device readability or touch accuracy.

## Phone follow-up and Space-label fix

With direct user approval and an acknowledged reservation, RFCR91GWXLX passed
the landscape ink/symbol test, keyboard height test and both portrait typography
tests. The on-screen native trial also passed taps and up/down slides across
four landscape geometries; these automated contacts do not measure human accuracy.

Device inspection found an additional footer bug: InsetDrawable's 14 dp top and
bottom decoration insets also became TextView padding. The 34 dp landscape Space
key had only 6 dp left for its label. A baseline Android pixel regression measured
2 px of visible Chinese ink instead of the expected 38 px, with 42 px padding at
each edge on the 3x-density phone.

The fix clears Space text padding only in landscape after setting its background.
It preserves the decoration, tap rectangle, row height, portrait behavior and all
input logic. SpaceLabelTest covers Chinese/English labels, portrait/landscape and
font scales 1/1.3. Baseline failed; the fix passed. Final local instrumentation:
6 tests passed in 108.491 seconds, including native trial, Space labels, landscape
ink, height and both portrait typography tests. Actual IME screenshot confirms
the full 拼音 label is visible. No dictionary/core behavior changed.

Evidence session: `artifacts/device-tests/d59677a2-3310-4ef5-b3ef-b5dd7e6b0c8b`.
Preferences and prior Samsung IME were restored, rotation settings stayed
unchanged, display OFF was verified, and the phone reservation was released.
