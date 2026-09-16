# Joined KALQ

Enable **Settings → Use joined KALQ letter layout**. This single option controls
Chinese Pinyin, Taiwanese, Japanese and English letter boards. It also applies
to literal/password text fields. QWERTY remains the default; disabling the option
restores it. Dedicated Zhuyin and numeric layouts are unaffected.

| Row | Letters and controls | Slide-down symbols, ASCII punctuation |
| --- | --- | --- |
| 1 | M B W H G T O J | 1 2 3 4 5 6 7 8 |
| 2 | P Space X C I E Space U | 9, then 0 @ * + - on the remaining letters |
| 3 | R Y S Z K A L Q | = / # ( ) ' : " |
| 4 | Shift, D N F V, Backspace | ? ! ~ … |

The two internal Space keys accept input exactly like the bottom Space key,
including hold-to-keep-literal behavior. Shift and Backspace each use two columns
at the left and right edges, centering D N F V between them. Slide up for capitals; one-shot Shift and double-tap
Caps Lock retain their existing behavior. Punctuation width follows the editor
and mode policy; the same positions carry the corresponding Chinese symbols.
The existing symbol/emoji pages and their remembered page remain available.

Four letter rows share the former three-row letter area, preserving the complete
keyboard height. Hint typography is fitted to these shorter rows. Whole-word
tracing remains QWERTY-only: the current decoder's geometry is QWERTY-specific.
Joined KALQ uses taps and per-key slides; it does not ship the experimental fuzzy
decoder or change dictionary ranking, correction defaults, learning or privacy.

Geometry follows the joined adaptation in [the compact-layout study](../two-thumb-english/compact/DECISION.md),
based on [KALQ Figure 1](https://www.pokristensson.com/pubs/OulasvirtaEtAlCHI2013.pdf).
The Android controls and hit regions are independently implemented. Simulation
improvements are not a claim of faster human typing.

The [September 16 bottom-row adjustment](CENTERED-BOTTOM-ROW.md) centers the four
letters; older simulation results and screenshots use the previous left-aligned row.

## Verification, September 15, 2026

- Core: 306,718 regression assertions pass, including the complete letter/symbol
  inventory. This is mechanism coverage, not a language accuracy score.
- Desktop Rime: all 13,014 existing evaluation inputs complete; 11,272 uncached
  native queries. The dictionary and decoder behavior were not changed.
- Android: 18 initial layout/settings/backup/touch tests, 55 editor/keyboard/Rime/
  capture tests, then six final-version tests including additional private-field
  and active-composition checks. All pass. These are 79 executions, including
  repeated final checks, not 79 independent human trials.
- Every letter's tap, slide-up and slide-down action is exercised across all six
  supported mode IDs, portrait/landscape and normal/enlarged font configurations.
  Heights also remain fixed through symbol/emoji/punctuation panels and fallback
  numeric/Zhuyin boards. All three Space keys are exercised.
- 416 combinations cover each letter against all three Space keys and Backspace,
  both contact orders and both release orders. Cancellation emits no text.
- Changing the layout preserves active composition identity, raw spelling and
  candidates. The new preference round-trips in backups and rejects wrong types.
- The real editor capture asserts joined geometry in Pinyin and literal English
  Space acceptance with correction off. Screenshots show unmodified production
  UI and actual candidates. Their authored note examples are demonstrations, not
  evaluation/training corpus entries.
- SHINE explicitly acknowledged the phone reservation before any ADB use. Each
  session held the shared mutex and restored the original preferences/IME with
  readback; actual display OFF was verified after each. The phone was explicitly
  released after the final session. No always-on-display settings were changed.

Final captures use the 0.8.5 (34) debug build, with the same production keyboard
code used for the signed release. They do not certify signed-release latency,
human hit rate, or 16 KB-page runtime behavior. The optional layout's four rows
remain shorter than QWERTY, particularly in landscape.

[Pinyin screenshot](chinese.png) · [English screenshot](english.png) ·
[Actual candidate labels](capture-candidates.json) · [Final phone checks](final-phone.txt)
