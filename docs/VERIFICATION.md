# Verification — reviewed MinIME 0.2.1 prototype

Date: 2026-09-07. Windows/JDK 17. Authorized phone: Samsung SM-G781B,
Android 13, RFCR91GWXLX. No tablet installation or interaction tests were performed.

Latest comparative review: [Google/MinIME gap report](GOOGLE_MINIME_GAP_REVIEW.md).
Ninety paired scenarios plus 12 paired rechecks yielded 87 comparable cases, with
three explicit limitations. The fresh acceptance suite passed 22 tests in 95.199 s.
The isolated phone benchmark passed in 16.784 s: dictionary construction 11,349 ms,
about 124 MiB retained heap, and 198 ms median for the long repeated-initial probe.
Those costs exclude dispatch/editor/rendering and are not a Google speed comparison.
The production APK is unchanged; [final cleanup state](parity/final-device-state.json)
verifies the prior IME and preferences restored and the phone in Dozing state.
Older measurements below remain historical evidence for the same prototype.

## Final gates

| Gate | Result |
| --- | --- |
| Licensed dictionary asset integrity / offline manifest | PASS |
| JVM keystroke regression | PASS — 908 assertions, 808 timed key events, 12 corpus cases |
| Debug, test and unsigned release builds | PASS |
| Android lint | PASS — 0 errors, 12 warnings |
| Android editor + real keyboard interaction | PASS — 22 tests, 97.274 seconds; focused English test also PASS |
| Debug signature | PASS — Android development certificate |
| Release excludes debug editor / test instrumentation | PASS — packaged manifest inspected |
| Packaged app network permissions | NONE — aapt permission output inspected |
| Keyboard restoration / display sleep | PASS — Samsung Keyboard restored; phone put to sleep |

The phone suite contains six editor integration tests and sixteen visible interaction
tests. It covers real composition replacement and selected deletion, editor actions,
secure policies, multiline Enter, queued selection acknowledgements, mixed Pinyin
and Zhuyin, direct literal slides, caps/Shift, first-tone Space, slide cancellation,
held Backspace, candidate scrolling/raw recovery, cursor changes, URL/password/
numeric fields, and mixed input in a local WebView textarea. The shifted-Pinyin
slide check verifies the capital result explicitly requested for MinIME. The added
Pinyin test selects jt → 今天, srufa/shrfa → 輸入法 and wxsrf → 我想輸入法,
refines a composition with Backspace, and recovers exact abbreviated input.

Version 0.2.0 adds single-tap EN / 中 switching, timed double-tap Caps Lock,
immediate punctuation and width choices, and emoji/symbol navigation with complete
skin-tone, family and flag insertion/deletion. See [feature results and reference
observations](CAPS_PUNCTUATION_RESULTS.md). The runner now backs up preferences
outside the instrumented process and bounds execution to 180 seconds.

Version 0.2.1 separates English mode from restricted fields: English word
completions are visible with lower/title/all-caps preservation, raw remains the
Space default, and ordinary English Enter commits and inserts a newline in one
press. Restricted fields retain candidate suppression and ASCII slide symbols.
The English phone sequence also covers contractions, saved mode across fields,
Caps Lock, switching back to Chinese and URL restrictions. See
[English audit and live reference observations](ENGLISH_PRIMARY_AUDIT.md).

Screenshots: [Pinyin](evidence/review-pinyin.png), [Zhuyin](evidence/review-zhuyin.png),
[emoji](evidence/review-emoji.png), [symbols](evidence/review-symbols.png).
English: [suggestions](evidence/review-english.png), [final phone run](evidence/english-primary-tests.txt).
The phone tests restore the app’s layout settings and learned choices. Both device
scripts restore the previous input method and issue KEYCODE_SLEEP from cleanup,
including on failure. A sleep command does not alter the user's always-on-display
preference; Samsung reports Dozing after the interactive display sleeps.

## Live reference evidence

Google Zhuyin 2.4.5.164561151-arm64-v8a (2451413) was installed and enabled by the
user, then tested in the same synthetic editor. Twenty-five valid reference
sequences cover gestures, Chinese/Latin transitions, first tone, Space, Enter,
punctuation, capitals, numeric text, a URL, email and editing. See
[reference study](LEGACY_REFERENCE.md) and [keystroke observations](legacy-observations.json).
The study runner's success means it completed the scripted observations; it is not
a declaration that MinIME reproduces every reference behavior. Another 39 Pinyin
sequences now cover initials, mixed syllables and sentence combinations. See
[Pinyin decoder results](PINYIN_PREDICTION_RESULTS.md), including all negative results.

## Defects and test-rig corrections

- Non-ASCII punctuation after active Pinyin reproduced as uncommitted raw text;
  it now accepts the token before committing the punctuation. ASCII technical
  tokens remain editable and unchanged.
- Explicit first-tone marks now constrain source syllable boundaries. Regression
  checks retain 西安 for ㄒㄧˉㄢˉ and reject the merged single syllable 先; explicit
  first tone also excludes third-tone 主 from ㄓㄨˉ.
- Cursor movement clears owned composition/context. Empty-engine callbacks do not
  finish editor-owned spans. Queued acknowledgements of our writes are distinguished
  from external moves without reading surrounding text.
- Candidate page counts, horizontal scrolling and idle prediction highlighting were
  reviewed and corrected. Screenshot evidence showed scrolling worked; the failing
  test compared a viewport-clipped left edge rather than the moving right edge.
- Touch tests needed accessibility/resize settling after layout and shifted-key
  redraws. Failed actions on stale node IDs were test synchronization failures.
- The first legacy coordinate replay accidentally changed layouts after its first
  case. Later results from that run were rejected. The final harness locates live
  keys, accounts for the observed first-tone/Space labels, and stops on missing keys.

## Performance and open gates

Latest isolated core run: desktop key-processing p50 0.63 ms, p95 6.17 ms,
maximum 34.53 ms over 808 events; dictionary startup 2,771 ms. These are desktop
measurements, not Android frame latency. The APK is about 3.6 MiB. Phone frame
latency, long-session memory and cold-start usability remain broader gates.

Headless Android prediction benchmark: PASS (1 test, 10.887 seconds). Normal
short probes: median 3.4–7.4 ms, p95 4.3–12.4 ms. The sentence probe takes median
10.8 ms; 32 repeated initials still take median 130 ms, down from 794 ms before
bounding intermediate sentence beams. Fresh-process dictionary load: 7,342 ms,
approximately 124 MiB retained Java heap. See [raw measurements](evidence/prediction/android-performance.json).
This excludes dispatch/composition/render work and does not establish frame latency.

Chrome, messaging and terminal apps, stock Android, Android 10, landscape,
large text and TalkBack need a broader device matrix. Ranking remains a source
frequency/beam baseline. Pinyin initials, partial/full syllable mixtures and trailing
phrase completion are implemented; short-initial and sentence ranking still need
further work and user evaluation. Reference-style composition
presentation is still different. This prototype is not declared production-complete.

## Artifact

Installed debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 3,742,475 bytes.
Version: 0.2.1 (3).
SHA-256: `181ade044ee202bd316c1fde4f86059d00224b645e8f7d5ebe4b584d56bcdf08`.
Release output: `app/build/outputs/apk/release/app-release-unsigned.apk` (unsigned).
No supplied Google APK, model, decompiled code or visual asset is distributed.
