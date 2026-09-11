# Source and engine decisions

All additions are evaluation-only registrations, with no production input patterns.
Download URLs, complete commit IDs, source hashes, generated data hashes and
current MinIME asset hashes are in source-manifest.json. The downloaded source and
executables live under ignored artifacts/japanese-engine-benchmark.

| Candidate | Evidence and rights | Decision |
|---|---|---|
| [Kazuma Naka C++ converter](https://github.com/KazumaProject/kana-kanji-conversion-c-plus-plus/tree/f8af0c2e6a7538f83e7483e9171ebec04fd963a0) | MIT implementation; compressed LOUDS lookup, POS connection costs, A* sentence conversion. Built from pinned source. | Run the non-neural converter as an isolated feasibility candidate. This is not Mozc's engine. No production import approval. |
| [Google Mozc](https://github.com/google/mozc/tree/d918f30063999d4ca250f76ced8b92169a0e0b25) | BSD source plus NAIST/ICOT and public-domain dictionary notices in the full LICENSE. Official [Windows build](https://github.com/google/mozc/blob/master/docs/build_mozc_in_windows.md) requires a newer toolchain than this machine's VS2019 setup. | Use its pinned OSS dictionary/cost data with the existing converter. Actual Mozc engine was not run; compare it before selecting a long-term backend. |
| [azooKey converter](https://github.com/azooKey/AzooKeyKanaKanjiConverter) | Swift package; [project overview](https://azookey.com/OpenSource) identifies MIT engine / Apache-2.0 dictionary. Neural and non-neural configurations have different resource costs. | Retain for a separate engine comparison. No installed Swift toolchain, no Android integration or measured performance in this experiment. No unmeasured quality claims. |
| Hechima Mozc WASM | QuuBee credits identify third-party release hechima-wasm-v0.2.0, but the release API returned 404. | Reject as this experiment's reproducible baseline. No unavailable artifact executed, no performance inferred. |

The 1,289,076 dictionary source rows and 7,139,584 connection values are processed
by the unmodified pinned upstream builders. No entries, costs, aliases or output
forms are selected by MinIME. The result contains 745,965 reading keys. Neural
models, optional extra dictionaries and the upstream network fetch executable are
disabled. Keep the additional dictionary notices when packaging any later pilot.

## Independent evaluation

RealPersonaChat and ASDC reuse the already registered, byte-pinned source archives.
The new sampler excludes the 192 previously selected conversation files and the
two schema-inspected files. New splits have 16 development / 48 holdout documents
per source, eight hash-selected turns each. All 1,024 turns are retained, including
unreadable clauses; 96 turns repeat previously exposed text and 31 held-out turns
repeat development text. These sets overlap. Report those flags and the subset
without either repetition; document disjointness alone is not textual novelty.
Repeated greetings are not removed from the natural conversation denominator.
Readings remain silver except original kana; neither is a record of human keys.

[AJIMEE-Bench](https://github.com/azooKey/AJIMEE-Bench/tree/401666cd56d1a570c2021798b64b6da4396bfd45)
contributes all 200 human-reviewed conversion items, acceptable alternatives and
original Wikipedia Input Error Dataset indices. Its data is CC BY-SA 3.0; its
evaluation utility is CC0. We reuse its exact-match and minimum CER definitions.
The published README example is exposed, and the general benchmark may have been
used by upstream engine authors. It is an external benchmark, not a pristine
holdout. Both adapters omit left context, even on the 100 context-bearing items;
report that subgroup separately and do not claim the official contextual score.
Twenty-seven items require over 96 canonical romanization keys, so also retain
the input-length flag rather than hiding MinIME's composition-limit failures.

The sources support comparing Japanese conversation and encyclopedic conversion.
They cannot establish Taiwanese quality, community spelling preferences, human
touch hit rates, personal-name appropriateness or subjective candidate relevance.
