# MinIME 0.8.5 release preparation

Package `app.minime.keyboard`, version code 34. QWERTY remains the default.
Enable **Settings → Use joined KALQ letter layout** for Chinese Pinyin, Taiwanese,
Japanese and English. Numbers and dedicated Zhuyin retain their layouts.

The signed AAB is the Google Play upload artifact. The signed APK is for direct
installation; the APK ZIP contains the same APK. An installation signed with a
debug certificate cannot be upgraded in place with this upload-key-signed APK.
Use the app's settings/dictionary export before replacing such an installation.
The Play app-signing certificate can differ from the upload certificate; only
matching signing identities support an in-place update.

Joined KALQ keeps both internal Space keys and puts Shift/Backspace at the lower
right. Slide up for capitals, down for the printed symbol. Digits follow visual
order: 1–8 on the top row, then 9 and 0 on the next row. Its four letter rows fit
the existing keyboard height. Whole-word tracing remains QWERTY-only. Symbol/emoji
pages, mode switching, literal acceptance and dictionary/correction settings retain
their existing behavior. This release does not ship the experimental spatial decoder.

This release also includes changes since 0.8.4: attested Chinese candidate handling,
first-syllable choices alongside phrase matches, the requested fresh-install
settings defaults, and preserved letter/Space/Backspace contact order during
overlapping two-thumb typing. See the repository history for their separate commits.

Build and verify locally, using the existing upload key. Run core and desktop Rime
checks before Android builds. Test only the explicitly reserved RFCR91GWXLX phone
through `tools/phone-lease.ps1`; restore preferences/IME and verify display OFF.
No GitHub Actions and no Google Play submission are part of this preparation.

Release notes are under `en-US.txt` and `zh-TW.txt`; the packaged copies use UTF-8
with a BOM for Windows compatibility. Each language is below the Play Console
500-character limit. The ZIP includes public notices and checksums, not keys or
personal backups. Source/data archives are offered separately to avoid inflating
the APK download.

Known release gates are preserved: `sources/release-policy.json` still records the
UD-derived text rights review. Native ELF/ZIP alignment checks do not certify
runtime behavior on a 16 KB-page device. This package preparation does not close
those gates or establish human typing-speed/latency acceptance.

Official references checked for this preparation:

- [Create a Play release and supply an app bundle and localized notes](https://support.google.com/googleplay/android-developer/answer/9859348?hl=en).
- [Android 16 KB page-size compatibility and verification](https://developer.android.com/guide/practices/page-sizes).

Privacy policy: https://github.com/poi890poi/minime/blob/codex/close-ime-gaps/PRIVACY.md
