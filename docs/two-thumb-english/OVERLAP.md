# Two-thumb word-boundary ordering

2026-09-14; baseline `75ac63c`. Type: Android touch-order bug fix.

## Reproduction and cause

Two thumbs can touch in one order and lift in the opposite order. MinIME already
completes a pending plain letter tap when the next letter is touched, preserving
contact-down order. That rule excluded Space, Backspace, and punctuation both as
the incoming key and as the pending key. Those combinations followed release
order instead, putting a space on the wrong side of a letter or deleting the
wrong letter.

The new `TypingTouchTest.testEnglishLetterBoundaryOverlap` exhaustively pairs all
26 letters with Space, Backspace, comma, and period, in both contact orders and
both release orders: 416 command-order cases. Baseline fails every reversed-release
case (208), while all ordinary-release cases pass. The four existing touch tests
also pass. This isolates a boundary in the touch dispatcher, not the dictionary.

A paired reference replay uses the same 16 event patterns in actual editors under
Google Zhuyin `2.4.5.164561151-arm64-v8a` (versionCode 2451413) and MinIME.
The seed `b` and letter `a` are diagnostic event markers, not a language corpus or
word-specific production rule. For example:

| Contacts after seed `b`; second contact lifts first | Google | Baseline MinIME |
| --- | --- | --- |
| `a`, Space | `ba ` | `b a` |
| Space, `a` | `b a` | `ba ` |
| `a`, Backspace | `b` | `a` |
| Backspace, `a` | `a` | `b` |
| `a`, comma | `ba,` | `b,a` |

Google preserves contact order for all 16 reference cases. Baseline MinIME agrees
on the eight ordinary-release cases and disagrees on all eight reversed cases.
No reference case was unavailable. The observation harness records text and
composing spans; passing the harness alone does not assert provider equality.

## Fix and boundaries

`KeyboardView` now registers the displayed alphabet keys and typing controls
(Space, Backspace, and inserted punctuation) together. On a new contact in this
group, it completes older eligible plain taps using the existing `SlideKey` rule.
The active layout must contain the QWERTY letter board. It snapshots the list
before callbacks, which can synchronously render the keyboard, and rebuilds the
list when the layout changes.

Active slides, consumed holds, and canceled contacts remain governed by the
existing eligibility checks. Candidate taps, Shift, Enter, and mode/panel switches
are outside this completion group. Those actions have separate selection or layout
lifecycles; this fix does not claim to solve every possible multi-control gesture.
The list is bounded by displayed keys; no dictionary lookup or new delay enters
the pointer path. No spelling, ranking, learning, geometry, or default changes.

## Verification

- Shared core: 306,634 assertions pass before the Android build. The patch changes
  no core/native suggestion or acceptance logic, so the previously pinned desktop
  language replay was not rerun for this Android dispatcher change.
- Local offline debug and instrumentation APK builds pass.
- Fixed command-order matrix: all 416 cases pass.
- 21 instrumentation tests pass, including existing human-input precision,
  candidate stability, caps lock, slides/cancellation, held delete, and English
  trace/vertical-slide controls. Synthetic event correctness is not a human touch
  hit rate or a typing-speed measurement.
- Paired final-editor replay: all 16 fixed cases match Google, up from 8/16 before
  the fix; no cases unavailable. Text equality and composition spans are recorded
  separately in `evidence/overlap-summary.json`.

The plan and compressed raw paired observations are retained under `evidence/`.
These are deterministic diagnostics. No conversation accuracy or WPM improvement
is claimed. The broader layout/spatial-correction study remains a separate proposal.

## Phone sessions and reproduction

Only RFCR91GWXLX was used after SHINE explicitly acknowledged the test window.
Every session ran through `phone-lease.ps1` and `test-device.ps1`, restored the prior
Samsung IME and MinIME settings/learning files with readback, and verified display
OFF. The paired-study runner also verifies OFF after pulling evidence. No
always-on-display setting was changed.

From a locally built debug/test APK pair, with a newly acknowledged phone window:

```powershell
./tools/test-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk `
    -TestClass dev.minime.ime.TypingTouchTest
./tools/study-parity.ps1 -Serial RFCR91GWXLX `
    -Plan docs/two-thumb-english/evidence/plan.json `
    -Output artifacts/two-thumb-english/new-run
```

Use a fresh output directory. `ParityStudyTest` supports `overlapKeys` with two
semantic key names; its older two-letter `overlap` format remains supported.
