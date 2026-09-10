# MinIME — Google Play publishing materials

Start here. This ZIP contains store materials, not a signed app release.
Nothing has been submitted to Google Play.

## Copy into the Console

1. Choose your primary listing language; zh-TW is suggested for the Taiwan focus.
   Add en-US as the other localization. For each language, paste the contents of
   listing/<locale>/title.txt, short-description.txt and full-description.txt.
   Release notes are in a separate file for the test-track release form.
2. Upload graphics/icon-512.png as the app icon for either locale.
3. Upload graphics/feature-zh-TW.png or feature-en-US.png as the localized feature
   graphic. The SVG files are editable sources; upload PNG files to Play.
4. Upload screenshots/01-chinese.png through 04-japanese.png in that order.
   These are actual MinIME 0.8.0 keyboard captures at 1080x1920. The upper Notes
   area is a debug-only example editor, not an advertised MinIME notes feature.
5. Use alt-text.json for concise English/Traditional Chinese image descriptions.
6. Read review/STORE-LISTING.md for the proposed Data safety answers and reviewer
   access instructions. Complete the actual Console questionnaires truthfully.

## Privacy and publisher details

privacy/privacy.html is the public HTML policy; privacy/privacy.txt matches the
in-app policy. Host the HTML at a permanent, public HTTPS URL and verify it works
without signing in. The temporary download tunnel for this ZIP is not suitable
as a permanent privacy URL. Add the publisher's support email when provided.

Still needed from the owner: publisher display name, support email, Play account
status/type and confirmation of the permanent application ID dev.minime.ime.
Complete target-audience and content-rating questionnaires in Console; this kit
contains no invented IARC rating or age declaration. Launch direction: free,
offline, no advertising, no login or subscription.

## App-release blockers

The materials do not clear the outstanding UD-derived text-rights review, signing,
Android 16 / 16 KB runtime validation, release touch-latency/hit-rate checks, or
full debug-to-production backup/import verification. See review/RELEASE-CHECKLIST.md.
There is deliberately no unsigned AAB or private device backup in this ZIP.
The existing local AAB is a preparation artifact and must not be uploaded as-is.

## Provenance and reuse

Brand: MinIME (minimal + IME, and mini-me). The icon retains the existing Android
vector geometry and teal color. Feature graphics are original typographic/vector
artwork, with no Google assets, fake UI, store badges or ranking claims. Fonts are
rendered using Segoe UI and Microsoft JhengHei; font binaries are not distributed.
Editable SVG rendering may vary if these fonts are unavailable.

Screenshots: unchanged main app source at d5876e0, debug build 0.8.0; captured on
September 10, 2026 from the actual MinIME service in a clean example editor.
Authored samples: mingtian, hello, liho and arigatou. These examples are marketing
demonstrations, not train/dev/holdout corpora or evidence of language accuracy.
No candidate text was replaced or rearranged. The phone's preferences, original
IME and display size were restored; cleanup verified display OFF. Earlier captures
were rejected after phone contention; only the final identity-checked run is used.

Original code/artwork/copy retains Apache-2.0; imported language resources retain
their separate terms. Source and complete notices:
https://github.com/poi890poi/minime/tree/codex/close-ime-gaps
See review/NOTICE.txt and review/LICENSE as well. Do not infer third-party data
clearance from the original-artwork license.

## Validation

Title <=30 characters, short description <=80, full description <=4000, release
notes <=500. Icon: 512x512 RGBA PNG under 1 MB. Feature graphics: 1024x500 RGB PNG.
Four phone screenshots: 1080x1920 RGB PNG. Every payload file is listed with a
SHA-256 hash in MANIFEST.json. Graphics and screenshots were visually inspected.

Current specifications checked September 10, 2026:
https://support.google.com/googleplay/android-developer/answer/9866151?hl=en
https://developer.android.com/distribute/google-play/resources/icon-design-specifications
https://support.google.com/googleplay/android-developer/answer/9859152?hl=en
