# MinIME 注音

An offline Android IME with Pinyin and English as primary layouts, plus Taiwan Zhuyin. Chinese mode also supports mixed English. This repository implements the functional prototype in the product specification. It does **not** claim verified equivalence to legacy Google Zhuyin or production language-model quality.

The [September 7 comparative review](docs/GOOGLE_MINIME_GAP_REVIEW.md) covers 90 paired Google/MinIME scenarios, targeted rechecks, screenshots, and prioritized remaining gaps. Production behavior was held unchanged during that review.

Version **0.5.2** keeps the IME height stable while typing, expanding candidates,
switching languages/layouts, and opening symbol or emoji panels. Portrait and
landscape each use a fixed content budget; system navigation insets remain honored.
See [height verification](docs/stable-height/RESULTS.md).

Version **0.5.1** restores Taiwan-standard glyph conversion, keeps literal English
defaults visible, separates learned Chinese choices from English continuation,
and retains source-capitalized English vocabulary. These are data and general
logic changes, evaluated in the shared core before Android builds. See the
[candidate-quality results](docs/conversation-ranking/RESULTS.md) and
[desktop corpus workflow](docs/conversation-ranking/DESKTOP_TESTING.md).

Version **0.5.0** aligns the QWERTY appearance and layout with the observed legacy
keyboard, adds continuous candidate browsing and explicit partial-phrase selection,
and enables Rime Pinyin by default. Turn **Use Rime for Pinyin phrase prediction**
off in MinIME settings to use the original decoder; an existing explicit choice
is preserved. See [first-use results](docs/first-impressions/RESULTS.md) and the
[existing-engine comparison](docs/existing-engines/RESULTS.md).

## Included

- Standard `InputMethodService`, Android 10+ (min 29, target/compile 35).
- Three-row staggered QWERTY and four-row Taiwan Zhuyin, with Latin/symbol slide hints and bottom-row ㄦ.
- Bottom globe key: one tap switches between English and Chinese. English offers completions, next words, spelling alternatives and word tracing. Automatic correction on Space is optional and off by default.
- Double-tap Shift for Caps Lock; tap again to unlock. Upward letter slides enter capitals.
- Immediate Chinese/English punctuation, hold-drag-release period popup, categorized symbols, and 3,010 Unicode emoji sequences. Optional local recents are disabled in private input.
- Raw spelling above Chinese candidates, exact recovery in both languages, continuous candidate scrolling and a grid that gives long phrases more room.
- Incremental token intent: Chinese, English/ambiguous Latin, URLs/email, identifiers and alphanumeric text.
- English Space commits U+0020; Chinese Space accepts a candidate. On an unfinished Zhuyin syllable, Space first adds first tone. Another Space after commitment inserts U+0020.
- Taiwan vocabulary, full/abbreviated Pinyin syllables, phrase segmentation, English completions, contextual Chinese continuations and explicit local choice learning.
- Editor actions, composition deletion, cursor invalidation, private/password/numeric/email/URL field policy.
- No Internet permission, input logging, telemetry, cloud dependency or backup of learned data.

## Build and run

Install JDK 17, Android SDK platform 35, NDK 27.2.12479018 and CMake 3.22.1. Set `ANDROID_HOME`, or create an ignored `local.properties` with `sdk.dir=E\:/Android/Sdk` on this Windows setup. Fetch pinned native sources once before building:

```powershell
python tools/fetch_rime_sources.py
.\gradlew.bat :core:regression :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

On Linux/macOS use `./gradlew` instead. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Release builds are unsigned until the project owner supplies their signing configuration; the reference APKs and their signing keys are never reused.

Install the debug APK, open **MinIME 注音**, enable it in Android settings, then choose it from the input-method picker. The setup screen includes a test field, local dictionary editor, learning reset and open-source notices.

Type `zhege`, Space, Space, `pronunciation`, Space, `budui`, Space to produce `這個 pronunciation 不對`. No language key is involved. Chinese homophones may need a candidate tap, such as selecting `請` for `qing`. To keep `ming` literal, tap the exact-input control. Hold Space also commits exact input plus a space.

The onscreen comma and period accept pending composition and insert immediately: ，。 in Chinese mode and ,. in English mode. Slide up for the other width. Hold comma to open emoji; hold period for punctuation, text faces and the Chinese-mode punctuation-width preference. Tap ?123 for symbols and ABC to return to letters; hold comma for emoji. Physical-keyboard ASCII punctuation remains with its token so URLs, paths and code retain exact spelling.

Pinyin candidates support initials and mixed syllables, for example
`womenmtjian` → 我們明天見 with Rime. Tap the desired candidate; abbreviations
that match known English words, English prefixes or technical commands retain
literal acceptance. Other lowercase abbreviations with candidates prefer Chinese.
Apostrophes force syllable boundaries. With Rime, selecting 你 from `nihao`
commits 你 and continues conversion of `hao`. Exact recovery preserves the remaining
spelling. Space accepts a complete phrase; it never silently drops an unconsumed
suffix. Swipe the candidate strip or expand it to browse alternatives.

## Verification

```powershell
# Fast core checks without Gradle or the Android SDK (JDK 17 required):
.\tools\test-core.ps1
python tools/verify_assets.py

# Install and exercise the real visible keyboard on an unlocked, authorized device:
.\tools\test-device.ps1 -Serial YOUR_DEVICE_SERIAL -SdkDir E:\Android\Sdk
```

The device script restores the previous input method and turns the display off in a `finally` block. Interaction tests restore the app’s settings and learned choices. Tests type only synthetic text in the debug-only editor activity. The debug activity and test code are excluded from release builds.

## Foundations and limits

`core/` owns composition, classification, phonetic lookup/segmentation, candidate ranking and commit policy. `app/` adapts that core to Android and renders the keyboard. `third_party/` contains pinned, licensed language inputs; `tools/compile_dictionary.py` deterministically generates the shipped assets. There are no acceptance-phrase ranking overrides.

The selectable Rime backend builds librime 1.16.1 from source and uses unmodified
Luna Pinyin/Essay vocabulary. Its own learning and logging are disabled; each
query uses an isolated session. Space uses complete-input candidates; explicit
prefix choices carry a consumed length and preserve the remaining spelling.
Original MinIME candidates remain available as alternatives and fallback. Full
source archives, licenses and model reproduction are documented in
[the Rime foundation](third_party/rime/README.md). English and Zhuyin decoding continue to use MinIME's existing implementation.

The dictionary is augmented with attributed offline context counts. English has optional one-edit correction, contraction alternatives, deferred completion spacing, double-Space punctuation, editor-driven capitalization and geometric word tracing. These are limited models; Chinese sentence and initial-only ranking remain substantially weaker than Google Zhuyin on the reviewed conversational probes. English/Pinyin ambiguity still requires a candidate choice in some cases.

Gradle precompiles the TSV sources into a versioned binary model. The measured phone load fell from 7.38 to 2.25 seconds, with roughly 30 MB less retained heap, with a roughly 18.5 MB debug APK in version 0.3.0. Version 0.4.0 adds native Rime libraries and its model: the universal debug APK is 64.4 MB and the unsigned release APK is 39.3 MB. See [startup measurements](docs/gap-implementation/STARTUP.md). Chinese decoding runs on a coalescing worker with stale-result rejection and ordered commits.

No device-wide surrounding text is collected. Same-editor restart recovery validates only the IME-owned composing text, up to 96 characters. Cursor moves and field changes clear prediction context. Explicit choices are learned locally; optional English word-pair learning and emoji recents default off. Private fields neither read nor write personalized history.

On Zhuyin, slide down for lowercase Latin/digits and up for capitals/shifted digits. On QWERTY, slide up for a capital letter and down for the small symbol; Shift-and-tap also gives capitals. Slides commit directly. In English mode, Enter commits spelling and inserts a newline in one press. In Chinese mode, Enter first accepts composition and the next Enter inserts a newline. Hold a key to choose alternatives by tapping; hold Backspace to repeat deletion. This is an independent implementation of functional layout mappings. One-handed ergonomics, landscape, TalkBack, larger text, Chrome and messaging-app behavior need broader device validation. Cold dictionary loading occurs in the background and can take seconds; literal input stays available, and phonetic candidates appear when loading completes.

See [complete requirements review](docs/REQUIREMENTS_REVIEW.md), [verification results](docs/VERIFICATION.md), [legacy study and APK inspection](docs/LEGACY_REFERENCE.md), [implementation audit](docs/IMPLEMENTATION_AUDIT.md), [dictionary inventory](docs/dictionary-report.json), and [data attribution](third_party/README.md). The user-requested `shine_aac` guidance informed the core-first, source-based, visible-test workflow.
