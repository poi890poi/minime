# MinIME 0.6.0: optional source packs and local phrase learning

Baseline: 6ddbfe9 / 0.5.6. Buffer fix: 1687dd5. All language tests ran in the
shared core and desktop Rime before Android validation. No complaint-specific
word overrides or model-weight changes were introduced.

## Shipped behavior

- The nonempty Chinese composition buffer stays visible across literal defaults,
  empty predictions and asynchronous replies. Keyboard height stays fixed.
- Taiwan, Rudy geography, Japanese and Taiwanese POJ packs have independent default-off switches.
  Supplemental matches appear near the front, including names previously buried
  among duplicate base candidates. The exact Space default remains unchanged.
- Repeated Chinese phrase learning has a separate default-off switch. After three
  accepted occurrences, an exact reading/phrase pair becomes an explicit candidate.
  Adjacent accepted segments can combine, including partial phrase selections.
  Storage is bounded to 512 pairs, outputs to 2–16 Han characters, and each chain to
  eight segments. Edits, punctuation, field/language transitions and literal input
  break chains. Only accepted MinIME-owned readings are observed; editor
  surroundings are never collected. Private/secure/literal input is excluded.
- Settings provide source lookup, source/licence summaries, learned-phrase review
  and deletion. Turning a feature off stops its use; it does not silently erase data.
- Sync is a [separate proposal](SYNC-PROPOSAL.md), not an implemented transfer.

## Data and provenance

The [source manifest](source-manifest.json) and [Rudy manifest](rudy-manifest.json)
pin dataset versions, hashes, licences, general selection rules and skipped readings.
No production names, phrase IDs, aliases or per-word promotions are handpicked.
The earlier manual hiking list and POJ/Japanese selectors were rejected and removed.
Source IDs already supplied by upstream are preserved automatically; no per-name
article-link list is maintained.

| Pack | Distinct indexed outputs | Systematic source selection |
|---|---:|---|
| Taiwan | 2,353 | CC-CEDICT Taiwan metadata; recorded Wikidata title queries |
| Rudy geography | 19,373 | All descendants of hiking/nature/place/waterway/history categories |
| Taiwanese POJ | 356 | Short iTaigi expressions with Mandarin source frequency >=1,000 |
| Japanese | 4,151 | JMnedict works, creative-profession and Taiwan-related metadata |

General packs contain 11,641 rows; the separately licensed geography pack contains
54,969. Counts include aliases and local usage variants, not all new proper names.
Japanese output counts include names, kana and Romanized spellings. WanaKana 5.3.1
supplies build-time Romanization. Neither Japanese source categories nor bounded
Wikidata title queries establish popularity in Taiwan. POJ selection uses written
Mandarin frequency as a proxy, not a measured spoken-Taiwanese popularity ranking.
It retains every eligible original source variant.

[Rudy Map](https://rudymap.tw/) POIv3 snapshot 2026.09.03 contains 765,630 records.
The importer selects 335,387 thematic records, yielding 30,962 distinct eligible
Han names after generic alias/script/length rules. It resolves 19,373 names:
3,148 through upstream pronunciation tags, 1,831 through exact McBopomofo entries,
and 14,394 through unambiguous dictionary units. 19,367 have Pinyin keys; six have
only Zhuyin. The other **11,589 names remain unresolved** in the complete normalized
source snapshot. Derived readings are not independently verified local-name
pronunciations. POI way-segment counts are never treated as popularity.

The complete machine-extracted snapshot, importer and generated geography database
are provided under ODbL 1.0, separate from CC BY-SA language assets. Source licences
and notices are available in Settings. The 64 MB upstream ZIP is fetched separately;
offline rebuilding uses the vendored normalized snapshot. General imports skip
279 unsupported or ambiguous source readings/aliases. No articles, photographs,
map tiles, definitions or lyrics are bundled.

Offline rebuilds of both dictionaries and notices are byte-identical; hashes are
recorded in [rebuild verification](rebuild-verification.json). Evaluation files are
never read by the production compilers.

POJ is the entire orthography, not a ts/ch substitution of Tâi-lô. Output uses
source POJ tone marks, vowels, o͘ and ⁿ. The letter board uses untoned joined POJ
aliases, with oo/nn input encodings. Original source-numbered aliases are retained
in the data and tested at core level; onscreen numeric slides still commit literal
digits. This is a short-phrase pack, not a full Taiwanese or Japanese decoder.

## Independent Wikipedia evaluation

Wikipedia supplied factual article titles, not training data. The compiler never
reads the holdout files. Snapshot API URLs and page IDs preserve provenance to
Wikipedia contributors. Categories were sampled deterministically with one
subcategory layer; some parent categories were empty or had only index articles.
Dictionary-based Pinyin preparation rejects ambiguous readings and removes only
trailing title-disambiguation parentheses. Its eligibility rate must not be
confused with candidate coverage or independently verified name pronunciation.

Development: 437 distinct titles, 291 eligible readings, 146 unavailable/ambiguous;
873 full/initial/mixed queries. A first implementation that discarded existing
duplicates yielded almost no visible benefit. This negative result is retained in
`wiki-addons.jsonl.gz`. The causal issue was duplicate suppression retaining a
matching source name deep in the base list. The revised general membership rule
promotes that match while preserving the default and other candidates' order.
No individual Wikipedia misses were added to the dictionary or reweighted.

A second snapshot was fetched after proposing that rule. It contains 362 disjoint
titles: 258 eligible readings and 104 unavailable/ambiguous; 774 queries. It was
fresh for the promotion experiment; it is now a frozen regression sample for the
systematic source replacement. No entry selection used its labels.

| Frozen second sample query style | Baseline top 8 | Optional packs top 8 | Baseline anywhere | Packs anywhere |
|---|---:|---:|---:|---:|
| Full Pinyin | 60 / 258 | 65 / 258 | 109 / 258 | 110 / 258 |
| Initial-only | 14 / 258 | 18 / 258 | 44 / 258 | 47 / 258 |
| Alternating initial/full | 37 / 258 | 37 / 258 | 67 / 258 | 67 / 258 |

Space output changes: **zero** in the second sample. Development top-8 counts
were 88→117 full, 16→23 initial, 43→43 mixed, each out of 291. Compared with the
rejected pre-systematic draft, second-sample initial-only top-8 retrieval falls
from 19 to 18; that negative result is retained without per-name tuning. These are conditional
name-retrieval results, not conversational accuracy or broad model improvement.
The improvement is modest and mixed abbreviation remains a gap in the add-ons.
Raw records, eligibility exclusions and hashes are retained beside this report.

## Verification

- Shared core: **12,857 assertions**, including 411 frozen continuity inputs,
  84 source-derived add-on probes, buried-duplicate promotion, default preservation,
  POJ marks/aliases, privacy, bounded storage and repeated/partial learning.
- Packaged binary model SHA-256 remains
  `1d1b9a2d5379eeb0099b38cb48695a22541300c765a0dd2d5851c25b049572f1`.
- Debug app, test APK, all four native ABI release builds and lint pass. Lint has
  0 errors and the same 17 warnings as 0.5.6. Release APK is unsigned; the distributed
  installable APK uses the existing development signing key.
- Phone RFCR91GWXLX: **14 tests passed in 52.932 seconds**. These cover packaged
  general/geography asset loading, local persistence, actual service candidate taps in Chinese and
  English, Japanese kana, option disable/restart, seven candidate/buffer stability
  cases, English literal defaults, fixed height, lifecycle and Rime partial selection.
  The earlier buffer-only baseline failed before the fix; its fixed run passed
  11 tests. Both raw logs are retained. The prior Samsung IME and MinIME preferences
  were restored, and the phone was slept with `mWakefulness=Dozing` verified.

- The 13,014-input conversational regression (21,044 output records) is byte-identical
  with optional features off: SHA-256
  `63b9b5e1a6f64ae68ef58c1b1af71a2f94219e6a3eb694207270476f72a6c3c3`.
- Desktop timing is not an Android latency claim; assertion counts are not language
  accuracy. Source-derived probes validate integration, not independent vocabulary
  quality. Raw Wikipedia records include all misses and eligibility exclusions.

No claim of parity with Google Zhuyin, comprehensive Taiwan vocabulary, complete
Japanese/Taiwanese input, improved general English ranking, or implemented sync
is made. Learned phrases currently require the recorded reading (apostrophes are
normalized); they do not infer every unobserved phonetic abbreviation. Supplemental
choices do not train the base preference model or seed further phrase chains.

## Package

MinIME 0.6.0 (version code 13), built from implementation commit `05abd3e`.
[Package hashes](package.json) record the 65,566,664-byte development-signed APK
and 35,925,134-byte ZIP containing that APK only. The complete public Cloudflare
ZIP download returned HTTP 200 and matched local bytes and SHA-256; see
[download verification](download-verification.json). The tunnel is temporary.
