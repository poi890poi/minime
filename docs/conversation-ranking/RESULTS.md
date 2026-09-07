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

Further investigation and verification recorded below as each independent change is completed.
