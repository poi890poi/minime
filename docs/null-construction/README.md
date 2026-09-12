# Construction must earn inclusion

September 12, 2026. This is an isolated experiment against **no automatic
construction**, not a release change. Production dictionaries, decoders, ranking
and default settings remain unchanged. No contextual model was added.

## Decision

- **Null remains the experimental default.** It queries stored entries through
  the existing lookup paths and supports explicit incremental selection.
- **Java construction: do not enable under this contract.** It adds deep-list
  reference coverage, but no first-eight coverage or selection-effort advantage
  here, and substantially increases core processing time. With native construction
  present it produces the same first-eight and Space results as native alone.
- **Native Rime construction: retain as an experiment, not an approved default.**
  It adds selectable clauses and reduces some selection effort, but provides
  little first-choice improvement and no reserved Space-hit improvement. Natural
  Mandarin conversation and independent semantic/error-cost evidence are missing.
  Whole-clause recall alone does not earn inclusion.
- **Both: no justification over native alone** on the measured first-page and
  acceptance outcomes. This finding is conditional on the current merge, ranking
  and acceptance rules, which were deliberately held fixed.

These are decisions about this measured configuration, not proof that every
possible construction algorithm is unhelpful. Nor is this a claim that null's
current coverage is sufficient for release.

## Controlled experiment

Pinned Java revision: `7f0f179`. The exported experiment inserts one guard at each
Java traversal's continuation boundary. Root/single-entry lookup, scores, budgets,
partial matching, filtering and acceptance are preserved. Character-span and
reading-unit joining have independent switches; the four main configurations
compare both Java paths as the existing provider. Synthetic checks independently
exercise each switch; they are mechanism checks, not language accuracy.

Native on/off use the same Clang toolchain, Rime 1.16.1 sources, dependencies and
model. Off skips only `MakeSentence`. Native null produced zero sentence records
across all 11,220 queries, and the null core replay asserted no constructed output.
The static native on control matches the previous upstream-DLL text/order/span/
origin records on all 5,100 development queries and 6,118/6,120 reserved queries.
Two sentence choices differ across those toolchains; they are retained in raw
evidence. The controlled on/off comparison uses the same static build throughout.

All four variants replay all queries, with Taiwan/geography add-ons off and on:
89,760 core observations. Targets enter scoring only. Data is previously consumed
Taiwan.md prose, not fresh holdout or spontaneous conversation. Existing source
and reading-lineage limitations apply. GUM adds separate English conversation and
essay checks; it does not fill the Mandarin conversation gap.

## Reserved prose results

Full spelling, add-ons enabled, **1,530 queries per configuration**:

| Configuration | Reference at rank 1 | Reference in first 8 | Reference anywhere in bounded list | Space hits |
|---|---:|---:|---:|---:|
| Null | 196 | 199 | 244 | 231 |
| Native only | 204 | 674 | 710 | 231 |
| Java only | 196 | 199 | 1,152 | 231 |
| Both | 204 | 674 | 710 | 231 |

Rank 1 excludes the raw-literal slot. Space follows the actual preferred candidate
and confirmation rules; its hit rate is a different metric. Java's deep-list gain
must not be hidden, but reaching a reference far down the list is not first-page
utility or semantic precision. Native full-spelling rank-1 gain is 0.52 percentage
points; paired document-bootstrap 95% interval is approximately 0.07–1.11 points.

Native versus null on the other reserved conditions (1,530 each, add-ons on):

| Input | Null → native first 8 | Null → native rank 1 | Null → native Space |
|---|---:|---:|---:|
| Initials | 167 → 173 | 107 → 107 | 124 → 124 |
| Mixed | 207 → 307 | 145 → 147 | 184 → 184 |
| Partial | 35 → 35 | 4 → 4 | 68 → 68 |

Java changes none of those first-eight/rank-1/Space counts. In development it
loses one full-spelling rank-1/Space hit with add-ons; no word exception was added.
Native construction appears in the first eight on 929 initial, 1,188 mixed and
1,190 partial reserved queries. That exposure is not a nonsense count: alternatives
may be valid, and no independent semantic grading has been performed.

All add-on conditions, development/reserved splits, MRR and paired document
intervals are in [summary.json](summary.json). Raw lists retain candidate text,
consumed length, construction origin, preferred choice and actual Space output.

## Incremental selection and performance

An additional 512 hash-selected tasks use an ideal-user controller: choose the
longest correct target prefix already present within the first eight candidates,
then let the real engine continue with remaining phonetics. The reference guides
selection only, never decoding. This is a greedy policy, not an optimal-path
guarantee or a measurement of human time/corrections. All failures remain counted.

Across the 128 full-spelling tasks, null completes 100; native completes 98.
Of 97 common successes, native saves about 1.01 selections per task. It gains one
completion and loses three. Java has the same completions and selection counts as
null. Mixed completion is 58/128 for null versus 59/128 for native; initials and
partial completion counts are unchanged. The low partial results remain a gap.

Three sequential, order-balanced JVM passes use the same 128 hash-selected
queries with add-ons off/on. Core flush p95 ranges:

| Configuration | p95 across three passes |
|---|---:|
| Null | 2.88–3.48 ms |
| Native only | 2.87–3.77 ms |
| Java only | 12.44–14.29 ms |
| Both | 12.83–23.62 ms |

This excludes native execution, dictionary loading, Android dispatch and rendering.
It is not touch latency. Native timings are separately preserved with native raw
results; they were not measured on an idle phone. Dictionaries/model bytes are
identical; runtime peak memory was not measured. No memory reduction is claimed.

## English and Google Zhuyin

All four variants have identical GUM outcomes. In fresh Chinese/English mode,
conversation has 1,422/1,432 literal-word hits and ten Han conversions; essay has
1,066/1,067 hits and one Han conversion. The dedicated English mode preserves all
2,499 words. The separately reported simulated-primed conversation mode has 25
Han conversions. These interference problems survive null and need their own
general intent/data investigation.

Google Zhuyin `2.4.5.164561151-arm64-v8a` (code 2451413) was tested on the
authorized phone against baseline MinIME using 18 hash-selected cases: three per
Chinese spelling condition and three words per English genre, all on the Pinyin
board. This small sample is diagnostic, not a population estimate. Google output
does not supply training data or replace the independent source reference.

The original timed run completed all 36 provider/case observations. Google accepts
`zhongyangshebaodao` as 中央社報導; MinIME leaves the raw spelling as its Space
default. Google also generates poor combinations on some aggressive abbreviation
probes. The early MinIME candidate snapshot sometimes differs from the subsequent
Space outcome, so it cannot be called a settled-ranking measure. Both original
observations and every screenshot are retained under [phone/](phone/).

A fixed one-second delayed observation completed all 36 provider/case records,
retaining the original run rather than replacing unfavorable observations. The
same display/acceptance discrepancy remains: MinIME shows 鼓譟、古早、鼓噪… for
`guzaowei`, but Space accepts 古早味 outside the visible strip. Google displays
and accepts 古早味. This does not support the initial settling-only hypothesis.
`KeyboardView` renders list order and highlights `preferred`, while the engine can
select a later full-input default behind prefix candidates. The observation is a
separate display/default consistency gap; no per-word fix or runtime edit is made.

On this small delayed sample, Google exposes and accepts 3/12 exact Chinese
references; baseline MinIME exposes 1/12 and accepts 2/12. Google preserves 5/6
English words on the Pinyin board (he becomes 和); MinIME preserves 6/6. These
figures are diagnostic and not representative accuracy estimates. The field
requests no personalized learning; existing proprietary reference-model state
was not inspected or cleared. All observations and screenshots are retained under
[phone-delayed/](phone-delayed/) and [reference-comparison-delayed.json](reference-comparison-delayed.json).

Phone sessions `fd901ded-86d6-4149-89f2-f6db58e3a1bf` and
`1c171b93-3e00-421b-b290-93ad783960ad` each passed instrumentation, restored MinIME
preferences and the previous Samsung IME, and verified display OFF. Evidence was
collected under the mutex and both reservations explicitly released. No experimental
decoder was left on the phone. The wrapper also now has an outer display-off guard
for transfer-stage failures; that extra failure path was not fault-injected.

## Verification and reproduction

Local only; no GitHub Actions or hosted CI. Baseline core checks passed with
306,540 assertions; the pinned desktop entry point completed 13,014 probes.
Exported Java checks passed for null, each independent Java joiner, and both.
Android changes are observation tests only; the experimental decoders were not
installed. Source/evaluation registry verification is required before commit.

```powershell
./tools/build-desktop-metadata.ps1 -SourceBaseline -Output artifacts/null-construction/native-on.exe
./tools/build-desktop-metadata.ps1 -SourceOnly -Output artifacts/null-construction/native-off.exe
python -X utf8 tools/prepare_null_construction.py
python -X utf8 tools/run_null_construction.py native
python -X utf8 tools/run_null_construction.py replay
python -X utf8 tools/run_null_construction.py english
python -X utf8 tools/run_null_construction.py effort
python -X utf8 tools/run_null_construction.py timing
python -X utf8 tools/summarize_null_construction.py
```

Phone scripts require an explicit cross-task reservation before any ADB operation.
`study-parity.ps1` now holds the shared mutex around transfers, testing, restoration
and evidence collection. `test-device.ps1` restores preferences/prior IME and
verifies display OFF. Do not run the phone script as an unattended replay command.
