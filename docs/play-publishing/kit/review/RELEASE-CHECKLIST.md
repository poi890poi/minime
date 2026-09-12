# MinIME 0.8.4 release checklist

Prepared September 12, 2026. No Play upload or publication has occurred.

## Completed locally

- English/zh-TW names, application ID app.minime.keyboard, version 0.8.4 (32).
- Signed release APK and AAB matching the dedicated upload certificate.
- Core: 306,303 assertions; pinned Rime: 13,014 inputs, IPC+decoder p95 5.483 ms.
  These are correctness/desktop results, not phone touch latency.
- Release lint: zero errors, 15 warnings; bundle validation, component resolution,
  four-ABI ELF and APK ZIP alignment passed. No Internet permission, debug editor
  or registration token in release. All 24 language assets unchanged from 0.8.3.
- Public repository privacy URL verified without login (HTTP 200).
- Localized listing, release notes, original icon/feature graphics, five current
  screenshots, reviewer instructions, proposed Data safety and source offerings.
- Android 13 debug integration: 26 editor/privacy/settings/backup/touch checks passed.
  Screenshot capture passed; exact results are in the repository evidence report.

## Before distribution

- Fix native sentence provenance/ranking: 家碾斷 outranks the attested partial
  name 加年端社. Current screenshots are evidence only; recapture after the fix.

- Resolve the exact UD-derived rights decision in RIGHTS-REVIEW.md; preserve all
  other source licenses, notices and data/source distribution obligations.
- Owner provides publisher name/support email and confirms account type/date,
  app creation, package registration, App Signing, target audience and countries.
- Back up the upload key AND a portable recovery credential securely off-machine.
- Complete Android 16 / 16 KB runtime validation. The authorized phone runs Android
  13; the installed emulator image is Android 14. Static alignment is insufficient.
- Complete signed-release latency/hit-rate acceptance, accessibility/large fonts,
  rotation, Chrome private/password flows, process restart and manual document-
  provider migration, including old-ID to new-ID transfer and cancel/replace.
- Complete actual Console content-rating and app-content forms; follow the account's
  testing requirement. Current debug checks are not signed-release acceptance.
- After rights clearance, process the AAB in internal testing, inspect Play-generated
  APKs and signing certificate, then closed test before production approval.

No successful build, synthetic assertion count or store kit establishes legal
clearance, Google approval or all-device compatibility. No GitHub CI is used.
