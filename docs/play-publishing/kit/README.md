# MinIME 0.8.4 — Google Play materials

REVIEW ONLY: candidate-ranking defect found in geography screenshot; do not publish
these images or treat this kit as production approval.

Start with review/RELEASE-CHECKLIST.md and review/CONSOLE-ANSWERS.md. The separate
signed AAB is the Play upload binary; APK is for sideload review. This ZIP contains
public listing materials. Nothing has been submitted to Google Play.

1. Paste listing/<locale>/title.txt, short-description.txt and full-description.txt
   for zh-TW and en-US. Release notes are separate. TXT uses UTF-8 with BOM for
   Windows compatibility. Paste the visible text into each corresponding field.
2. Upload graphics/icon-512.png and each locale's feature graphic PNG. SVGs are
   editable sources. The approved bamboo-leaf hat/cream face uses teal #006765.
3. Upload five screenshots in numeric order. The Notes area is an example editor,
   not a MinIME notes feature. Image descriptions are in alt-text.json.
4. Use the permanent privacy URL below. It returned HTTP 200 without login on
   September 12, 2026. Included disclosures match the app's offline privacy text.
5. Complete owner contact/account/age/rating fields using the Console worksheet.

Privacy: https://github.com/poi890poi/minime/blob/codex/close-ime-gaps/PRIVACY.md

Identity: MinIME — Pinyin for Taiwan / MinIME — 台灣多語拼音鍵盤.
Package app.minime.keyboard; version 0.8.4 (32). The old dev.minime.ime package is
separate. Export old settings/learning before uninstalling; import into the new app.

Screenshots were captured September 12, 2026 from the current 0.8.4 debug build
using unchanged production keyboard code. Inputs: mingtian, hello, liho, arigatou,
jianianduan. 加年端社 appears among actual geography candidates. No candidate text
was fabricated or rearranged. These authored demonstrations are not quality
benchmarks or training data. The first two capture attempts failed during IME
startup and were excluded; the final identity-checked capture passed. Original
preferences, IME and viewport were restored and display OFF verified.

Five screenshots: 1080x1920 RGB PNG. Icon: 512x512 RGBA PNG under 1 MB. Features:
1024x500 RGB PNG. Text limits: title 30, short 80, full 4,000, notes 500 characters.
MANIFEST.json records every public payload's size and SHA-256.

Original code/artwork/copy is Apache-2.0. Imported dictionaries retain separate
terms: review/LICENSING.md, NOTICE.txt and LICENSE. No Google artwork or proprietary
code is included. Five source/data archives are offered separately; they do not
clear the outstanding UD-derived rights question. Signing secrets and personal
backups are never included. Runtime and account gates remain in the checklist.

Repository: https://github.com/poi890poi/minime/tree/codex/close-ime-gaps
Graphics specifications: https://support.google.com/googleplay/android-developer/answer/9866151?hl=en
