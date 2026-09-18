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
- Instrumentation tests compile but have **not run**. Phone approval remains
  pending from the earlier blocked session. No phone operation occurred here.
- Inspected an approximate desktop font preview. It is not an Android screenshot
  or a pixel golden and does not establish device readability or touch accuracy.

Before release, run the new landscape tests, the existing portrait golden and
height tests, and actual landscape gesture checks on RFCR91GWXLX after direct
approval and a fresh acknowledged reservation. Retain the phone lease throughout
cleanup, restore its prior IME/preferences, and verify its display is OFF.
