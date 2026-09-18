# Landscape phone assessment — 2026-09-18

Corrected recommendation: keep compact and reject the current split prototypes.
The earlier recommendation to offer taller split is withdrawn: its empty center
does not return usable editing space while it consumes more editor height.
Full-height sides also fail because they obscure the app. A split design must
demonstrate a usable central editor before geometric benefits justify it.
These remain debug-only prototypes; no production split feature was added.

Phone: RFCR91GWXLX, Samsung SM-G781B, Android 13 / API 33, density 3.
Native prototype content viewport: 770.67 × 360 dp. Screenshots are actual phone
captures, with fixture text and clearly marked sample candidates. They do not
measure suggestion quality. The real IME is captured separately.

| Native prototype | Letter target (dp) | Clear height above (dp) | Assessment |
|---|---:|---:|---|
| Compact joined KALQ | 96.33 × 25.5 | 176 | Preserves editor space; shallow targets and long inward reach |
| Split, same height | 60 × 25.5 | 176 | Rejected: center returns no usable editing space |
| Taller split | 60 × 44 | 96 | Rejected: sacrifices editor height while the center stays unusable |
| Full-height sides | 48 × 68 | 0 | Large targets, but covers the title and beginnings of text lines |

Both compact and taller split screenshots show the two fixture text lines; the
taller split leaves little extra space. Full-height sides leave a center gap,
but the app does not reflow into it. Existing controls/text behind either side
remain covered. This directly limits usefulness despite favorable simulated
contact results in RESULTS.md. These screenshots show geometry, not thumb comfort.

## Touch checks and limitations

Per layout: 26 centered letter taps, 26 upward capital slides, 26 downward symbol
slides and 70 letter taps in a repeated pangram. All 148 actions per layout
produced their intended output: **592 / 592 injected actions across four layouts**.
This is a software touch-routing check, not a measured human hit rate. The trial
does not establish typing speed, physical touch latency, fatigue or multitouch
ordering. Split footer mode/symbol controls are illustrative, not full panels.

Native prototypes reuse production SlideKey and its 22/16 landscape renderer.
They are hosted inside an activity and do not implement split InputMethodService
window insets, transparent touch regions or editor resizing. The separate real
compact IME check typed `hello`, observing `h`, `he`, `hel`, `hell`, then `hello`
in the editor, with 176 dp editor height. The real IME shows the system navigation
rail; its letter width is about 90.33 dp instead of the prototype's 96.33 dp.
This viewport difference must be handled when implementing a real split IME.

Final run: all six instrumentation tests passed in 108.491 seconds. This includes
the native trial, Space-label regression, 192 landscape rendering configurations,
fixed-height checks and both portrait typography tests. Preferences and prior
Samsung keyboard were restored, rotation settings stayed unchanged, display OFF
was verified, and the phone reservation was explicitly released.

## Negative findings retained

1. First trial: prototypes passed, but actual IME output was ` ello`. The original
   trial asserted key availability but did not assert that final string. Adding
   stable screen-bound sampling before contacts and exact output assertions
   produced `hello` with no production input changes. Opening animation timing
   is the likely cause; the first run did not retain enough bounds telemetry to
   prove which moving rectangle received its first contact.
2. Second trial: blank screenshots, 316 failed checks and no real IME. The test
   could retain an obsolete activity after orientation recreation. It now uses
   the current, focused, attached instance, checks ownership and removes old
   screenshots before each run. This was a fixture failure, not keyboard accuracy.
3. The real compact Space label was clipped. Baseline regression: expected 38 px
   of Chinese glyph ink, observed 2 px; top/bottom padding were 42 px each.
   InsetDrawable's 14 dp visual inset also became TextView padding, leaving only
   6 dp of a 34 dp row for text. The fix clears text padding only in landscape;
   decoration, hit target, row height, portrait labels and input logic stay intact.

Raw results, screenshots and test output are in `device/`. `manifest.json` there
records evidence/source hashes and session identities. Earlier failed runs are
retained as negative evidence; only the final run is used for the final comparison.

## Reproduction

Obtain an explicit shared-phone reservation, then build locally:

```powershell
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --offline --no-daemon
./tools/test-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk `
  -TestClass 'dev.minime.ime.LandscapeDeviceStudyTest,dev.minime.ime.SpaceLabelTest,dev.minime.ime.LandscapeKeyboardTest,dev.minime.ime.KeyboardHeightTest,dev.minime.ime.KeyboardTypographyTest' `
  -TimeoutSeconds 600 `
  -Reports @('landscape-device.json','landscape-compact.png','landscape-split-low.png','landscape-split-tall.png','landscape-side-panels.png','landscape-actual-ime.png')
```

Use the repository JDK/SDK configuration. The wrapper holds the phone mutex,
restores preferences and the prior IME, then sleeps and verifies display OFF.
The trial verifies system rotation settings unchanged and finishes its landscape
activity. Explicitly release the cross-task reservation after cleanup.
