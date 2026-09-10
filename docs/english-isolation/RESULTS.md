# English suggestion isolation

## Before implementation

Type: bug fix for English board dictionary scope, plus an explicit clarification
of user-dictionary behavior. Reported release: 0.6.4 (`3a566a1`); current runtime
baseline: 0.7.4 (`cec2263`). The report is not dismissed as an obsolete version.

An isolated core probe compiled the historical sources without changing the
working checkout. Identical synthetic add-on/custom records and English input
`fixture` produced:

| Core revision | Configured add-ons only | Same with custom entries |
| --- | --- | --- |
| 0.6.4 | fixture, かな, 文化, fixtureword | fixture, 自訂詞, カナ, かな, 文化, fixtureword |
| 0.7.4 | fixture, fixtureword | fixture, 自訂詞, カナ, fixtureword |

Cause: 0.6.4 passed every enabled pack to lookup even in English mode. Current
`InputMode.packs` already excludes them. A remaining path in `applyCandidates`
inserts custom output without a language check, sometimes re-labeling it as
`literal=true`. Literal is an acceptance/spacing flag, not evidence of English.
The old regression explicitly allowed a Chinese custom entry in English; the
new requirement supersedes that behavior. `Learning.predictEnglish` also trusts
stored text without checking script; this is a defensive migration case, not an
observed source of this user's report.

Boundary: shared-core suggestion admission in the explicit English board.
Reject known foreign pack provenance and non-Latin-script suggestions. Untagged
Latin custom entries retain their user's intent; script alone cannot identify
their language. Filter custom entries before they influence ranking or suppress
apostrophe restoration. Apply the same gate to idle predictions. No per-word
exceptions, source refresh, score tuning, stored-data deletion or schema migration.

Preserve mixed/focused modes, explicit raw recovery, literal/symbol insertion,
English custom entries (including accents), normal corrections/apostrophes,
composition ownership and stale-callback/selection rejection. Risks are overbroad
script filtering and mutating immutable Learning results; use source metadata,
Unicode scripts and copied lists, with paired negative controls.

Verification plan: failing current-core regression before the fix; historical
probe; full shared core; pinned desktop Rime before Android build. Device tests
are reserved for integration changes. Existing phone timing and human-input
results do not certify the new performance acceptance requirements.

## Verification results

- The new `EnglishIsolationRegression` fails against the 0.7.4 core at the first
  foreign custom output (`自訂詞`), and passes with the fix. It covers immutable
  custom lists, misleading literal flags, supplementary Han, full/half-width kana,
  mixed Latin/Han, tagged romanization, valid Latin/diacritics/technical text,
  explicit raw recovery, all other modes, saved predictions, stale callbacks and
  candidate identities. These are synthetic scope tests, not language accuracy.
- `tools/test-core.ps1`: **34,120 assertions pass**. The existing hash-selected
  production sample covers 24 readings per pack (96 total), now with full/half
  English scope comparisons (192 queries). All enabled packs leave English order
  identical. The 52 source-annotated contraction probes additionally verify that
  excluded custom output cannot suppress apostrophe restoration.
- `tools/test-desktop.ps1 -Corpus docs/suggestion-latency/native-inputs.tsv`:
  **628 uncached native Rime queries**, 480 output records. Parsed results match
  the retained 0.7.4 `artifacts/two-language-modes/native.jsonl` exactly. This
  verifies unchanged base behavior with no custom entries, not a latency speedup.
- One test-harness correction was necessary: the shared `Regression.find`
  deliberately throws on a missing candidate, so absence checks now use an
  explicit index/stream check. The corrected regression still fails on old core.
- No device session was started for this core-only scope fix. Android packaging
  is recorded with the release. Touch dispatch and UI rendering were not changed.
  Human accuracy and visible-frame latency remain **NOT MEASURED** against the
  new requirements. The full core run's 118 ms maximum is retained in its log;
  it includes the synchronous desktop test path and is not a phone UX PASS.

Raw local evidence: `artifacts/english-isolation/{legacy-probe.txt,
current-probe.txt,regression-before.txt,core-final.log,desktop-after.log,
desktop-after.jsonl}`. The historical probe used only synthetic records; no
production dictionary entries or ranking weights changed.

## Release 0.7.5

Version code 23 packages the fix. Android `assembleDebug` and `lintDebug` pass
(zero lint errors, 17 warnings). APK asset/model verification, signature
verification, version read-back, ZIP CRC and embedded-APK identity checks pass.
The public Cloudflare download was fetched in full and matched the local ZIP's
SHA-256. Exact sizes/hashes and test status are in [release.json](release.json).

Additional source check: all 44,127 `en_us.tsv` entries match the Latin-word
pattern; none of 46,225 English context records contains CJK output in the
audited Han/kana ranges. This supports the identified scope/custom-entry cause;
it is not a semantic English-vocabulary accuracy claim. No source data changed.

No phone was awakened, installed or reconfigured during this change. Phone UI
integration was not rerun, and this release does not claim the new performance
acceptance gates pass.
