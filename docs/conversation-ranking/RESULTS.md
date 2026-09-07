# Everyday conversation investigation

Baseline: 01c88ff / 0.5.0. The user deliberately supplied no examples. `conversation-probes.tsv` was authored and frozen before execution.

Exact probe SHA-256: b465efc82c1f47ec4b8103bf415994a9c9b7bd1d940dfe7be732d14b5b7b5e14. It covers 30 glyphs, 6 initials, 20 full/mixed/initial phrases, and 12 everyday English sentences. The English sentences also run in explicit English mode. Expected labels are evaluation-only and never enter production assets.

## Taiwan glyph conversion

Cause: our Luna Pinyin patch removed every upstream conversion filter. The decoder consequently emitted general Traditional variants rather than the Taiwan-standard glyphs this IME promises. This is a representation defect, separate from relative word frequency.

Fix: restore the existing `simplifier@zh_hant_tw` filter and its Taiwan switch default. Package the entire pinned OpenCC `TWVariants` dictionary, unmodified, with its upstream t2tw configuration using OpenCC's supported text dictionary serialization. Sources and hashes are retained in `third_party/rime/data-sources/opencc` and `opencc-data.json`. No evaluation word overrides, source frequency edits, or proprietary data.

| Measurement | 0.5.0 | Taiwan filter only |
|---|---|---|
| chi / chifan | 喫 / 喫飯 | 吃 / 吃飯 |
| weishenme / wsm | 爲什麼 | 為什麼 |
| nizainali | 你在哪裏 | 你在哪裡 |
| Existing reference first choice, 24 | 14 | 14 |
| Existing development first choice, 24 | 20 | 20 |
| Existing fresh phrase set first choice, 36 | 15 | 19 |
| English sentences preserved, Pinyin / English mode | 12/12, 12/12 | 12/12, 12/12 |

Seven new phrase labels were frozen after proposing the upstream filter and before querying it. All seven are available with Taiwan-standard glyphs; six rank first. `taishou` still prefers 太守 over 抬手 (rank 2). The first test incorrectly required every ambiguous phrase to rank first; its failure is retained in `taiwan-first-test.txt`. The corrected test verifies glyph conversion and reachability, and reports the independent first-choice result without concealing the remaining ranking gap. An existing test's exact count assertion also failed because coverage improved from 15 to 19; those assertions now enforce the previous coverage floors.

Paired physical-key observations in `phone-baseline/observations.json` confirm Google commits 吃飯, 為什麼, 你在哪裡 while 0.5.0 commits the variant forms. All 14 provider/case runs were observed. Google preserves hello/time/thanks in Pinyin mode and displays the Latin word first; MinIME preserves the commit but hides raw from the main candidate strip and displays Chinese conversions first. This baseline does not establish spontaneous English-mode Chinese conversion.

Verification: five focused Android tests passed in 12.751 s on SM-G781B / Android 13 / RFCR91GWXLX. Rime baseline and complete/prefix consumption checks passed. Asset hashes verified. Settings and learning were restored, Samsung IME restored, display verified Dozing. Only ARM64 was exercised on hardware. A class-list quoting error before the first run was a harness invocation failure; its stale pulls were replaced by completed-run evidence and were not counted.

## Candidate presentation

Cause: the layout hid raw candidate zero for every Pinyin-mode token, even when it was the literal Space default. Core appended English completions behind all Chinese conversions. Consequently common English words appeared to have been replaced in the candidate strip despite being committed literally.

Fix: show the literal default directly in both collapsed and expanded strips. When literal is the default, alternate English completions and Chinese alternatives, retaining each source's order and raw index zero. This intentional presentation policy keeps both languages close to the front without changing acceptance or dictionary ranking. Chinese-default compositions retain their separate phonetic row.

The new core regression fails on the old English completion ordering. Eight independently authored word probes, raw recovery, and an explicit learned Chinese default pass with the fix: 1291 core assertions. The phone test exercises hello/time/thanks plus fresh morning, visible raw coordinates inside the strip, expansion, Space, and partial Chinese selection. That test and the frozen conversation study pass (2 tests, 15.693 s). Every frozen study commit is unchanged from the Taiwan-only run. Phone restored and Dozing.

## Learned choices crossing language contexts

Cause: START_OR_LATIN conflated a fresh editor with a position following an English
word. Explicitly choosing 有 for you at the start then typing `see you tomorrow`
produced `see 有tomorrow`; three other independently authored sentences reproduced
the same mechanism. The regression fails on the baseline at `we can meet today`.

Fix: a transient boolean distinguishes a committed Latin word boundary. New
explicit choices there use AFTER_LATIN; the existing start key and Han contexts
are retained for stored-preference compatibility. This stores no English history
and changes no word-specific rules or model parameters. Private input and
lifecycle/newline/delete/punctuation resets are tested. Deliberate Chinese choices
within Latin input remain available and learn only in that context.

1615 core assertions pass, including four reproduced and four fresh ambiguity
cases. The broader desktop result is a one-variable comparison with the same
Taiwan model, corpus, source dictionaries and candidate presentation:

| Corpus/state | Known noninitial English tokens | Replaced before | Replaced after |
|---|---:|---:|---:|
| EWT development, 29 source-derived Chinese choices | 17714 | 981 | 0 |
| EWT test, same learning state | 17325 | 965 | 0 |

All 8999 Chinese probe results and fresh-state English results are unchanged.
Explicit English mode preserves all 43411 English tokens. The 29 overlaps are
data-derived, not an exception list. First-word preferences remain intentional:
the fix does not erase an explicit Chinese preference in its original context.

The broader run also exposes a separate data-import issue: capitalization filtering
removed capitalized AOSP entries, including ordinary months, cities and acronyms.
This is not concealed by the zero above, which measures known words after the
first word only. Unknown-word conversions remain in the raw reports and summary.
See DESKTOP_TESTING.md for corpus and measurement limitations. Android integration
of this core slice will be checked after the core/data work is complete.
