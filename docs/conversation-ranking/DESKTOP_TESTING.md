# Core-first suggestion testing

SHINE AAC reference inspected: local revision b5b2de7b2eab8dc5e5b56ff97d599b5deb692f8a,
`packages/aac-core/test/communication-benchmarks.test.js`, its fixture/runner,
`scripts/lib/nccu-conversation-corpus.mjs`, and MOE glyph inventory. Its useful
pattern is a platform-independent core with source-provenanced frozen corpora and
separate physical integration gates. Its app-specific optical/release rules are
not copied into this IME.

Run `./tools/test-core.ps1` for fast behavioral regressions. Run
`./tools/test-desktop.ps1` for the frozen broad corpus. Neither builds or installs
an APK. The desktop evaluator loads the same TSV dictionary sources as the core,
and the exact packaged Rime model directory through the already pinned public
librime 1.16.1 desktop DLL. It requires JDK 17, g++, the pinned native headers and
the Windows DLL from the existing engine evaluation. Android SDK/Gradle are not
used. Outputs default to ignored `artifacts/desktop-results.jsonl`.

The C API bridge derives consumed input from the highlighted preedit's raw suffix.
It applies the current native adapter's complete/prefix bounds and ordering.
`python -X utf8 tools/check_desktop_rime.py` independently checks text, score and
consumption against saved ARM64 phone output: 95 raw inputs, 1767 candidates,
zero differences. This validates those adapter paths; it is not an Android UI or
latency test. A Highlight call on an already highlighted candidate returns false;
the adapter deliberately skips the redundant index-zero call.

The core evaluator coalesces intermediate phonetic queries and flushes the final
query before Space, exercising the real CompositionEngine callback/commit path.
Native results are cached by raw spelling only because sessions are independent;
fallback/context conversion still runs for the current core context. Reported
native timing excludes cache hits and includes IPC, not Android drawing.

Corpus v2: 13014 source input rows, 4015 English sentences in fresh/primed/English
modes, 2000 Chinese words in full/initial/two mixed spellings, and 999 eligible MOE
glyph entries with source ranks up to 1000. The 29 primed English/Pinyin overlaps
are selected by intersecting source syllables, the existing English dictionary,
and the corpus vocabulary; an ordinary explicit Chinese candidate selection is
simulated for each, without choosing words manually. Corpus v1 was withdrawn
before scoring because annotation token boundaries broke natural contractions;
the retained metadata explains that import error. V2 uses original sentence text.

`python -X utf8 tools/make_conversation_corpus.py` regenerates inputs from checksum-
verified UD dev/test archives and the adjacent SHINE MOE extraction. The manifest
records source URLs, hashes and sampling. UD sources/licenses are in
`third_party/ud`; derived evaluation text follows their CC BY-SA 4.0 attribution.
The MOE public frequency table is an independent reference, not runtime training.
No evaluation file is packaged in the IME.

Limitations: the English audit lowercases and extracts alphabetic/apostrophe words,
so it does not assess case, punctuation or numeric fields. Chinese GSD is largely
Wikipedia, not spontaneous speech; the earlier everyday scenarios complement it.
Chinese source words without dictionary reading annotations are counted as missing
coverage. Existing annotated readings are used to construct test spellings, not
to define expected output; polyphonic reading ambiguity remains a limitation.
Repeated spelling forms and cache hits must not be described as independent
language samples. Fresh/primed modes are a controlled learning-state comparison,
not evidence that every user has selected all overlapping syllables.
