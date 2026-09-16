# MinIME 0.8.7

Package `app.minime.keyboard`, version code **37**.

Upload `MinIME-0.8.7-play-signed.aab` from the Google-Play ZIP to Play Console.
The separate APK ZIP contains `MinIME-0.8.7-release.apk` for direct installation.
Both use the existing upload key. Existing settings and layout choices are kept.
No Play submission is performed by this packaging task.

Changes since 0.8.6:

- Restore selectable English completions in Keep-style text fields while retaining
  the editor's restriction on optional automatic spelling correction.
- Preserve next-word context through validated same-field restarts. External
  edits, cursor moves and new sessions still clear context.
- Center joined KALQ's D N F V between equal-width Shift and Backspace controls.
- Update privacy wording for the bounded, temporary validation of IME-owned text.

The notes file is ready to paste into Play Console, with `<en-US>` and `<zh-TW>`
blocks, each under 500 characters. It uses UTF-8 with BOM for Windows compatibility.
The ZIP includes the signed AAB, public certificate, notes, notices, source revision,
metadata and checksums. It contains no private signing files or personal data.

Release audit: increment the upload code, run shared core and pinned desktop checks,
build/sign/lint locally, then verify signatures, existing signer identity, manifest
version, assets, native alignment and ZIP contents. Reuse the completed phone
checks for the included changes: 88 native editor observations, 18 Keep probes,
nine Chrome probes, lifecycle/ownership regressions and joined-KALQ geometry/touch
checks. These are mechanism tests, not human accuracy or speed measurements.
No GitHub CI or Actions; no further phone changes are needed for packaging.

Publication gates carried forward: UD-derived underlying-text rights review in
`sources/release-policy.json`, and runtime certification on a 16 KB-page device.
ELF/ZIP alignment validation is separate from runtime certification. This archive
does not declare those existing publication gates cleared.

Privacy policy:
https://github.com/poi890poi/minime/blob/codex/close-ime-gaps/PRIVACY.md

The upload certificate may differ from Play's app-signing certificate or a debug
installation. In-place installation updates require compatible signing identities.
