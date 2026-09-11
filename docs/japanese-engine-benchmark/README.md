# Japanese sentence conversion: benchmark result

**Retain the existing converter for a native integration experiment; do not
replace the whole Japanese suggestion provider at these settings.** It improves
complete-clause coverage dramatically, but its conversion-only mode loses early
completions and takes much longer per query. No production code or dictionary
changed in this experiment.

Baseline: MinIME 52e083d, Japanese/English core, private session, learning disabled,
current generated assets. Candidate: Kazuma Naka's pinned, unmodified non-neural
C++ A* converter, Mozc OSS dictionary/cost data, top 8, beam 50,
CommonPrefixOnly. This is **not a run of Google's Mozc converter**.

## Coverage and completion

New document-disjoint test split: 48 conversations per source, eight source turns
each. The simulator attempts whole clauses, chooses the intended output if it is
among eight visible slots, otherwise deletes the composition and retries source
words separately. MinIME selections go through the real CompositionEngine;
the C++ adapter models raw/commit state around the stateless converter. These are
oracle user choices, not automatic correctness, physical pages or human timing.

| Held-out source | MinIME | Existing converter |
|---|---:|---:|
| Casual chat: valid clauses matched in eight slots | 196/653 (30.0%) | 623/653 (95.4%) |
| Accommodation dialogue: valid clauses matched | 496/1,448 (34.3%) | 1,406/1,448 (97.1%) |
| Casual chat: complete source turns | 113/384 (29.4%) | 269/384 (70.1%) |
| Accommodation dialogue: complete source turns | 82/384 (21.4%) | 254/384 (66.1%) |
| Casual chat: mean actions per attempted turn | 118.4 | 47.5 |
| Accommodation dialogue: mean actions per attempted turn | 255.2 | 101.9 |

Actions count actual simulated keys, deletions, selection taps and literal
separator keys. Failed attempts remain in the means. Successful-only means are
also in summary.json; differing success sets prevent interpreting either average
as a measured human speedup. No gold Kanji is injected to complete a failed word.
The current engine's clause-first retries roughly double the action cost compared
with starting word-by-word (4.95/5.02 versus 2.30/2.35 actions per source character).
The existing converter reverses that trade-off (1.99/2.01 versus 2.14/2.17).

Unresolved source readings remain failures: only 278 casual and 255 accommodation
turns have every clause readable under the reference rules. Full-turn rates use
all 384 turns per source; the valid-clause scores exclude unreadable clauses and
must not be described as unrestricted language accuracy. Sudachi readings are
silver; source token boundaries provide idealized segmentation assistance.

After removing normalized exact repeats and near repeats identified by the
fixed retrospective audit rule, complete-turn results remain 90/356 versus 246/356 for
casual chat and 63/324 versus 202/324 for accommodation dialogue. This sensitivity
analysis changes scoring only; it does not remove common greetings from the main
corpus. It cannot establish speaker or semantic disjointness. See overlap-audit.json.

The external, human-reviewed AJIMEE encyclopedia benchmark also supports a
sentence-conversion gap: top-eight matches were **2/200 versus 145/200**; default
output matched **2/200 versus 89/200**. With the 27 inputs above 96 romanization
keys removed, the result remains **2/173 versus 134/173**. Both engines received
no left context. Context-bearing and context-free groups, minimum character error
rates and length-limit flags remain separate in summary.json. These are not
official contextual AJIMEE scores, and the public benchmark is not a fresh holdout.

## Negative results: completion and imprecision

| Intended full clause from partial input | MinIME | Existing converter |
|---|---:|---:|
| Casual chat, half reading | 55/653 (8.4%) | 7/653 (1.1%) |
| Casual chat, three-quarter reading | 72/653 (11.0%) | 7/653 (1.1%) |
| Accommodation, half reading | 245/1,448 (16.9%) | 0/1,448 |
| Accommodation, three-quarter reading | 322/1,448 (22.2%) | 0/1,448 |

CommonPrefixOnly performs sentence conversion; it is not a substitute for a
predictive-completion API. Neither engine robustly handles simulated neighboring
keys or transpositions. Changed neighbor inputs matched 0/653 versus 18/653 in
casual chat and 0/1,448 versus 28/1,448 in accommodation dialogue. Changed
transpositions produced no initial matches in either engine. Deleting and
retyping restored complete-input coverage, at the explicitly recorded extra cost.
Unchanged transpositions are flagged and excluded from those error-rate claims.

Do not attribute these losses to all configurations of the upstream engine:
its predictive search mode is a separate, untested candidate. Benchmark that
existing mechanism on development data before inventing another completion layer.
No typo robustness or touch hit-rate claim follows from these synthetic errors.

## Desktop resource cost

Three isolated processes per engine, alternating order. Each pass types 64 fixed
development clauses, 1,311 keys, then repeats them without a result cache. Engine
time excludes protocol I/O; transport-inclusive timings are retained separately.
First-cycle p95 is also retained; it does not improve the converter's conclusion.

| Repeat-cycle measurement | MinIME shared core | C++ converter |
|---|---:|---:|
| Engine p50 across three passes | 0.139–0.142 ms | 4.55–4.57 ms |
| Engine p95 | 0.723–0.770 ms | 36.67–36.76 ms |
| Engine p99 | 1.02–1.12 ms | 62.77–63.25 ms |
| Worst observed repeat query | 1.41 ms | 90.97 ms |
| Load time after process starts | 848–852 ms | 306–309 ms |
| Required data loaded by adapter | 50.37 MiB | 37.70 MiB |
| Process RSS immediately after load | 290–291 MiB | 69.6–69.7 MiB |
| Process RSS after typing pass | 404–413 MiB | 70.5–70.7 MiB |

The Java process includes MinIME's base Chinese/English model and JVM; the C++
process is only a Japanese converter. These are not interchangeable responsibilities
or Android memory estimates. Adding the converter initially adds its 37.70 MiB
of data; no APK-size or net-memory saving is demonstrated. Initialization uses
warm filesystem caches, not a reboot-cold-storage measurement.

The converter passes this experiment's coarse desktop p95 <50 ms / data <64 MiB
screen. It consumes most of the product's **50 ms p95 / 80 ms p99 stable-suggestion
budget before Android scheduling or rendering**, so it is not accepted for
synchronous per-key use. It has no demonstrated phone latency or power result.
Raw spelling and touch feedback must remain independent of conversion work.

## Next implementation boundary

1. Benchmark upstream predictive search and bounded conversion work separately,
   retaining these results as regression data and reserving new source documents
   for the next holdout. Compare actual Mozc before choosing a permanent backend.
2. If supported, put sentence conversion behind an optional Japanese provider.
   Keep scheduling, cancellation, composition ownership, prefix-consumption
   validation, visible/default candidate identity and acceptance in shared core.
   Keep Japanese segmentation, connection costs and inflection in the provider.
3. Measure a native Android build against section 28 before enabling it: rapid
   typing, stale-result rejection, frame stability, acceptance, memory and touch
   feedback. The authorized phone remains untouched during SHINE's reservation.

Taiwanese phrase composition remains a separate data/orthography experiment;
Japanese connection costs provide no evidence for applying them to Taiwanese.

## Reproduce and verify

Use the existing modern Python reference environment from the conversation
benchmark (SudachiPy 0.6.10, Sudachi core 20250129, jaconv 0.4.0). Restore the
already pinned RealPersonaChat/ASDC archives using that benchmark's downloader.
The Windows build script uses this workspace's installed GCC 10.3, CMake and JDK17.

```powershell
python docs/japanese-engine-benchmark/fetch.py
./docs/japanese-engine-benchmark/build.ps1
python docs/japanese-engine-benchmark/make_corpus.py
python -m unittest discover -s docs/japanese-engine-benchmark -p test_evaluator.py
# Each engine: minime, kazuma. Each role: development, holdout.
python docs/japanese-engine-benchmark/run.py minime completion --role development
python docs/japanese-engine-benchmark/run.py minime probes --role development
python docs/japanese-engine-benchmark/run.py minime ajimee
# Three passes, sequential engines; reverse engine order in pass 2.
python docs/japanese-engine-benchmark/run.py minime perf --pass-number 1
python docs/japanese-engine-benchmark/overlap.py
python docs/japanese-engine-benchmark/summarize.py
python tools/sources.py check
```

Verified: 7 evaluator behavior/partition tests; 19 source-framework tests;
289,460 shared-core assertions; the pinned desktop Rime evaluator completed
13,014 inputs. Corpus regeneration and converter-data rebuilding reproduced
their hashes. Production input/add-on ledgers are unchanged. No app build,
installation or phone test was needed for these evaluation-only changes.
Raw action/candidate events, 32,964 condition probes, 15,732 timing samples,
source pins, notices, load/memory snapshots and negative results are committed.
