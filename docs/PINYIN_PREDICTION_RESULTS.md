# Pinyin abbreviation and capitalization results — 2026-09-06

The user prioritizes Pinyin and requires mixtures of complete and abbreviated
syllables, including one initial per character. MinIME now matches those readings
and combines dictionary words. Upward QWERTY slides commit capitals directly,
including after Shift. This deliberately follows the user's request over the
lowercase output observed in legacy Pinyin's tested configuration.

## Direct reference evidence

Google Zhuyin 2.4.5.164561151 on the authorized Samsung SM-G781B, Android 13:
39 new sequences, each starting in an empty synthetic editor. Live accessibility
nodes supply key bounds; each step records visible candidates and editor text.
The previous keyboard was restored and the display put to sleep after each run.

- [18 abbreviation sequences](pinyin-abbreviation-observations.json): nh/nih/nhao
  offer 你好; jt/jtian offer 今天; srf/shrf/srufa offer 輸入法; wxsrf offers 我想輸入法.
- [21 further sequences](pinyin-holdout-observations.json): Google ranks 手機 first
  for sj/sji and 我們需要學習 first for wmxyxx. MinIME's frequency model disagrees.
  Some intended targets are not in Google's visible strip either: zj favors 自己,
  ches favors 車上, and za does not visibly suggest 早安. Intended-target coverage
  and agreement with Google are different measurements.

Plans are in `docs/legacy-study/pinyin-abbreviation-plan.json` and
`pinyin-holdout-plan.json`. These observations are evaluation evidence only.
The app contains no Google code, model, candidate recordings, or evaluation files.

## Implementation and boundaries

`PinyinSyllableIndex` stores canonical syllable boundaries from the licensed source.
Each input segment can match any nonempty prefix of a source syllable, not merely
the first letter. Apostrophes force boundaries. A bounded word lattice composes
matches across dictionary entries. Source frequencies and a fixed missing-letter
penalty rank them. Candidates retain multiple character-count interpretations so
`sh` can represent either one syllable or two initials. Exact homophones remain
available separately. A sorted reading-prefix index also offers trailing phrase
completion such as meiwen → 沒問題, without expanding every untyped trie branch.

Search is bounded to 2,048 states per word-start and 16,384 per token; word input
is limited to 32 letters and composition to 96. Candidate/beam pruning can omit
valid low-frequency phrases. This supports arbitrary mixtures as a grammar, but
does not guarantee exhaustive phrase recovery or Google's contextual ranking.

Candidate generation does not by itself change the raw Space default for literal
intent. Otherwise `adb` unexpectedly auto-converted to 愛丁堡 during development.
An explicit local Chinese choice can become the default for that abbreviation in
the same context; private fields ignore it. Exact-input recovery, URL/code handling,
English spaces, selection invalidation and secure input remain covered by tests.

## Measured target coverage

Top-five is the intended target among the first five **Chinese candidates**;
MinIME's fixed raw slot is excluded. Targets never influence runtime scoring.
This is a small compatibility/development study, not general language accuracy.

| Dataset and method | First candidate | First five | Decision |
| --- | ---: | ---: | --- |
| Initial 16-case development, original exact/segmented engine | 7/16 | 8/16 | Baseline |
| Same 16, bounded whole-reading prefix lookup | 15/16 | 16/16 | Keep for trailing completion |
| 18 live-reference cases, prefix lookup alone | 6/18 | 7/18 | Insufficient for initials |
| Same 18, final syllable + prefix matchers | 14/18 | 16/18 | Keep; all 18 targets reachable |
| 24-case first holdout, syllable matcher | 17/24 | 21/24 | Exposed character-count pruning |
| 21-case second holdout, final combined decoder | 8/21 | 15/21 | Keep failures; no ranking tuning |
| Final fresh 9-case probe, combined decoder | 7/9 | 9/9 | Supports the combined mechanism |
| Original 16-case set, final combined decoder | 15/16 | 16/16 | Preserves prefix improvement |

Raw per-case results, including intermediate failures, are in
[evidence/prediction](evidence/prediction). The original dictionary implementation
baseline SHA-256 was `bbf98876e92c810e5388a054c48d676d54f28f5dc4837dfe60c24ff9f7294cf2`.
Evaluation TSVs live under `core/src/test/resources/prediction-*.tsv`, outside
packaged assets. `PredictionBenchmark` can replay each TSV independently.

## Negative results and remaining gaps

- The first abbreviation decoder discarded 你好 for nh and 生活 for sh. Increasing
  reachable candidates and retaining character-count diversity makes them selectable;
  source frequency still puts 你好 eleventh for nh and 生活 on a later page for sh.
- Short initials remain highly ambiguous. 再見 is absent for zj in the final probe;
  手機 is tenth for sj. More typed letters and explicit local choices help, but a
  stronger contextual model is still needed.
- Sentence alternatives such as 我要去學校 were pruned for woyaoqxx; 我們需要學習
  ranks eighth for wmxyxx. The decoder can combine words, but unigram segmentation
  does not reproduce Google's sentence preferences.
- Expanding untyped trie tails directly raised desktop median key-processing from
  roughly 0.5 ms to 4.9 ms (p95 16.8 ms, max 112.9 ms). That experiment was removed;
  the bounded prefix index supplies that feature instead.
- Ordinary active-composition context scoring was considered but not added. Only
  existing local-choice context and idle continuations are used. The 可 + yi probe
  still ranks 以 second. There is no trained sentence model or typo correction.
- Android benchmarking exposed a second performance defect: character-count
  diversity was retained in every intermediate sentence beam. The strict six-path
  intermediate beam reduces the 32-s worst-case probe from median 794 ms / p95
  932 ms to median 130 ms / p95 138 ms on the same phone. The 18-case and 21-case
  target top-five counts remain 16 and 15 after the correction. Word and final
  display candidates retain character-count alternatives. Long ambiguous input
  still costs more than a frame, so UI scheduling remains a performance gap.
- Single-sample desktop benchmark outliers include GC and concurrent build load;
  they do not establish a speedup. Final core regression measured p50 0.79 ms,
  p95 6.55 ms, max 30.14 ms over 654 keys, startup 2,497 ms. Android conversion
  costs are measured separately; neither is key-to-visible-frame latency.

The explicit headless `PredictionPerformanceTest` uses four warmups and 12 measured
conversions per synthetic input. [Final Android costs](evidence/prediction/android-performance.json)
and [pre-fix costs](evidence/prediction/android-performance-before-beam-bound.json)
are retained. Normal short probes have median 3.4–7.4 ms and p95 4.3–12.4 ms;
womenxyaoxuexi has median 10.8 ms / p95 14.7 ms. A fresh-process dictionary load
took 7.3 seconds and approximately 124 MiB retained Java heap. Those costs need
future optimization; asynchronous loading keeps literal input available meanwhile.
This is a one-phone microbenchmark, not a claim about typing frame latency.

Final APK identity, Android checks and device cleanup are recorded in
[VERIFICATION.md](VERIFICATION.md).
