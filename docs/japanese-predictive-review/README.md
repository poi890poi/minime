# Predictive API review and posting-index experiment

**Keep the current Japanese provider.** Neither upstream predictive API passed
the feasibility screen. An isolated posting index makes sentence conversion
about eleven times faster at p95, but it exposes allocation-dependent candidate
ordering. Exact output parity fails, so this experiment is not integrated into
the APK.

Baseline: 1b3aa8d; same Kazuma source revision, Mozc dictionary bytes, top eight,
beam 50 and reference adapters as the preceding benchmark. No vocabulary, costs,
language weights, production source inputs or app code changed. The new corpus
is 384 systematically selected **previously evaluated development** probes,
split equally between source clauses and words. Timeouts and unrun cases remain
explicit; small completed prefixes are not reported as overall coverage.

## Existing prediction mechanisms

| Mechanism | Completed/attempted before rejection | Finding |
|---|---:|---|
| Graph CommonPrefixPlusPredictive | 0/1 clause probes | First query exceeded 5 seconds. It admits readings matching only the first kana, and excludes longer completions. |
| Separate lexical API, predK=1 | 4/5 clause; 0/1 word | Populated lookups took hundreds of milliseconds to over 5 seconds. |
| Lexical API, full supplied prefix | 4/5 clause; 0/1 word | Empty lookup improved from about 10 ms to under 0.1 ms; populated lookups remained slow. |
| Indexed postings + lexical API | 8/8 clause; 0/1 word | Three clause queries exceeded 250 ms; the first word query exceeded 5 seconds. |
| Indexed postings + full-prefix lexical API | 8/8 clause; 0/1 word | Same rejection; one populated prefix still took about 5 seconds. |

Each screen planned 192 probes. The automatic stop rule was fixed before runs:
stop on one 5-second timeout or three queries above 250 ms. These are rejection
thresholds, not UX acceptance targets. See every *-load.json for attempted,
planned and unrun counts. Word probes test word completion separately; they do
not rescue a failed clause-completion claim.

The graph mismatch is confirmed with a tiny synthetic trie, using the unmodified
upstream code: an input of かき admits かく while excluding the longer かきく.
This proves API semantics, not Japanese accuracy. The separate lexical API
retains the complete supplied prefix. Full-prefix and one-kana searches produce
identical ordered outputs for all completed baseline comparisons and the six
fixture queries; timeout cases provide no parity evidence. The adapter's lexical
output also matches the pinned command-line program on the fixture.

## General performance finding

TokenArray::getTokensForTermId scans the posting bit vector from the beginning
twice for select0, followed by two rank scans. A load-time offset array eliminates
that repeated work. The experimental patch owns its array, clears it on clear(),
and leaves builder-created objects on their existing lookup path. Serialized
dictionary files and every token's output, cost and ordering remain unchanged.

An independent sequential bit walk verified **745,964 reachable posting lists
and 1,289,075 token payloads**. The original file has one final unterminated list;
the old lookup cannot retrieve its one token. This experiment preserves that
behavior to avoid mixing a coverage fix into a performance comparison.

Three new isolated timing passes, alternating baseline/indexed order, each type
the same 64 development clauses twice. Timing below is the repeat cycle, with
1,311 keys per pass. No result cache is added.

| Desktop measurement | Original converter | Posting index |
|---|---:|---:|
| Per-key engine p50 | 4.53–4.55 ms | 0.46–0.48 ms |
| p95 | 36.68–36.78 ms | 3.24–3.30 ms |
| p99 | 62.65–65.31 ms | 9.42–9.62 ms |
| Worst repeat query | 100.99 ms | 11.03 ms |
| Load time | 300–302 ms | 317–321 ms |
| Process RSS after loading | 69.6 MiB | 72.5 MiB |
| Added dictionary bytes | — | 0 |

The speedup repeats, at about 2.9 MiB more resident memory and 15–21 ms more load
time. These are Windows desktop engine/process measurements, not Android event,
rendering, battery or retained-heap measurements. Lexical completion remains too
slow: removing one posting bottleneck does not bound its subtree enumeration,
term lookup and candidate expansion work.

## Failed output gate: address-based ties

The posting payload audit passes, but **exact candidate-list parity fails**:
195 of 18,730 primary regression records differ. An additional four differences
occur in the 384 development screen records. Across the primary records,
201 individual query events differ, including 28 changes to top-eight membership.
Reference ranks, hit outcomes, completion actions and committed text did not
change in those runs; AJIMEE's 200 records match exactly. Those facts do not
waive candidate stability.

The cause is visible in upstream FindPath::StateLess: after equal total cost,
position and length, it compares state pointer addresses. The posting index
changes allocations and can therefore change tie order and which tied items
fit in eight slots. This is an existing converter issue; MinIME's shipped
candidate flicker cannot be attributed to a backend it does not yet use.

report.py preserves all differences and exits nonzero. Do not change that gate
to ignore ordering or treat unchanged hit rate as complete behavioral parity.
The source patch remains an isolated experiment rather than a production fix.

## Decision and next boundary

- Reject graph prediction as a completion provider at these settings.
- Reject unbounded lexical prediction for per-key integration, including the
  full-prefix and posting-index variants tested here.
- Retain the posting-index technique for sentence conversion, but first replace
  pointer-address ties with a deterministic, general rule. Compare indexed and
  unindexed conversion under the same tie policy in an independent change.
- Then verify bounded prediction work and the complete shared-core provider
  contract before building the optional native integration. New ranking/coverage
  decisions need new document-disjoint holdouts; these corpora are consumed.

SHINE explicitly released RFCR91GWXLX during this work. MinIME acknowledged the
release but did not reserve or operate the phone. No APK was built or installed.

## Reproduce

Use the pinned dependencies and modern Python environment documented in
../japanese-engine-benchmark/README.md. All original sources/builds are retained;
index_postings.py creates a separate artifact copy and emits posting-index.patch.

```powershell
./docs/japanese-predictive-review/build.ps1
python docs/japanese-predictive-review/evaluate.py freeze
python docs/japanese-predictive-review/evaluate.py graph
python docs/japanese-predictive-review/evaluate.py lexical --unit word
python docs/japanese-predictive-review/index_postings.py
./docs/japanese-predictive-review/build.ps1 -Indexed
python docs/japanese-predictive-review/evaluate.py indexed-lexical-fullprefix
python docs/japanese-predictive-review/check_fixture.py
python -m unittest discover -s docs/japanese-predictive-review -p test_screen.py
# Reuse the prior evaluator; redirect new results to preserve earlier evidence.
python docs/japanese-engine-benchmark/run.py kazuma-indexed probes --role holdout --output-dir docs/japanese-predictive-review
python docs/japanese-predictive-review/report.py  # expected nonzero: ordering gate
```

The source registry records these as evaluation-only. See NOTICE.md for upstream
MIT code and CC BY-SA conversation attribution. Three screen-contract tests,
seven existing evaluator tests, the source-framework tests, fixture checks and
the exhaustive posting audit pass. The ordering gate deliberately remains red.
