# Legacy Google Zhuyin reference study

Live study: 2026-09-06, Samsung SM-G781B / Android 13 / RFCR91GWXLX.
Installed version: 2.4.5.164561151-arm64-v8a, code 2451413. Tested in
MinIME’s local debug EditText, with native touch taps and slides. Every sequence
starts in a cleared synthetic editor. The previous keyboard was restored and the
phone put to sleep after each study run.

## Observed behavior

The complete keystroke/editor/candidate record is [legacy-observations.json](legacy-observations.json).
It contains 25 sequences. Original full keyboard-node records and screenshots are
under ignored `artifacts/legacy-study`; selected images are in `docs/evidence`.
The study does not establish behavior in all editors or under all settings.

| Category | Observed sequence and result |
| --- | --- |
| Pure Zhuyin | ㄋㄧˇㄏㄠˇ stays in the keyboard’s own composition area; Space commits 你好; another Space inserts U+0020. |
| Pure English | Zhuyin downward slides spell meeting directly in the editor, one letter per slide. |
| Chinese → English | Active ㄋㄧˇ followed by a downward m slide commits 你m. |
| English → Chinese | pronunciation + Space followed immediately by ㄅㄨˊㄉㄨㄟˋ + Space produces pronunciation 不對. |
| Repeated transitions | The recorded sequence produces 你m 你U 你 without using a language toggle. |
| English spaces | Literal slides spelling meeting followed by two Spaces produce `meeting  `. |
| Capitals | Zhuyin upper/lower slides produce GitHub exactly. |
| Acronym | Zhuyin upward slides produce USB. |
| Numbers | Zhuyin slide entry produces `3pm USB3 `. |
| URL | Pinyin literal slides plus dot/slash alternatives produce `github.com/minime `. Scheme/colon entry was not included in this probe. |
| Email | Pinyin literal slides plus @/dot alternatives produce `name@example.com `. |
| Punctuation | Downward ？ after active ㄋㄧˇㄏㄠˇ commits 你好？. |
| Ambiguous Latin | Pinyin taps ming + Space produce 名; upward literal slides produce `ming ` instead. |
| Editing committed text | After committing 你, moving to position zero and sliding a + Space produces `a 你`. Composition Backspace removes the tone and then the latest symbol. Reference committed-range deletion remains unmeasured. |

Additional state distinctions:

- Zhuyin has four rows of ten, with ㄦ in the bottom row. Downward slides enter
  Latin lowercase/digits; upward slides enter capitals/shifted digits.
- Pinyin is three-row staggered QWERTY. Upward slides directly commit lowercase
  letters, including upward sliding Q after Shift; downward q commits 1.
  Shift-and-tap provides capitals. Slides use a different commit path from taps.
- An unfinished Zhuyin syllable uses Space for first tone. ㄓㄨ + Space remains
  uncommitted and offers first-tone candidates such as 朱/豬. The next Space accepts.
  ㄋㄧ + Space + Space ranked 內衣 in this particular source model/state; that is
  an observed decoder result, not a ranking target hardcoded into MinIME.
- In the multiline editor, first Enter accepts ㄋㄧˇ as 你 or nihao as 你好;
  second Enter inserts the newline.
- With normal Pinyin taps, meeting + Space committed `meeting` **without a space**
  in this tested configuration. Tapped pronunciation also required a separate
  space. Explicit literal slides followed by Space did insert U+0020.
- Both Zhuyin and Pinyin tap composition remained in the keyboard’s own panel;
  the editor had no composing span until commitment. Literal slides appeared in
  the editor immediately with no composing span.

## MinIME compatibility decisions

Implemented from the observations: layout structure, direct literal slide events,
Zhuyin capital/number alternates, Pinyin literal slide commitment, first-tone Space,
first-tone syllable boundaries, explicit Taiwan punctuation after composition, and
commit-before-newline in ordinary text. First-tone boundaries are derived from
McBopomofo source readings, not from Google’s model. Search/Go/etc. still commit
and dispatch the editor action directly; their reference equivalents were not tested.

The PRS explicitly requires English token + Space to insert U+0020, so MinIME
retains that behavior for ordinary English taps even where the tested legacy
configuration differs. It also retains the stable exact-input slot and Android
inline composing text. Pinyin upward slides now produce **capitals**, as explicitly
requested by the user, overriding the lowercase result observed in legacy Pinyin.
Pinyin initials and mixed syllables now have a separate decoder, verified against
39 more live sequences; see [Pinyin results](PINYIN_PREDICTION_RESULTS.md).
Reference-style composition presentation, broader ranking quality and exhaustive editing behavior
remain compatibility gaps, not completed claims.

## Study controls and negative results

`tools/study-legacy.ps1` invokes only `LegacyStudyTest`; the normal MinIME suite
explicitly excludes that class. The initial coordinate replay changed into a
layout picker/Cangjie after the first case, so those later records were rejected.
The final study resolves each key from the live accessibility tree and fails if
it is missing. The Space label changes between 阴平 and 空格; both were observed
and handled. These corrections affect the study harness, not the reference app.

No reference executable, decompiled implementation, model or artwork is packaged
in MinIME. Keyboard accessibility is used by tests only, never to implement input.

## Supplied APK inspection (2026-09-06)

Both user-supplied files were inspected with Android SDK 35 `aapt` and `apksigner verify --print-certs`:

| File source label | Version | Code | APK SHA-256 |
|---|---|---|---|
| APKMirror arm64-v8a | 2.4.5.164561151-arm64-v8a | 2451413 | 5f6dc14d26194a29abfb3da71688b76417115054f078d22b1e13f512305a0dc3 |
| APKPure armeabi-v7a | 2.4.5.164561151-armeabi-v7a | 2451412 | 803686bbcee5aaf9b13d89582ac354468c3abed3071ea8d7ca86dcc18210fed2 |

Both identify package `com.google.android.apps.inputmethod.zhuyin`, min SDK 17, target 26, and the same certificate SHA-256 `3d7a1223019aa39d9ea0e3436ab7c0896bfb4fb679f4de5fe7c23f326c8f994a`. Signature verification succeeds with a warning that a META-INF protobuf service entry is not protected by the JAR signature. Matching certificates and self-reported metadata do not independently establish official distribution or complete integrity.

The user authorized testing on the Samsung SM-G781B (Android 13). Installing the arm64 APK returned `INSTALL_FAILED_VERIFICATION_FAILURE: Install not allowed`. No verification settings were disabled and no installation bypass was attempted. The user subsequently installed and enabled the arm64 reference successfully; the live study below supersedes that initial installation limitation. The APKs remain user files, excluded from Git and from MinIME distribution. No proprietary code or assets are incorporated.

## Published source versus the supplied application

Google's [2010 open-source announcement](https://opensource.googleblog.com/2010/04/open-sourcing-traditional-chinese-ime.html) links an Apache-licensed Android Zhuyin/Cangjie implementation. Its [Google Code source archive](https://storage.googleapis.com/google-code-archive-source/v2/code.google.com/android-traditional-chinese-ime/source-archive.zip) was downloaded and inspected (archive SHA-256 `2321007be28c4c0db2687bebd11b3d1978123bab90641a19b6a3b038b2a46d60`). The trunk manifest identifies `com.googlecode.tcime`, version 1.0/code 113, target API 4. That is not the package/version of the supplied 2.4.5 app. Its editor abstraction and state-dependent Space are useful historical evidence, but cannot establish the later application's mixed-language behavior.

Google also publishes [Chromium OS input tools](https://github.com/google/google-input-tools), including Zhuyin. Those are a separate product and were not substituted for the Android reference.

## Static analysis of the arm64 APK

JADX 1.5.6 analyzed 1,794 classes and reported two decompilation errors. This is partial reverse engineering, not recovery of the original source. Analysis outputs stay under ignored `.tools/legacy-jadx` and are not packaged or committed.

Verified structural findings:

- The APK contains distinct `ZhuyinHmmDecodeProcessor` and `PinyinHmmDecodeProcessor` classes sharing `AbstractHmmChineseDecodeProcessor`.
- Pinyin creates a decoder identified as `zh-hant-t-i0-pinyin` and attaches user dictionaries.
- Space and Enter are dispatched through composition-dependent decoder paths. While composing, apostrophe is explicitly sent as a token separator.
- Shared Chinese commit logic distinguishes original and converted text and records choices in separate English/Chinese dictionary paths. Enter is explicitly distinguished from other commit triggers.
- `ChineseAutoSpaceProcessor` checks ASCII letters and a user preference; double-space handling is a separate processor.
- The engine factory and decoding wrappers contain JNI/native entry points. Decompiled Java alone does not recover native ranking, complete classification rules, or model data behavior.

These findings support a shared composition/commit layer with replaceable decoding and explicit raw-text recovery. They do not prove what any test string will produce in a running legacy IME. No decompiled implementation or proprietary model has been copied into MinIME.


## Static functional resource evidence

Independently implemented mappings were cross-checked against
`keymapping_body_zh_tw_zhuyin_4x10.xml`, `keyboard_prime_bottom_zhuyin.xml`,
`keyboard_qwerty_input_area.xml`, `softkeys_input_zh_tw_zhuyin.xml`,
`softkeys_input_zh_pinyin_qwerty.xml`, and `softkeys_input_zh_tw_pinyin_qwerty.xml`.
The special Zhuyin downward mappings are ㄤ→：, ㄝ→…, ㄡ→！ and ㄥ→？;
these keys have no upward action, and ㄦ has neither slide action.

Replay plans are saved under `docs/legacy-study/`. For example, after enabling the reference keyboard, run `tools/study-legacy.ps1 -Serial RFCR91GWXLX -SdkDir E:\Android\Sdk -Plan docs/legacy-study/zhuyin-plan.json`. These plans were observed on the specified phone/version and stop on a missing key; they are not a cross-device certification suite.
