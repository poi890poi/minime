# Publishing preparation: impact and verification

Authorized by the owner's approval of the publishing plan on 2026-09-10.

Independent slices:
1. Licensing/tooling: Apache-2.0 for original work, separate third-party terms,
   current source notices, reproducible release inventory/source-data archive.
   No production vocabulary changes. UD underlying-text review remains explicit;
   a release readiness check must fail rather than silently declaring clearance.
2. Privacy/migration feature: explicit local document export/import of preferences
   and learned data; bounded validated input; cancel leaves data unchanged. Never
   export automatically. Explain plaintext/user-chosen storage and deletion.
   Public HTML privacy text plus offline in-app text must agree with behavior.
3. Build/platform: target/compile API 36, environment-only release signing, no
   debug signing fallback. Verify release AAB manifest, permissions, packaged
   sources and all native segment alignment. No signing secrets in source/logs.
   Retain package id for now and provide migration before any debug uninstall.
4. Submission preparation: listing/reviewer/data-safety drafts and readiness ledger.

Relevant risks: import can overwrite personal data; reject malformed/oversized or
wrong-type fields before changes, confirm replace only after validation, and roll
back failed writes. IME processes may hold preferences: reload after file import
and validate visibility in integration tests. API 36 changes window insets/back
behavior; use current APIs where needed, preserve Android 10 compatibility.
Licensing text cannot relicense third-party data. Bundling disabled data still
counts as distribution. Public release needs closed rights and signing gates.

Verification: source-contract/asset checks, existing core and pinned desktop tests
before Android builds, semantic backup tests, Android UI/insets and import/export
integration on authorized RFCR91GWXLX, APK/AAB packaging and ELF alignment checks.
Record unavailable API 36/16 KB runtime testing honestly; the authorized Android 13
phone cannot certify either. Restore IME/settings/learning and verify display OFF.
