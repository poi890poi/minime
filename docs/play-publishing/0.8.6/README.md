# MinIME 0.8.6

Package `app.minime.keyboard`, version code 35. This local release packages the
approved Plex 26/18 portrait typography and unreadable-candidate filtering since
0.8.5. Existing settings and QWERTY/joined KALQ choice are preserved.

Upload `MinIME-0.8.6-play-signed.aab` from the Google-Play ZIP to Play Console.
The APK ZIP contains `MinIME-0.8.6-release.apk` for direct installation. Both are
signed with the existing upload key. The upload certificate may differ from the
Google Play app-signing certificate or a debug installation; only compatible
signing identities permit an in-place update. No Play submission is performed.

The release notes file uses Play Console's `<en-US>` / `<zh-TW>` blocks, each
under 500 characters, and UTF-8 with BOM for Windows compatibility. The archive
contains the binary, public certificate, notices, metadata and checksums. No
private keys, personal backups or experimental font assets are distributed.

Release preparation audit: bump the version/code to distinguish this upload;
build and sign locally with the existing protected key; verify manifest identity,
signatures and certificates, packaged assets/font licence, and native alignment.
Run shared core and pinned desktop checks before the release build. Carry forward
the seven successful typography/interaction device checks from commit 8075af5;
this preparation changes version metadata only. Do not repeat phone operations
unless verification finds an Android integration issue. Package explicit public
files, then verify the complete downloads through a temporary Cloudflare tunnel.
No GitHub CI or Actions.

Existing release gates remain open: UD-derived underlying-text rights review in
`sources/release-policy.json`, and runtime verification on a 16 KB-page device.
ELF/ZIP alignment passes do not certify that runtime behavior. This package is
prepared for review, not a declaration that all publication gates are cleared.

Font: unmodified IBM Plex Sans Condensed Regular under SIL OFL 1.1. Its licence
is included in NOTICE.txt. The 26/18 styling is for normal portrait text scale;
enlarged text uses a fitted arrangement and short landscape rows retain their
previous compact styling. Touch geometry and language dictionaries are unchanged.

Privacy policy:
https://github.com/poi890poi/minime/blob/codex/close-ime-gaps/PRIVACY.md

Official release instructions:
https://support.google.com/googleplay/android-developer/answer/9859348?hl=en
