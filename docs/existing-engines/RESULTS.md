# Existing Pinyin engines, September 7, 2026

The user requested established solutions before further custom decoder work.
The proposed word-join experiment was stopped before implementation. Baseline
MinIME code is a3637e3, version 0.3.0. No evaluation phrase was added to a model.

| First-choice target hits | MinIME | AOSP PinyinIME + OpenCC | Rime + Luna Pinyin |
|---|---:|---:|---:|
| 24 exposed reference phrases | 8 | 12 | 14 |
| 24 exposed development phrases | 16 | 20 | 20 |
| 36 fresh probes | 6 | 13 | 15 |

The fresh set has 12 new authored phrases, each replayed as full readings, all
initials, and alternating full syllables/initials. Its source and generated inputs
were written before running any engine on it. Rime improves full readings 4→9/12,
initials 0→2/12 and mixed readings 2→4/12. Long initials remain a major gap.
Targets are plausible intended text, not unique interpretations of ambiguous
phonetics. These small tests establish a reason to try Rime, not general parity.

Rime produces 我們明天見 for both `womenmingtianjian` and `womenmtjian`, and
我要喝咖啡 for `woyaohekafei`. It also changes `bkq` from 不客氣 to 不可取;
backend selection must remain available. Raw per-probe results are adjacent.
The older paired Google result (23/24 Space commits) is a separate device study;
these new numbers measure decoder first choices, not complete UI parity.

## Reproduction and source identity

- [librime 1.16.1](https://github.com/rime/librime/releases/tag/1.16.1), official
  Windows MSVC x64 runtime, de4700e. Native Android integration builds source.
- Luna Pinyin, Essay and Prelude revisions and archive hashes are recorded in
  `third_party/rime/data-sources.json`; complete upstream source archives are
  retained there. Personal dictionaries are disabled. No optional grammar model
  or custom phrase vocabulary was loaded. The build excludes reverse lookup and
  script conversion filters, retaining Traditional dictionary output.
- [AOSP PinyinIME](https://android.googlesource.com/platform/packages/inputmethods/PinyinIME/+/49aebad1c1cfbbcaa9288ffed5161e79e57c3679/),
  revision 49aebad1c1cfbbcaa9288ffed5161e79e57c3679, Apache-2.0. All 84 inputs
  were fully parsed. The source's 1,068,442-byte dict_pinyin.dat is unchanged.
  The evaluation DLL uses `jni/share/*.cpp`, forced `sys/time.h` inclusion and an
  empty `cutils/log.h` shim (only disabled performance logging refers to it).
  G++ 64-bit compilation uses `-std=c++11 -O2 -shared -fpermissive`.
- AOSP's Simplified output is converted only for evaluation with
  opencc-python-reimplemented 0.1.7, `s2twp`; raw and converted outputs are both
  retained. Neither AOSP nor this Python package is included in MinIME.

`tools/benchmark_rime.cpp` calls the public C API with a new session for each
probe. Compile with the release `dist/include` headers; put the resulting Windows
executable beside rime.dll. Arguments: shared-data directory, empty user directory
with the recorded custom patches, input TSV, output TSV. `tools/benchmark_aosp.py`
uses the isolated `.tools/aosp-pinyin` DLL/source and `.tools/opencc-python`
package. `downloads.json` records downloaded binary/archive SHA256 values.
MinIME runs `PredictionBenchmark INPUT OUTPUT context` after `tools/test-core.ps1`.

Timing columns are diagnostic, not comparable latency benchmarks: Rime processes
incremental key events while MinIME and AOSP query the whole spelling. Desktop
results exclude Android dispatch/rendering. Phone verification is a separate gate.

## Android 0.4.0 integration and decision

Land Rime as an **optional** Pinyin backend. Enable **Use Rime for Pinyin phrase
prediction** in MinIME settings. It is off by default because individual candidate
choices can regress. The old decoder remains available as a selectable backend,
additional candidates and native-load fallback. English, Zhuyin, literal recovery,
privacy rules, gestures and editor ownership retain their existing implementation.
No proprietary Google code, models or assets are packaged.

Only complete-input Rime candidates enter the whole-token commit path. Each query
uses a fresh session with personal dictionaries disabled; a separate empty session
keeps model objects cached. The engine receives active spelling only. Reusing the
public whole-input API and retaining the model cache preserved all 84 candidate
lists; the one-variable measurements are in [CACHE_EXPERIMENT.md](CACHE_EXPERIMENT.md).
The two isolated model rebuilds produced the same bundle, with unmodified compiled
bytes and no evaluation phrases or frequencies added to the models.

Final validation on September 7, 2026:

- **32/32 Android tests passed**, 122.032 seconds, on the authorized Samsung
  SM-G781B phone running Android 13 (ARM64). Includes the existing editor/keyboard
  suite, Rime sentence and mixed-phonetic commits, punctuation, single-tap English
  switching, exact literal recovery, original-backend recovery, all 84 native
  probes, long-input bounds and query isolation.
- Native first choices reproduced **14/24 reference, 20/24 development and 15/36
  fresh**. All 84 complete candidate lists matched the previous isolated run.
  Repeated native query p50 **9.87 ms**, p95 **35.40 ms**, maximum **40.77 ms**.
  This full-suite run used an already loaded model (loadMs=0); the earlier isolated
  run measured initialization at 133 ms. These are debug native query timings,
  excluding MinIME fallback lookup, scheduling and rendering.
- The three synthetic long/noisy queries took **309.40, 92.89 and 10.59 ms**;
  each was below the predeclared one-second bound and left the next known phrase
  unaffected. This is bounded stress coverage, not a general worst-case guarantee.
- **1,153 core assertions passed**. Debug, test and unsigned release APK builds
  passed; Android lint reported **0 errors, 15 warnings**. Source preparation ran
  successfully from the hash-verified cached archives. Language/model integrity
  and exact packaged assets/notices passed verification.
- All four ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) compiled in debug and release.
  Every release native LOAD segment has 16 KB alignment, and both APKs pass Android
  zipalign with 16 KB page alignment. Only ARM64 was tested on a device; no 16 KB
  page-size device test was performed.
- Version **0.4.0 (5)** is installed. The wrapper restored MinIME preferences and
  Samsung Keyboard; a final power check confirmed **Dozing**. AOD was unchanged.

The universal debug APK is **64,399,962 bytes**; the unsigned release APK is
**39,264,211 bytes**. The additional native libraries and Rime model increase size
from 0.3.0. APK hashes, bundle identity, ELF checks and test evidence are retained
in `verification.json`, `elf-check.json`, `phone-tests.txt`, `core-tests.txt`,
`rime-phone-final.json` and `rime-stress.json`. The visible phrase screenshot is
[rime-phone-phrase.png](rime-phone-phrase.png).

Long initial-only sentences, candidate ordering and broader language coverage
remain open. This measured improvement does not establish Google Zhuyin parity.
The comparison favors this existing foundation over further custom sentence
assembly, while leaving future model choices open to independent evaluation.
