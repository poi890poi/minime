# Shared-core verification for Taiwanese pairs

Baseline: `e6d1334` (MinIME 0.7.0). New feature: immutable source-owned alternatives,
with default POJ-primary acceptance, direct Han acceptance and optional reversal.
Ranking and matching run on the original phonetic output before presentation.

- `tools/test-core.ps1`: final run passes 29,186 mechanical assertions, including
  delayed selection using the current decoder consumption boundary, paired tap,
  hold, Space, preference reversal, missing pairs, source isolation, privacy,
  stale/mode/option changes, queued acceptance and same-Han candidate identity.
- Exhaustive paired-off/on audit: 179,699 unique complete keys and proper prefixes
  from the entire existing POJ inventory; equal result counts, ordering, scores,
  consumption and incomplete flags. 126,858 result occurrences carried a pair.
  This is structural parity, not language-model accuracy or a fresh holdout.
- 3,922 frozen conversation/essay/retrieval conditions also retain output order.
  Inputs and exposure are recorded in `../input-modes/coverage-manifest.json`.
- Pinned desktop Rime 1.16.1: 628 native queries; output SHA-256
  `8e3ec8cd7cf56571a55fd42e0100b646267e9f5ce3b4b0a1f04af8941f86abe1`, identical
  to the 0.7.0 run. Pairs do not modify the native bundle.

The first acceptance implementation incorrectly included the old `consumed` value
in identity. Existing `CandidateSelectionRegression` rejected this: a still-visible
word must use the current decoder alignment. That constraint was removed; the
source pair, text and composition retain ownership. The exhaustive lookup audit
passed in that first run, whose later acceptance failure is retained in the result
record. The complete final core run passes after correction.

Desktop lookup timing (JDK 17, Windows, six alternating paired/off passes, three
warmups; 3,922 frozen rows per pass; all optional Chinese + POJ packs):

| Run | Median, off / paired | p95, off / paired | p99, off / paired |
|---|---:|---:|---:|
| Concurrent exhaustive audit | 307 / 296 µs | 3,664 / 3,701 µs | 6,822 / 7,814 µs |
| Separate repeat | 270 / 270 µs | 2,867 / 3,008 µs | 5,830 / 6,785 µs |
| Separate repeat with saved samples | 283 / 286 µs | 3,032 / 3,098 µs | 5,904 / 6,274 µs |

The last run's mean is 764 / 782 µs and maximum 31.9 / 35.8 ms. No speedup is
claimed. Median cost is similar; paired-result copies show a small tail overhead
in these runs (p99 +0.37 to +0.99 ms). This is lookup-only timing, not Android
end-to-end response. Metadata parsing took 155–322 ms once off the input thread;
that cold measurement includes parser/JIT startup. Sidecar size is 388,661 bytes.

Reproduction (from the repository root, after compiling core tests):

```powershell
python -X utf8 tools/compile_paired_forms.py --check
& tools/test-core.ps1
java -Dfile.encoding=UTF-8 -Dminime.exhaustivePairs=true -Xmx1g -cp core/build/manual dev.minime.core.Regression
java -Dfile.encoding=UTF-8 -Xmx1g -cp core/build/manual dev.minime.core.PairedFormsBenchmark artifacts/paired-lookup-samples.tsv
& tools/test-desktop.ps1 -Corpus docs/suggestion-latency/native-inputs.tsv -Output artifacts/paired-native.jsonl
```

The exhaustive check is opt-in because it takes several minutes. Its input set is
systematically enumerated from production for parity; it must never be presented
as independent retrieval accuracy. See `RESULTS.md` for Android and release evidence.
