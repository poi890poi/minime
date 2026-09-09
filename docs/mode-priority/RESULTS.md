# Dedicated language priority

The dedicated 台/日 modes now prioritize their own complete and incomplete
matches. Chinese and English modes retain their previous policy. Explicit custom
entries, learned phrases, apostrophe restoration and unfamiliar/duplicate Han
glyph safeguards remain ahead of generic promotion. Raw recovery stays slot zero;
Space uses the displayed highlight and typing alone never accepts a suggestion.

This is a ranking change against 0.7.2 (`7fb0ae1`), before the subsequently
requested single-kana/common-kanji data extension. No asset changed in this stage.

## Frozen regression comparison

3,922 exposed queries, four modes, 15,688 paired outputs; target labels only score
saved outputs. Chinese and English have exact output/highlight parity on every
query. Candidate text inventories are unchanged in every mode. Taiwanese order
changes on 689 queries, Japanese on 453; highlights change on 252 and 179.

| Source retrieval | Queries | First 3 optional slots, before → after | First 8, before → after |
|---|---:|---:|---:|
| Taiwanese complete | 128 | 120 → 121 | 122 → 122 |
| Taiwanese half prefix | 128 | 14 → 28 | 17 → 51 |
| Japanese complete | 128 | 79 → 79 | 79 → 79 |
| Japanese half prefix | 128 | 25 → 64 | 29 → 97 |

These are ordinal slots excluding raw slot zero, not measured physical first
row/page. Japanese complete targets often equal raw Romanization (49/128); the
raw count is separate. The corpus contains English conversations and essays,
Chinese essays, and Japanese/Taiwanese seen-source retrieval, with genre and input
conditions retained in [results.json](results.json). It is not a fresh holdout,
Japanese/Taiwanese conversation benchmark, or language-model accuracy estimate.

Tradeoffs: in Japanese mode, English conversation half-prefix top-three targets
fall 30 → 26/256 and essay targets 25 → 21/128. Chinese essay full top-three falls
224 → 223/416. Taiwanese mode English conversation top-three rises 25 → 30/256,
but top-eight falls 54 → 53; essay top-eight falls 45 → 44/128. No target becomes
unavailable. Exact Chinese-mode and English-mode parity protects the other modes.

Missing retrieval is unchanged: half-prefix targets are available anywhere for
only 51/128 Taiwanese and 97/128 Japanese probes. Transposition results do not
improve. This policy cannot manufacture absent readings or correct source spelling.

## Performance and verification

Three measured passes after a complete warmup, 11,766 samples per mode/version.
Cached query merging, ranking and callback application only; native/dictionary
lookup, typing, UI rendering and cold loading are excluded.

| Mode | Median µs, before → after | p95 µs, before → after |
|---|---:|---:|
| Taiwanese | 21.4 → 21.0 | 169.2 → 167.1 |
| Japanese | 17.8 → 17.5 | 164.4 → 153.1 |

Separate sessions and other machine activity prevent a causal speedup claim.
Full distributions including maxima are retained in the timing files and JSON.
Core passes 29,323 mechanical assertions. Pinned desktop Rime 1.16.1 passes the
same native-inputs regression as 0.7.2 (628 uncached queries), with identical output
SHA-256 `8e3ec8cd7cf56571a55fd42e0100b646267e9f5ce3b4b0a1f04af8941f86abe1`.
An accidentally started default 13,014-row evaluator was stopped in favor of this
same-baseline gate; its incomplete output is not counted as a pass. One initial
synthetic test incorrectly expected two initials to retrieve a three-syllable
phrase. The fixture expectation was corrected; matching logic was unchanged.

No new source-derived holdout was added in this stage. Production sources and
previously exposed corpora are explicitly separated from any independent accuracy
claim. Android/package checks follow in the release commit, after the user's
additional Japanese character request.

## Reproduce

Run `tools/test-core.ps1`; run `tools/test-desktop.ps1 -Corpus
 docs/suggestion-latency/native-inputs.tsv`. Run `ModeCoverageBenchmark` against
0.7.2 classes and this commit using the frozen input-modes TSV with `candidate`
as the third argument (four modes in both runs). Run `ModePriorityBenchmark`
with an output TSV argument for cached application timings. Save results in
`artifacts/mode-priority/`, then run `python tools/report_mode_priority.py`.
