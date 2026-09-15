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

Normal portrait typography matches the approved native preview. Unusually narrow
windows scale the pair to fit. Enlarged system text retains the fitted stacked
arrangement with Plex; short landscape rows retain the earlier compact styling.
This bounds the change to the approved geometry and prevents larger text from
crowding the fixed key height. No drawing changes apply to candidates, Zhuyin,
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
