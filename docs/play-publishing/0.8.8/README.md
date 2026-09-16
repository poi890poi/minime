# MinIME 0.8.8

Package `app.minime.keyboard`, version code **38**.

Upload `MinIME-0.8.8-play-signed.aab` from the Google-Play ZIP to Play Console.
The separate APK ZIP contains `MinIME-0.8.8-release.apk` for direct installation.
Both use the existing upload key. Settings and layout choices are preserved.
No Play submission is performed by this packaging task.

Changes since 0.8.7: fix the portrait font-scale fallback that replaced the
approved 26/18 IBM Plex labels with smaller stacked labels above scale 1.05.
QWERTY and joined KALQ now retain the approved style at all portrait font scales,
fitting the pair to the available key bounds. Hit areas, mappings, candidates
and landscape behavior are unchanged.

The notes file is ready to paste into Play Console, with `<en-US>` and `<zh-TW>`
blocks, each under 500 characters. It uses UTF-8 with BOM for Windows compatibility.
The ZIP includes the signed AAB, public certificate, notes, notices, source revision,
metadata and checksums. It contains no private signing files or personal data.

Release audit: increment the upload code, build/sign/lint locally, verify signatures,
existing signer identity, manifest version, assets, native alignment and ZIP contents.
Reuse the completed core/desktop checks from 0.8.7 because no core, decoder or data
changes follow that release. The font fix passed 168 native portrait configurations
(seven font scales, three widths, two layouts, two cases, two punctuation sets),
eight independent reference renders and KALQ gesture/height/overlap checks.
See `docs/keyboard-typography.md` for the causal failure and verification sessions.
These are visual/mechanism checks, not human typing-efficiency measurements.
No GitHub CI or Actions; no additional phone use is needed for packaging.

Publication gates carried forward: UD-derived underlying-text rights review in
`sources/release-policy.json`, and runtime certification on a 16 KB-page device.
ELF/ZIP alignment validation is separate from runtime certification. This archive
does not declare those existing publication gates cleared.

Privacy policy:
https://github.com/poi890poi/minime/blob/codex/close-ime-gaps/PRIVACY.md

The upload certificate may differ from Play's app-signing certificate or a debug
installation. In-place installation updates require compatible signing identities.
