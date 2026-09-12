# Chinese live construction: evidence and policy

Rime has phonetic segmentation and lexical-weight logic. The packaged model does
not have a contextual grammar/language-model asset that checks whether the joined
words form a meaningful sentence. `script_translator` builds a word graph, and
`Poet` selects a path using entry weights and a fixed per-piece penalty. It then
places the assembled `sentence` before dictionary prefixes.

The Android bridge previously discarded native type and replaced ordering scores
with `100 - position`. Thus a generated sequence looked like an ordinary phrase
to MinIME. Optional phrase learning could further persist a generated output after
three automatic acceptances. The earlier [origin audit](../native-sentence-origin/README.md)
reproduced the reported sequences without Rudy, Java fallback or personal history.
This is runtime construction and lost provenance; it is not evidence that the
whole sequence was present in a source dictionary.

## Frozen benchmark

Baseline: `385a7f0`, Rime 1.16.1, model bundle
`145b3f69c8be5be50929587e5dede33300385ddf22aa326f840d12972128fb94`.
The native metadata reader checks every C++ candidate page against its same-session
public C API page. The production serializer also passed exact text, endpoint,
origin and ordering parity on **11,220 queries / 159,382 candidate records**.
Native wrapper tests cover a generated candidate shadow and a duplicate with both
generated and lexical origins.

There are 40 previously inspected development documents and 48 disjoint validation
documents from pinned Taiwan.md. Source text supplies the expected Han output;
unique original McBopomofo annotations supply readings. Documents and up to 32
spans per document were hash-selected before querying. No reference labels enter
runtime ranking, and no dictionary entries or frequencies were changed.

Each split contains four input conditions. Development has 1,275 spans / 5,100
queries; reserved validation has 1,530 spans / 6,120 queries. The reserved results
were inspected only after the policy in [PLAN.md](PLAN.md) was frozen. They are now
consumed regression evidence, not reusable fresh holdout data.

| Input condition | Development construction hits | Reserved construction hits | Reserved native query p95 |
|---|---:|---:|---:|
| Full spelling | 428 / 1,094 (39.1%) | 476 / 1,324 (36.0%) | 2.96 ms |
| Initials only | 5 / 796 (0.6%) | 6 / 929 (0.6%) | 7.49 ms |
| Alternating initial/full syllables | 90 / 997 (9.0%) | 100 / 1,190 (8.4%) | 6.83 ms |
| Last syllable omitted: target-prefix support | 196 / 998 (19.6%) | 247 / 1,190 (20.8%) | 2.93 ms |

Denominators count queries where construction was emitted, not all queries.
Full/initial/mixed hits require exact reference reconstruction. The last row asks
whether the generated output matches the beginning of the intended text; exact
whole-reference hits there were zero. Native timing includes session/query/menu
work, excludes process IPC and Android, and is not touch-to-display latency.

**Limitations:** a different reference output is not automatically meaningless.
Homophones and other valid sentences exist. This is prose reconstruction, not
human semantic grading or spontaneous-conversation accuracy. Taiwan.md has varying
editorial provenance, including possible AI assistance. Ambiguous/unmapped readings
and spans outside 3–16 Han glyphs were excluded; development/reserved had
9,975/12,038 reading exclusions. Reading annotation shares lexical lineage with
the model. Natural Taiwan Mandarin conversation evidence is still missing; the
existing Chinese UD audit words must not be relabeled as conversations.

## Confidence and rejected alternatives

Every generated sentence reported native quality **0**. That is an uninformative
native field, not a calibrated zero-percent success probability. Removing the
constant per-piece penalty from path weight did not produce a reliable confidence
threshold. For example, the development full-spelling subset with mean entry log
weight at least -6 hit 61/105; at least -5 hit 9/12. The latter is too small to
justify a high-confidence rule. No threshold was fitted to complaint examples.

Reject moving all lexical prefixes before construction: development full-spelling
first-eight reconstruction fell from 596 to 168 / 1,275. Putting three early
dictionary choices before unverified constructions retained first-eight coverage
in every development condition. No late construction is promoted by the actual
core implementation. Exact lexical duplicate evidence is preserved during native
uniquification and Java fallback merging.

## Implemented behavior

- Preserve native construction provenance through JNI, Java and candidate copies.
- Share the ranking rule for native and Java Chinese construction: defer early
  unverified constructions until three other choices have been shown; keep them
  selectable. Existing explicit context-scoped choice votes can override this.
- An unverified construction is not a whole-token Space/Enter default. Prefer a
  nonconstructed whole-input choice, otherwise preserve literal spelling. Prefix
  choices still consume only their advertised span when selected explicitly.
- Automatic constructed acceptance cannot teach the phrase lexicon. Explicit
  selection can teach it. Private-field protections remain enforced.

The same Chinese rule applies to Pinyin and Zhuyin. A former regression expected
automatic acceptance of a multi-entry Zhuyin phrase; it now verifies literal
recovery and a paired explicit-selection case. Another regression now checks
relative order within provenance groups, because add-ons legitimately change
how many early attested choices are available.

The policy is deliberately conservative. It does not supply the contextual model
that is missing, or claim that every attested word is common. Previously saved
phrase history has no provenance, so existing entries cannot be retroactively
classified safely; this change does not delete user data.

## Actual shared-core comparison

These are the real composition engine, Java fallback, English competition and
Taiwan/geography add-ons, with frozen native results replayed identically. There
are 1,530 inputs per reserved condition. First eight means candidate slots, not
eight items proven visible at a particular phone width.

| Reserved condition | First-eight reference hits, before → after | Automatic reference hits | Automatic nonreference conversions | Literal defaults after |
|---|---:|---:|---:|---:|
| Full | 674 → 674 | 671 → 231 | 859 → 46 | 1,253 |
| Partial | 35 → 35 | 0 → 68 | 1,529 → 385 | 1,077 |
| Initials | 173 → 173 | 113 → 124 | 1,408 → 507 | 899 |
| Mixed | 307 → 307 | 245 → 184 | 1,284 → 211 | 1,135 |

This preserves first-page availability but sacrifices many correct automatic
constructions: users must select them. It is an acceptance tradeoff, not a claim
of improved language-model accuracy. Results with add-ons disabled and complete
development results are in [core-comparison.json](core-comparison.json). Native-only
counterfactuals are separately labeled. Baseline constructed-default counts are
unavailable because the old bridge erased that metadata.

## Verification and reproduction

Run locally; never use GitHub CI. Prerequisites are the repository's pinned native
sources, MSVC Rime evaluation DLL, MSVC C++ tools, JDK 17 and Python. The metadata
build generates its own header and does not require an Android build directory.

```powershell
tools/build-desktop-metadata.ps1
python tools/make_construction_corpus.py
python tools/benchmark_native_construction.py --role development
python tools/benchmark_native_construction.py --role reserved
python tools/make_construction_replay.py
python tools/verify_construction_transport.py
python tools/run_construction_core.py --variant baseline --role development
python tools/run_construction_core.py --variant baseline --role reserved
python tools/run_construction_core.py --variant policy --role development
python tools/run_construction_core.py --variant policy --role reserved
python tools/summarize_construction_replay.py
tools/test-core.ps1
tools/test-desktop.ps1
java -cp core/build/manual dev.minime.core.ConstructionPolicyBenchmark artifacts/native-metadata/reserved-replay.tsv.gz
```

The baseline runner exports source from Git into artifacts without changing the
checkout; its evaluator-only adapter reproduces the old loss of origin metadata.
No network is needed with the committed corpus and existing pinned runtime.
`fetch_construction_validation.py` is an optional pinned-source refresh only.

Core regressions, native transport parity, pinned desktop evaluation and the
four-ABI Android debug build were exercised locally. No phone operations were
performed for this change. Signed release installation and visible candidate/
acceptance integration remain unverified; the existing Play kit stays review-only.
See [validation.txt](validation.txt) for recorded checks and isolated rank overhead.
