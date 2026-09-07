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
