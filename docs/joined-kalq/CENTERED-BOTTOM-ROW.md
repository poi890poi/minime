# Centered bottom letters

Requested layout adjustment, 2026-09-16: bring frequently used D and N inward by
centering joined KALQ's four bottom letters.

Before: `D N F V [Shift ×2] [Backspace ×2]`

After: `[Shift ×2] D N F V [Backspace ×2]`

This moves the four-letter group right by two letter widths. Each letter remains
one eighth of the row width; Shift and Backspace remain two eighths each. D and N
centers move from 6.25% and 18.75% of the row width to 31.25% and 43.75%. These are
horizontal positions, not measured typing-performance improvements.

Scope: joined KALQ across the shared Latin boards. Letter order, slide-symbol
assignments, typography, row height, overall IME height and QWERTY are unchanged.
Moving Shift changes its reach and learned location; keep the change independently
reviewable. No dictionary, suggestion, correction or acceptance logic is changed.

Verification: extend the existing alphabet/slide/height test with centered-group,
equal outer-control and retained letter-width checks. It exercises every mode,
portrait/landscape and normal/enlarged fonts. Reuse overlapping-pointer coverage
for letters with Space/Backspace. Human typing efficiency is not measured here.

Result: local debug/test APK build passed. Both selected device tests passed:
`testAlphabetSlidesModesAndStableHeight` and
`testOverlapEveryLetterWithAllSpacesAndDelete` (including 416 overlapping-pointer
combinations). Suggestion/decoder code is unchanged; no core or desktop Rime rerun.
Prior IME/preferences were restored, actual display OFF was verified, and the
acknowledged phone reservation was explicitly released. No hosted CI was used.
