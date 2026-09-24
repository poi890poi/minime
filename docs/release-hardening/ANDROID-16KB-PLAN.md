# Prepared Android 16 / 16 KB runtime check

September 24, 2026. Build/platform validation only. The authorized physical phone
is Android 13 with 4 KB pages, so it cannot establish Android 16 or 16 KB native
runtime compatibility. Existing static ELF/APK alignment passes are necessary
but do not replace loading and using each native engine on the target runtime.

Official SDK inventory checked locally: package
`system-images;android-36;google_apis_ps16k;x86_64`, revision 7, is available.
Current installed images contain Android 34 only. Android Emulator 36.6.11 is
installed. No additional image, AVD or emulator was created by this preparation.

Proposed bounded local validation:

1. Install the official API 36 16 KB x86_64 image into a workspace-owned SDK/AVD
   directory. Record package revision, installed metadata and downloaded hashes.
2. Create a dedicated disposable AVD named minime-release-api36-16k. Launch it
   headlessly on a fixed, unused emulator port with snapshots and audio disabled.
   Address every ADB call to that emulator serial; never use an unqualified target.
3. Fail the platform gate unless ro.build.version.sdk reports 36 and
   `getconf PAGE_SIZE` reports 16384. Record architecture/build and page size.
4. Install the final release-equivalent payload plus matching instrumentation.
   Validate four-mode input/selection, Rime native loading, Japanese conversion,
   editor lifecycle and private fields. Verify an upgrade from the previous
   locally signed payload preserves preferences and learning. This is a local
   upgrade, not Play-generated delivery or the Play signing chain.
5. Stop only the created emulator in a finally block and retain logs/screenshots,
   APK hashes and test results. Keep data directories for diagnosis; remove them
   only after verified workspace-path resolution if cleanup is requested.

This must not be used for performance comparisons with the physical phone or
for human hit-rate certification. An x86_64 emulator does not establish native
ARM64 behavior on a 16 KB physical device; report the ABI and limitation.

Authorization boundary: AGENTS.md currently says “Test only the authorized phone
RFCR91GWXLX.” A specific exception for this local emulator is needed before
launch/install/test operations. Preparing the inventory and procedure does not
override that rule. Phone coordination remains separate and its reservation has
already been released. No Play upload, account action or GitHub CI is involved.
