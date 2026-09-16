# Plex keyboard typography

Type: approved visual design change. QWERTY and joined KALQ letter keys in all
Latin-based input modes use IBM Plex Sans Condensed Regular: 26 sp foreground
letters, 18 sp muted symbols, 12 dp minimum symbol area, 4 dp right inset, and
letters raised 3 dp. No per-character rules or stretched glyphs. Font fallback
continues to supply Chinese punctuation.

The font is a local Android font resource, loaded through the resource cache.
`PortraitKeyLabels` owns the common native drawing geometry. It caches envelopes
when a key is created and reuses its bounds object while drawing. Gesture dispatch,
composition, dictionaries, symbol assignments, settings and keyboard height are
unchanged. The TextView text and accessibility actions remain available even
though portrait labels are drawn explicitly. Sliding displays the alternate glyph
in the center, then restores the normal label on release/cancel.

Normal portrait typography matches the approved native preview. All portrait font
scales use that same 26/18 pair. The pair scales uniformly only when its cached ink
envelopes and insets exceed the available key size. Larger system text no longer
switches to smaller stacked labels. Short landscape rows retain the earlier compact
styling; key height and touch targets stay unchanged. No drawing changes apply to candidates, Zhuyin,
number/symbol panels or control keys.

Risk and verification: independent golden renders must match actual production
letter-key drawing; inspect both layouts, case states and punctuation inventories.
Check fixed-height/scaled-text fit and existing taps, slides, multi-pointer ordering,
mode changes and cancellation. Local Android build/lint and device instrumentation
are the relevant gates. No core suggestion change calls for a model benchmark.
Run phone tests only inside the acknowledged reservation and shared lease, restore
previous IME/preferences, verify display OFF, then explicitly release.

Font source: Google Fonts commit
`1ac2012c34919f5fa2675aacf723fa98edb30b5f`,
`ofl/ibmplexsanscondensed/IBMPlexSansCondensed-Regular.ttf`, version 1.3.
SHA-256: `e7437c072eef2ef592ae6f2beb0000446287385907abb57ac1cf07bcbaa2aa33`.
Original font size: 111,236 bytes. Font bytes and metadata are unmodified.
Copyright 2017 IBM Corp.; Reserved Font Name “Plex”; SIL OFL 1.1.
Original licence: `third_party/fonts/ibm-plex/OFL.txt`, also packaged in NOTICE.txt.

The eight independent test fixtures preserve approved Android preview pixels from
session `2dab39e2-f00d-460c-9e1a-9706fbadf13e`, before runtime implementation.
They cover 26/18 with the smaller inset and raised letters. Tests compare letter
key regions only; mode controls and other unchanged screenshot content are not
typography assertions. They are visual regression fixtures, not typing accuracy.

## Verification result

Local `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` passed.
Seven selected device tests passed in session
`e15b31ce-1b52-4dfa-991e-07cfaad9462e`:

- Pixel-exact match for all 208 letter-key regions in the eight independent
  approved renders: both layouts, lowercase/capitals and ASCII/Chinese punctuation.
- Enlarged portrait text at font scales 1.3 and 1.5 fits its available key bounds.
- Existing all-letter slide/mode/height checks (including landscape), all-letter
  multi-pointer space/delete ordering, caps lock, English trace/slide and Pinyin
  overlapping-thumb integration checks.

The font's packaged bytes match the pinned upstream SHA-256. Prior Samsung IME
and MinIME preferences/learning were restored with readback, actual display OFF
verified, and the shared phone reservation explicitly released. No GitHub CI used.

## Font-scale fallback regression (2026-09-16)

Type: bug fix restoring the approved portrait design. The 0.8.6 implementation
selected overlap only at `fontScale <= 1.05`. Above that threshold it switched
symbols from 18 sp to 10 sp and letters from 26 sp to a fitted 23 sp stack. On
44 dp KALQ rows the extra reserved symbol line reduced the letter size further.
The supplied screenshot is consistent with this stacked Plex path. The test
phone was at scale 1.0; the screenshot device's exact setting was not measured.

The existing enlarged-text test proved only containment; it did not check that
larger text retained the approved styling or size. A new native raster regression
fails on the old implementation at 1.06 because letter ink becomes smaller than
at 1.0. The first exploratory run used an incorrect hard-coded letter ink color;
that test-detector error was corrected before the causal failing run.

Remove the font-scale branch. Reuse the approved drawing path and uniformly fit
its cached letter/symbol envelopes plus insets to the actual key bounds. This
preserves normal-size reference pixels without a new preference or per-glyph
exception. Scope is portrait Latin key labels in QWERTY and joined KALQ; control
keys, candidates, landscape, mappings, hit areas and language behavior are unchanged.
No suggestion/core benchmark is needed for this drawing-only change.

Regression coverage: seven context font scales (1, 1.05, 1.06, 1.1, 1.3, 1.5, 2),
three window widths (320, 360, 411 dp), both layouts, letter cases and punctuation
inventories. Check rendered ink containment, visible symbols and letters, and no
size reduction below the normal-scale reference (one raster pixel per key in aggregate
allowed). The golden-pixel test translates D/N/F/V reference regions by two key
widths to account for the separately committed centered row; reference images
are not regenerated. Existing KALQ slide/mode/height and overlapping-pointer
checks cover input behavior. Tests use configuration contexts, not global device
font or display changes.

Verification result: local debug/test APK builds and lint passed (0 errors).
The old renderer failed the new regression at scale 1.06 in phone session
`a867b356-773d-409a-a42c-8c8eee35df2a`. The fix passed all four selected typography
and gesture tests in `7bd61b3b-f86f-42e8-8e12-ca3d91d29568`. After replacing
per-pixel JNI reads with bulk bitmap reads, both final typography tests passed
again in `52af0ff9-6009-4da6-979c-040cfebca525`. Coverage comprises 168 portrait
configurations / 4,368 letter keys plus the eight independent normal-size golden
renders. These are visual checks, not human typing-efficiency measurements.
Native previews at scales 1, 1.1 and 1.5 were inspected. Every phone session
restored prior IME/preferences and verified display OFF; reservation explicitly
released. This fix is not in the already-distributed 0.8.7 / code 37 ZIP.
