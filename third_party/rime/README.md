# Rime Pinyin foundation

MinIME builds **librime 1.16.1** from source. Native inputs, exact archive hashes
and URLs are in `native-sources.json`. Run `python tools/fetch_rime_sources.py`
before the first Android build. Sources expand into ignored `.tools/native-sources`.
The Android NDK is 27.2.12479018 and CMake is 3.22.1. Standard SDK installs work;
the build also recognizes these versions under `.tools/android-sdk`. Configure
`cmake.dir` in ignored local.properties when CMake is outside the main SDK.

The independent CMake/JNI adapter is in `app/src/main/cpp`. It builds the public
Rime engine and dependencies directly, with native logging and external plugins
disabled, static C++ runtime, and 16 KB ELF alignment. No Trime code or binaries
are included. The sole compatibility flag demotes OpenCC's deprecated C++17
iterator warning from an upstream `-Werror`; dependency source is unchanged.

Native licenses: librime BSD-3-Clause, Boost BSL-1.0, yaml-cpp MIT, LevelDB
BSD-3-Clause, Marisa's BSD-2-Clause option, OpenCC Apache-2.0, and RapidJSON's
upstream MIT/third-party terms. The complete notices are in `licenses` and
`NOTICES.txt`, also appended to the app's visible open-source notices.

## Model sources and reproduction

`data-sources.json` pins complete upstream source archives for Luna Pinyin,
Essay and Prelude, provided under LGPL-3.0 with authors and licenses retained.
MinIME's two configuration patches are next to the archives. They disable Rime
personal dictionaries, reverse lookup and script conversion filters. Dictionary
readings and frequencies are unchanged. There is no additional grammar model.
All output uses the original Traditional dictionary, without Simplified conversion.

Run `python tools/compile_rime_data.py /path/to/rime_deployer` with librime 1.16.1.
On Windows the official MSVC release's rime_deployer needs rime.dll beside it or
on PATH. The script verifies the source archives, compiles in an isolated temp
directory with fixed source timestamps, and regenerates the model
manifest and bundle identity. No evaluation inputs enter compilation. Generated
files in `app/src/main/rimeAssets/rime` are retained so ordinary Android builds
need no host Rime executable. `model.json` records their hashes.

The app copies the precompiled files to a versioned, non-backed-up private folder;
it does not compile dictionaries on the phone. New sessions receive only active
Pinyin spelling, never surrounding editor text. Personal Rime dictionaries and
native input logs are disabled. MinIME's existing optional local choices remain
subject to its original privacy policy. Only candidates consuming the complete
input reach MinIME's whole-token commit path. Original MinIME candidates remain
available as alternatives and as fallback if the native engine is unavailable.
