# Phone reference and broader reading-prior recheck

September 24, 2026. **Keep the source-prior trial outside production.** The
reference study supports its direction, but additional source/evaluation defects
need resolving before a data refresh.

## Observed phone behavior

The [predeclared plan](READING-REFERENCE-PLAN.md) selected 24 inputs by hash, in
three equal strata. Plan SHA-256 is
`094a47cf6a96ad6d49512394742445238649a57526fb23c279b2606a03a89802`.
Both providers completed all cases: 48 observations and 96 captures. All Google
raw spellings were checked visually, ignoring visual syllable spaces; all MinIME
raw spellings matched the editor. Every accepted composition finished. The
accepted MinIME phone default matched the shared-core baseline in all 24 cases.

| Selected stratum | Cases | Baseline matches Google Space | Offline prior trial matches Google Space |
|---|---:|---:|---:|
| Source-complete inputs with changed dictionary leader | 8 | 0 | 5 |
| Partial inputs with changed first-eight order | 8 | 1 | 1 |
| Unchanged source-complete controls | 8 | 3 | 3 |
| Total diagnostic sample | 24 | 4 | 9 |

Five cases gain agreement, none lose it. Agreement is exact accepted text after
removing trailing ASCII spaces only; case and glyphs are retained. These are
deliberately selected diagnostic cases, **not general typing accuracy**. The
source syllable inventory includes marginal forms such as standalone `p`; the
stratum name does not independently certify a linguistic syllable.

Examples selected by the rule, not by their outputs: `nuo` changes 哪 → 諾,
`yue` 說 → 月, `zong` 從 → 總, and `nei` 那 → 內. Google agrees with these
trial defaults. `yun` still differs (trial 運, Google 雲), and `ga` exposes
another poor reading/default (trial 高, Google 嘎). No per-word adjustment follows.

The study also exposes a separate intent-policy gap. On `w`, `sh`, and `y`,
accepted MinIME preserves Latin input while Google commits 我, 是, and 有.
Reading weights cannot fix that ownership boundary. Google also corrects `gon`
to `gin`; its outputs are observations, not universally correct labels.

Phone session `5e0fdb6d-adaf-4b65-9ad9-0eb04366302d` used the already-tested
accepted runtime APK and legacy Google 2.4.5.164561151. Trial weights were never
installed. The original APK, settings, learning and prior Samsung IME were
restored, display OFF was verified three times during cleanup, the mutex was
released and the coordinating task received explicit release. Google's existing
learning state was not erased, so this is not a controlled factory-fresh model.

## Reused broad and chat corpora

Run the same source-prior TSV through the current accepted shared engine, with
Taiwan/geography packs, on 31,527 broad and 4,608 chat conditions. These are reused
regressions; no new holdout claim. Expected text never enters decoding.

| Evaluation | Labelled cases | Whole target among first 8, before → after | Target-compatible first-8 slots, before → after |
|---|---:|---:|---:|
| Encyclopedic | 8,000 | 5,285 → 5,279 | 7,440 → 7,515 |
| Essay | 11,220 | 1,249 → 1,249 | 12,088 → 12,268 |
| Authored conversation scenarios | 24 | 0 → 0 | 38 → 38 |
| Seen-source retrieval | 5,000 | 4,999 → 4,999 | 11,059 → 11,123 |
| Edited chat excerpts/spans | 4,608 | 610 → 610 | 4,805 → 4,860 |

Compatible slots are candidates equal to the target or an accepted prefix of it;
they are not semantic-precision judgements of other homophones. First eight means
ordinal nonraw positions, not eight visible phone cells. Broad first-position
whole-target counts gain 20 and lose six; these are not Space statistics because
the raw recovery candidate can remain selected. Broad Space changes in 148
episodes; chat Space never changes. All tested suffix/acceptance contracts pass.

The 28 first-eight target losses are all single-character rows in the older
encyclopedic corpus. Its generator assigns one spelling per word by highest
whole-word frequency, retaining the **first file row on equal frequencies**.
All readings of a word have equal counts in that source. Across its 2,000 full
word cases, 42 have multiple distinct source Pinyin readings: 33 of 348
single-glyph cases and nine of 1,652 multi-glyph cases. No sentence pronunciation
annotation resolves those ties. This prevents interpreting every rank demotion
as poorer everyday language quality; it also does not make every demotion good.
Do not rewrite these labels using the proposed source weights and then advertise
the result as independent validation.

## Source-integrity findings and next loop

The pinned upstream compiler stores one reading per glyph per priority tier.
The complete three-list audit finds three glyphs with multiple distinct rows,
silently overwriting four readings. This includes a secondary neutral reading;
blindly importing the compiled weights would carry that loss into our model.
The lists' intended precedence needs validating, and a general set-preserving
parser experiment needs its own before/after evidence. No source-list edits or
glyph exceptions are authorized by this observation.

There are editorial discrepancies as well: the pinned primary list chooses
ㄆㄛ for 波, whereas the [MOE concise dictionary](https://dict.concised.moe.edu.tw/dictView.jsp?ID=105&la=0&powerMode=0)
uses ㄅㄛ and the [revised dictionary](https://dict.revised.moe.edu.tw/dictView.jsp?ID=598&la=0&powerMode=0)
identifies ㄆㄛ as an alternate. That warrants a systematic audit of the whole
priority source against independent Taiwan references, not a one-character fix.

Next: audit the source's multi-reading representation and primary-reading
coverage; separate pronunciation-unresolved evaluation rows from independently
resolved ones without deleting the old results; then retest a declared data
policy. In parallel, assess focused Chinese acceptance separately from English
completion display. Current production, release rights and platform gates stay
unchanged. No public release package is certified by this loop.

Aggregate receipts are in `reading-reference/`. Per-query outputs, source text,
phone captures and preference backups remain in ignored local artifacts.
