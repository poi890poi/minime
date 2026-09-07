# Device verification notes

Only RFCR91GWXLX, the authorized Samsung phone, is used. The wrapper restores
the prior IME/preferences and sleeps the display after every exit, including
installation and test failures. Explicit power checks confirmed Dozing.

The first targeted build used `android.injected.build.abi=arm64-v8a`. AGP writes
these test-only APKs under `app/build/intermediates/apk`, while the installer had
hardcoded `app/build/outputs/apk` and installed the previous APK. Thus the new test
class was absent. This was initially suspected to be an incremental packaging
problem; inspection of the Gradle artifact redirect identified the actual cause.
A clean build removed the stale standard APK, making the path mismatch explicit.

The wrapper now accepts explicit AppApk/TestApk paths and uses Android's `-t`
flag for debug/test-only APKs. Byte inspection verified RimeBackend and
RimeIntegrationTest in the selected APKs before the actual native test.
The first real Rime run passed visible phrase commits, punctuation, language
switching, literal recovery and backend fallback. The bulk probe test stopped on
an absent evaluation asset: the existing androidTest source-set override excluded
src/androidTest/assets. That path is now explicitly included.

These failed setup attempts are not counted as engine-quality failures or passes.
Final phone results are recorded separately after the corrected build.
