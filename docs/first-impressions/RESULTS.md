# MinIME 0.5.0: first-use layout and phrase selection

The user's first-use feedback identified layout/appearance and phrase candidates/
selection as the priorities. This release changes those paths based on live Google
Zhuyin observations. It is closer in these areas, not a complete Google replacement.

## Changes

- Flat QWERTY keys, 59 dp portrait row pitch, centered secondary symbols, geometric
  control icons, an idle Chinese/English toolbar and six bottom controls replace the
  boxed layout. The bottom globe switches language directly. Emoji is a comma hold;
  emoji/symbol switching remains available in the palette header.
- Chinese raw spelling is above the candidate strip, leaving the strip for choices.
  The primary Space choice uses dark text. Candidates scroll continuously; the
  expanded grid packs short choices and allows long phrases to wrap. It replaces
  the previous 90 dp raw slot, 24-item pages and fixed three-column grid.
- Rime is enabled when no saved backend preference exists. An explicit false value
  remains false. When the model becomes available, active Chinese predictions are
  refreshed rather than waiting for another key. The original decoder is selectable.
- Prefix choices carry the length consumed by Rime. For `nihao`, tap 你, then select
  好 from the remaining `hao`. Exact recovery, suffix editing, punctuation and a
  language switch preserve or finish the remaining spelling. Space continues to
  accept complete phrases. Prefix learning records only the consumed reading.

No dictionary/model inputs or evaluation-specific phrase scores changed. The full
candidate path retains the same first choices and all 84 complete candidate lists
as 0.4.0: 14/24 reference, 20/24 development, 15/36 fresh. The UI exposes three
prefix previews after the first three full phrases; order within each group stays
unchanged. This is a selection/presentation change, not a new accuracy improvement.

## Phone evidence

All nine final scenarios were observed for both keyboards (18 executions) in a
synthetic editor on Samsung SM-G781B, Android 13. This observation count is not a
parity score. Both commit 我們明天見 for `womenmtjian`, and both finish 你好 after
selecting 你 then 好. Raw stage data and screenshots are in [final](final/), with
baseline observations in [baseline](baseline/) and the isolated prefix reference
in [partial-reference](partial-reference/). The plan is [final-plan.json](final-plan.json).

| View | Google Zhuyin | MinIME 0.5.0 |
|---|---|---|
| Idle Pinyin | [Reference](final/parity-google-idle-pinyin-step-0.png) | [Updated](final/parity-minime-idle-pinyin-step-0.png) |
| Active Pinyin | [Reference](final/parity-google-active-pinyin-step-0.png) | [Updated](final/parity-minime-active-pinyin-step-0.png) |
| Expanded candidates | [Reference](final/parity-google-expanded-pinyin-step-1.png) | [Updated](final/parity-minime-expanded-pinyin-step-1.png) |
| Partial selection | [Reference](final/parity-google-partial-selection-step-1.png) | [Updated](final/parity-minime-partial-selection-step-1.png) |

The full suite passed **34 tests in 160.657 seconds**. Core regression passed
**1,219 assertions**. Tests cover original-backend behavior separately from the new
default/prefix path, including gestures, Caps Lock, exact recovery, English tracing,
punctuation, emoji access, selection, lifecycle and private fields. The first layout
run exposed a removed palette switch and an unstable initial Zhuyin touch; the route
was fixed and explicit first-slide evidence/assertions were added before the passing
repeat. See [LAYOUT_NOTES.md](LAYOUT_NOTES.md) for the narrower timing limitation.

Across the 84 warm-model phone probes, the actual prefix-query path took p50
**17.09 ms**, p95 **59.03 ms**, maximum **68.17 ms**. These debug native timings
exclude original-decoder fallback lookup, scheduling, editor updates and rendering;
they are not total key-to-frame latency. Raw values are in [rime-phone.json](rime-phone.json).

## Remaining differences

Google keeps unfinished Pinyin and selected prefixes inside the keyboard until
completion. MinIME retains its inline composing-span policy and commits a selected
prefix into the editor immediately; unconsumed spelling stays editable. Raw Pinyin
is not syllable-spaced in MinIME's composition row. Candidate ordering still differs,
and long abbreviated sentences remain weaker. Symbol/emoji panels, some punctuation
choices, visual details and accessibility/device coverage still need further work.
No proprietary Google assets, dictionaries, code or models are packaged.

## Final package and repeat checks

A separate three-test native repeat passed in **5.446 seconds** with the long-input
stress test using the actual prefix-candidate path. The 32/96/90-character synthetic
queries took **524.75 / 164.20 / 20.29 ms**, below the predeclared one-second bound,
and the next known query remained correct. Prefix enumeration adds work compared
with the old complete-only query; these stress results are not instantaneous typing
or a general worst-case guarantee. All 84 complete lists again matched the prior
run. Repeated prefix-query p50/p95/max were **20.27 / 62.06 / 70.98 ms**.

Debug, Android-test and unsigned release builds passed. All four native ABIs were
rebuilt (arm64-v8a, armeabi-v7a, x86 and x86_64); only ARM64 has device coverage.
Lint reports **0 errors, 15 warnings**. Packaged assets/notices and unchanged model
hashes passed validation. Release ELF LOAD segments and debug/release ZIP alignment
passed 16 KB checks; no 16 KB-page device was tested.

The universal debug APK is **64,406,471 bytes**; its ZIP is **34,884,406 bytes**.
The unsigned release APK is **39,267,203 bytes**. Artifact identities are recorded
in [verification.json](verification.json). The complete ZIP downloaded successfully
through the user-requested temporary Cloudflare Tunnel and matched its SHA-256.

MinIME **0.5.0 (6)** is installed on the authorized phone. Samsung Keyboard was
restored, both preference-file hashes match their backups, and the final power
state was **Dozing**. No tablet was used and no AOD setting was changed.
