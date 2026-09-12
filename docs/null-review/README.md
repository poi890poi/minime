# No-construction review build

September 12, 2026. The production default is now the null baseline: stored entries
and explicit selections, without joining dictionary fragments into new Chinese
candidates. Native construction remains available in isolated experiment tooling.
No production words, source weights, or complaint-specific rules were added.

## Independently checked changes

1. The existing Space choice becomes the first visible candidate. A pending row
   retains its text without highlighting an obsolete default. Acceptance itself
   does not change.
2. Remove both Java joining paths and disable Rime `MakeSentence` in the native
   build. Reuse the reading index's existing single-entry lookup. The desktop gate
   now builds the same source-only translator as Android.
3. When Chinese is the selected interpretation, whole-input choices precede
   shorter-prefix recovery. Both groups preserve their internal order and every
   alternative remains available. The accepted choice is still first.

## Frozen replay

`tools/benchmark_null_review.py` records visible-default, stored-lookup and ordering
stages. `tools/report_null_review.py` compares them with the controlled null from
`../null-construction/`. Source hashes accompany the raw compressed observations.
The two existing document partitions are consumed regression evidence, not fresh
holdouts. Reference reconstruction is not semantic precision. No Mandarin
conversation accuracy claim follows from this prose corpus.

Reserved document partition, add-ons enabled, 1,530 queries per condition:

| Input | Prior null first 8 | Visible default | Final ordering | Space hits, unchanged |
|---|---:|---:|---:|---:|
| Complete | 199 | 234 | 244 | 231 |
| Initials | 167 | 179 | 208 | 124 |
| Mixed | 207 | 227 | 241 | 184 |
| Partial | 35 | 88 | 150 | 68 |

Single-entry lookup exactly preserved the visible stage's first-page results and
Space choices. Across both partitions and both add-on settings, 22,440 observations
per stage: zero construction candidates, zero Space changes, zero previously
successful first-eight targets lost. Defaults outside the first visible position
fell from 662 to zero. This fixes presentation and discoverability; it does not
improve automatic acceptance accuracy. Whole clauses without stored evidence may
require multiple explicit selections, and overall clause recall remains low.

## Local gates

- Shared core: 306,580 regression checks passed. This is a contract check count,
  not language-model accuracy.
- Pinned desktop Rime: 13,014 replay inputs completed successfully. Uncached native
  IPC plus decoder: 11,272 observations, p50 1.992 ms, p95 3.359 ms, max 132.403 ms.
  These desktop timings are not phone touch or displayed-frame latency.
- Review build: version 0.8.5-review1 (33), debug signed, separate from the Play
  production artifact. Android and reference results are recorded separately.

The original construction experiment remains intact, including selectable-clause
gains from native assembly and its negative greedy completion results. Removal
does not establish that construction can never help; it has not earned default
inclusion through a sufficient joint coverage, precision and effort result.
