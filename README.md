# MinIME 注音

An offline Android IME with Pinyin and English as primary layouts, plus Taiwan Zhuyin. Chinese mode also supports mixed English. This repository implements the functional prototype in the product specification. It does **not** claim verified equivalence to legacy Google Zhuyin or production language-model quality.

The [September 7 comparative review](docs/GOOGLE_MINIME_GAP_REVIEW.md) covers 90 paired Google/MinIME scenarios, targeted rechecks, screenshots, and prioritized remaining gaps. Production behavior was held unchanged during that review.

## Included

- Standard `InputMethodService`, Android 10+ (min 29, target/compile 35).
- Three-row staggered QWERTY and four-row Taiwan Zhuyin, with Latin/symbol slide hints and bottom-row ㄦ.
- Visible EN / 中 key: one tap switches between English and your chosen Chinese layout. English offers case-preserving word completions; Space keeps typed spelling unless you tap a suggestion.
- Double-tap Shift for Caps Lock; tap again to unlock. Upward letter slides enter capitals.
- Immediate Chinese/English punctuation, categorized symbols, and 3,010 Unicode emoji sequences including skin tones, families and flags.
- Exact raw candidate in a fixed left slot; separate highlighted Space choice.
- Incremental token intent: Chinese, English/ambiguous Latin, URLs/email, identifiers and alphanumeric text.
- English Space commits U+0020; Chinese Space accepts a candidate. On an unfinished Zhuyin syllable, Space first adds first tone. Another Space after commitment inserts U+0020.
- Taiwan vocabulary, full/abbreviated Pinyin syllables, phrase segmentation, English completions, contextual Chinese continuations and explicit local choice learning.
- Editor actions, composition deletion, cursor invalidation, private/password/numeric/email/URL field policy.
- No Internet permission, input logging, telemetry, cloud dependency or backup of learned data.

## Build and run

Install JDK 17 and Android SDK platform 35. Set `ANDROID_HOME`, or create an ignored `local.properties` with `sdk.dir=E\:/Android/Sdk` on this Windows setup.

```powershell
.\gradlew.bat :core:regression :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

On Linux/macOS use `./gradlew` instead. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Release builds are unsigned until the project owner supplies their signing configuration; the reference APKs and their signing keys are never reused.

Install the debug APK, open **MinIME 注音**, enable it in Android settings, then choose it from the input-method picker. The setup screen includes a test field, local dictionary editor, learning reset and open-source notices.

Type `zhege`, Space, Space, `pronunciation`, Space, `budui`, Space to produce `這個 pronunciation 不對`. No language key is involved. Chinese homophones may need a candidate tap, such as selecting `請` for `qing`. To keep `ming` literal, tap the fixed exact-input candidate. Hold Space also commits exact input plus a space.

The onscreen comma and period accept pending composition and insert immediately: ，。 in Chinese mode and ,. in English mode. Slide up for the other width. Hold comma to open emoji; hold period for punctuation, text faces and the Chinese-mode punctuation-width preference. Tap ☺ for emoji, ?123 for symbols, and ABC to return to letters. Physical-keyboard ASCII punctuation remains with its token so URLs, paths and code retain exact spelling.

Pinyin candidates support initials and mixed syllables: `jt` / `jtian` → 今天,
`srf` / `shrf` / `srufa` → 輸入法. Tap the desired candidate; abbreviations that
could be literal text retain the exact-input Space default until an explicit local
choice is learned. Apostrophes force syllable boundaries. Continue typing or use
Backspace to refine the same composition. Candidate pages retain alternatives
when a spelling such as `sh` can represent one syllable or two initials.

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

The source dictionary is a unigram baseline. Local learning uses recent Chinese context, but the app does not yet have a contextual statistical sentence model, typo correction or word-tracing gestures. Device-wide surrounding text is not collected, so predictions reset after cursor movement or a field change. English/Pinyin ambiguity remains a candidate-choice problem in some cases; raw input stays recoverable.

On Zhuyin, slide down for lowercase Latin/digits and up for capitals/shifted digits. On QWERTY, slide up for a capital letter and down for the small symbol; Shift-and-tap also gives capitals. Slides commit directly. In English mode, Enter commits spelling and inserts a newline in one press. In Chinese mode, Enter first accepts composition and the next Enter inserts a newline. Hold a key to choose alternatives by tapping; hold Backspace to repeat deletion. This is an independent implementation of functional layout mappings. One-handed ergonomics, landscape, TalkBack, larger text, Chrome and messaging-app behavior need broader device validation. Cold dictionary loading occurs in the background and can take seconds; literal input stays available, and phonetic candidates appear when loading completes.

See [complete requirements review](docs/REQUIREMENTS_REVIEW.md), [verification results](docs/VERIFICATION.md), [legacy study and APK inspection](docs/LEGACY_REFERENCE.md), [implementation audit](docs/IMPLEMENTATION_AUDIT.md), [dictionary inventory](docs/dictionary-report.json), and [data attribution](third_party/README.md). The user-requested `shine_aac` guidance informed the core-first, source-based, visible-test workflow.
