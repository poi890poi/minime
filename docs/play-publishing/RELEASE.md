# 0.8.0 publishing preparation

Status: local preparation complete; not submitted or cleared for Google Play.
Original code is now Apache-2.0. The earlier PLAN.md is the historical proposal;
this file records the implemented state and remaining work.

## Verified on September 10, 2026

| Check | Result | Limit |
| --- | --- | --- |
| Shared core | 35,984 assertions passed | Behavioral checks, not language accuracy |
| Pinned desktop Rime | 628 uncached queries; p50 5.003 ms, p95 10.776 ms | IPC + decoder only; not phone touch latency |
| Source contracts | 19 tests and source ledger passed | Does not resolve underlying-text rights |
| Android build | Debug, test APK, release AAB and debug/release lint passed | Lint reports 0 errors and 12 warnings |
| Phone integration | 23 tests passed: 8 backup, 1 settings, 7 paired-candidate, 7 editor | Android 13; codec/restore and UI entry points, not full document-provider migration |
| Bundle | bundletool 1.18.0 validates; target 36, non-debuggable, no requested permissions, automatic backup disabled | AAB is unsigned |
| Native packaging | Four ABI libraries have compatible 16 KB ELF segments; bundle config PAGE_ALIGNMENT_16K; universal APK zipalign passes | Android 16 / 16 KB runtime remains untested |
| Device cleanup | Preferences restored, Samsung Honeyboard restored, display OFF verified | Only RFCR91GWXLX used |

The release inventory records all 24 packaged assets and four native libraries,
including byte hashes, source pins and open rights items. Native symbols are
included in the bundle metadata. Dictionary data and ranking were not changed.
The checker initially missed nested OpenCC assets; recursive inventory fixed the
checker. Bundletool's classpath distribution needed an explicit installed aapt2.

Local evidence is under `artifacts/play-prep/`: core.txt, desktop.txt, build.txt,
rebuild.txt, device.txt, manifest.xml, bundle-config.json, zipalign.txt,
signature.txt, release-inventory.json, and source-distribution/manifest.json.
Do not publish the entire artifacts directory: device backups are private.

## Required before internal Play distribution

1. Resolve `sources/release-policy.json`: review the exact UD-derived outputs,
   obtain the necessary permission, or evaluate a replacement dataset. This is an
   open question about underlying texts, not a conclusion that all UD reuse is
   prohibited. Keep annotation licensing separate from text rights.
2. Supply the Play account type/status, publisher name and support email; confirm
   `dev.minime.ime` as the permanent application ID. Free/offline/no ads is the
   approved launch direction. No account, key or Console app has been created.
3. Configure an upload key and Play App Signing. Build and verify a signed AAB;
   confirm certificate fingerprints in Console. Keep recovery copies securely.
4. Publish `site/` to stable HTTPS hosting after inserting the support contact.
   GitHub Pages settings could not be accessed because the browser is signed out.
   Use a Pages deployment publishing only `site/`, not the repository root.
   Verify the public privacy HTML without login before entering its URL in Play.
5. Perform Android 16 / 16 KB runtime tests and full document-picker export/import,
   cancellation, process-restart and migration checks. Test release delivery through
   an internal track, clean install, Chrome, passwords, TalkBack, larger text and
   rotation. Do not uninstall an existing debug build until its backup is verified.
6. Measure release typing/touch latency and hit rate against PRODUCT_REQUIREMENTS.md;
   report conversation/essay coverage separately. Existing desktop results do not
   close the previous phone latency gap. Prepare genuine app screenshots, the store
   icon and feature graphic; complete the listing worksheet and Console declarations.

Only then proceed to closed testing and production access under the requirements
shown for the owner's account. The personal-account testing minimum, if applicable,
does not replace actual feedback or Google's access review.

## Build and signing

Use JDK 17, SDK platform 36, NDK 27.2.12479018 and CMake 3.22.1. See
third_party/rime/README.md for fetching pinned native sources. Run core and desktop
checks before the Android build. Use `gradlew.bat :app:bundleRelease`.

Configure these four environment variables through a local secret mechanism:
`MINIME_UPLOAD_STORE_FILE`, `MINIME_UPLOAD_STORE_PASSWORD`,
`MINIME_UPLOAD_KEY_ALIAS`, `MINIME_UPLOAD_KEY_PASSWORD`.
Never put their values in a command transcript, repository file or build log.
Without them the output is unsigned. Partial configuration fails clearly;
there is no release fallback to the debug signing key.

For local bundletool inspection, `gradlew.bat -q printBundletoolClasspath` returns
the cached AGP classpath. Invoke
`com.android.tools.build.bundletool.BundleToolMain` with Java and that classpath:
`validate --bundle=...`, `dump manifest --bundle=...`, and `dump config --bundle=...`.
To inspect an APK, use `build-apks --mode=universal --aapt2=<SDK aapt2> ...` and
`zipalign -c -P 16 -v 4 universal.apk`. Local inspection APKs may be debug-signed;
they are not production artifacts. Check the AAB signature separately with
`jarsigner -verify -verbose -certs` and the expected upload certificate.

Run `python tools/verify_release_bundle.py --aab <bundle> --manifest <dumped XML>
--output <inventory JSON>`. Its `--strict` option intentionally fails: this tool
is an inventory and packaging check, not a replacement for the open release ledger.
Do not use signature-block presence alone as proof of a valid signature.

## Source distribution

`python tools/package_release_sources.py --output <directory>` creates fixed-time,
hashed ZIPs for runtime sources and independently reusable POJ, Japanese, Taiwan,
and ODbL geography data. Draft archives are local until rights review is closed.
Publish the final archives alongside each immutable release tag; retain the Rime
source archives, patches and reproduction instructions, not only compiled data.

The runtime ZIP contains editable application/model inputs and build tools. It
omits evaluation corpora, private artifacts, native toolchains and downloaded native
build trees. Fetch pinned native archives with tools/fetch_rime_sources.py. Full
source-framework audits and holdout evaluations use the repository and their own
licensed inputs; the runtime ZIP is not the complete research environment.
Update the inventory and archives after final signing or any source/asset change.

## Migration behavior and limits

Settings now provides explicit export/import through Android's document picker.
Exports are plaintext JSON and may contain personal words; the selected provider
may be a cloud service. Import validates size, types and record bounds before a
Replace confirmation. Cancel leaves preferences untouched. Recoverable write
failure restores both old preference files. A process/device failure between two
file commits is not crash-atomic; retain the exported copy until migration has
been checked. No automatic export, network permission or persistent URI grant is
added. Restart/select the IME in a new editor after import to reload preferences.

Official references checked September 10, 2026:
[target API](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en),
[16 KB checks](https://developer.android.com/guide/practices/page-sizes),
[Data safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).
