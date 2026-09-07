# MinIME 0.3.0 gap implementation

September 7, 2026. Changes are committed on `codex/close-ime-gaps`, following the
reviewed baseline `617b5ac`. This is a substantial functional update, **not full
Google Zhuyin parity**. Pinyin and English remain the primary layouts.

## Implemented changes

| Area | Result | Main commits |
|---|---|---|
| Abbreviated Pinyin | Space accepts a Chinese candidate for eligible lowercase abbreviations; known English prefixes, commands and exact recovery remain available | `3966245` |
| English completion | Deferred word spacing, full prefix-range frequency ranking, expanded candidate grid | `fd0ac1c`, `0ad23ed`, `d4117f4` |
| Capitals | Editor sentence/word flags, existing upward slides and double-Shift Caps Lock | `d54757a` |
| Context | Offline English next words, optional local word-pair adaptation, limited Chinese boundary reranking | `a06a5f2`, `98263a9` |
| English assistance | Optional spelling correction, contraction alternatives, immediate correction undo, configurable double-Space punctuation including after completion selection | `ebb7fca`, `6698d54`, `3e6b5be` |
| Palettes | Floating period popup with hold-drag-release, emoji category browsing/swiping, optional private-aware recents | `85e4c40` |
| Editor lifecycle | Validated owned-span recovery on hide/show and restart; Chinese Enter confirms before editor actions | `81b98ff`, `69dc35d`, `f3fef11` |
| Responsiveness | Coalesced Chinese decoding off the UI thread, stale-result rejection and ordered commits | `f3fef11` |
| English tracing | Geometric dictionary-based word tracing, candidate alternatives, retained vertical slides | `1d324b9`, `f7c10f2` |
| Startup | Build-time model compilation, precomputed indexes, shared strings and recorded hashes | `0c8e642` |
| Follow-up fixes | Stable candidate controls, privacy-policy restart boundaries and correction scope | `a64e72a`, `bb05ae9` |

Correction, English word-pair learning and emoji recents default off. Double-Space
period defaults on in English. Private fields do not access personalized history.
Restart recovery checks only the IME-owned composing text, bounded to 96 characters.
The app has no network permission. No Google APK code, dictionaries or model data
are packaged. Data attribution and derivation notices accompany the model.

## Evidence and limits

The repeated 24 conversational Pinyin probes improved from **4/24 to 8/24** intended
phrases accepted with Space. Google Zhuyin achieved **23/24** in the same replay.
For example, `bkq` now accepts 不客氣, but `wmmtj` still ranks 我們茅台酒 above
the intended 我們明天見. Long initial-only and mixed partial sentences remain
the largest gap. The review phrases never entered training or runtime overrides.

The accepted context change improved first-choice hits from 322 to 332 on 400
held-out dictionary-covered word probes. A subsequent set of 400 nonduplicate
probes improved from **323 to 332**, and top-five hits from **391 to 393**.
These are full-reading probes from a held-out encyclopedic corpus; they do not
establish conversational sentence quality. Broad and assembled-phrase reranking
experiments regressed and were rejected. See [experiment records](PINYIN_CONTEXT_EXPERIMENTS.md).

The paired follow-up covers **34 distinct scenarios**, plus a trace recheck:
all 34 pairs were observed. English completion spacing, double-Space punctuation,
sentence capitalization, upward capitals and the rechecked trace matched the
reference output. Matching output alone is not full interaction parity. The grid
is independently laid out. In this run Google discarded composition on hide/show
and restart, whereas MinIME preserved it; this differs from earlier observations,
so lifecycle compatibility should not be inferred from one reference replay.
See [raw comparison](gap-implementation/comparison.json) and adjacent screenshots.

Phone model loading measured **2.246 s binary vs 7.377 s TSV**, with approximately
**104.7 MB vs 135.0 MB** retained heap. The tradeoff is an approximately 18.5 MB
debug APK. Read the [measurement scope and ordering](gap-implementation/STARTUP.md)
before treating these as general device guarantees.

The first trace decoder took 494–921 ms on the phone and missed the paired
observation deadline. Removing per-word regex compilation and reusing key geometry
reduced the same probe to **6–50 ms**, with the same winning word. The subsequent
paired probe composed `hello` for both IMEs. This is a narrow latency/gesture check,
not a broad trace accuracy benchmark.

## Verification

- **1,153 core assertions** pass with both TSV and compiled model loading.
- **29 native phone tests pass**, covering editors, WebView, Pinyin/Zhuyin/English, slides,
  Caps Lock, candidate grids, correction undo, short contractions, punctuation
  hold-drag-release, emoji privacy, tracing, and lifecycle recovery. The final
  instrumentation result is retained in `gap-implementation/device-tests.txt`.
- Async contracts cover stale replies, ordered queued commits and field changes.
  Device tests wait for a Space commit to finish before moving the cursor;
  moving it during pending conversion intentionally cancels that conversion.
- Debug and unsigned release builds succeed. Lint has **0 errors, 15 warnings**;
  warnings include target SDK age, programmatic view constructors, localization
  and custom-view/accessibility/RTL follow-up work.
- Asset hashes and the model inside the APK are checked against the source manifest.

Only the authorized Samsung SM-G781B (`RFCR91GWXLX`, Android 13) was used. Test
wrappers restore the previous Samsung keyboard and MinIME preferences, then send
`KEYCODE_SLEEP`. Final device state and artifact hashes are recorded alongside
the test evidence. No tablet testing was performed.

Remaining work includes conversational Chinese data and ranking, broader typo and
trace evaluation, finer visual/punctuation parity, accessibility and landscape
testing across devices, and production signing. These gaps are not hidden by the
passing functional tests.
