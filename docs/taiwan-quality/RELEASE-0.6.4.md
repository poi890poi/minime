# MinIME 0.6.4

This release preserves Rudy geography, expands the other Taiwan sectors through
source-linked encyclopedia entities, and corrects unweighted add-on glyph promotion.
The glyph fix and source expansion are independently reviewable commits.

See [source coverage and exclusions](SOURCES.md) and [glyph root causes](GLYPHS.md).
Taiwan outputs increase from 2,353 to 12,458; 10,105 distinct outputs are added.
The Japanese-era source stratum contains 1,521 verified humans, of whom 1,164 have
included, source-derived readings. This is a bounded source inventory, not complete
historical coverage or proof that every pronunciation is correct.

## Lookup and conversation results

Frozen lookup corpus: 67,497 cases / 58,398 distinct queries, including all included
entities' full/initial/mixed/prefix readings and a hash-selected legacy-pack sample.
Targets test dictionary retrieval, not independent language accuracy. All four packs
are enabled simultaneously. Before/after summaries and complete ranks are retained.

| Taiwan input form | Cases | Before top 8 | After top 8 |
|---|---:|---:|---:|
| Full | 10,429 | 389 | 10,399 |
| Initial | 10,324 | 271 | 9,006 |
| Mixed-left | 10,346 | 322 | 10,273 |
| Mixed-right | 10,339 | 319 | 10,253 |
| Prefix | 10,439 | 324 | 10,334 |

The isolated Taiwan pack reaches 9,894 initial targets, compared with 9,006 when all
packs compete for eight slots. Initial ambiguity remains real. In the frozen legacy
sample, full lookup is unchanged for geography, Japanese and POJ. Geography loses
one mixed-left top-eight hit; POJ loses six initial, one mixed-left and two mixed-right
hits. Japanese top-eight lookup is unchanged. Dictionary expansion is not free of
cross-pack competition; existing enable switches remain the isolation mechanism.

The separate 21,044-record conversation comparison isolates the data change from
the glyph fix. All Chinese Space outputs and top-one results are unchanged. All
43,411 English-board tokens are unchanged. One ambiguous token in each Pinyin mode
changes from 李剛 to 李崗 (`ligang`); neither spelling can be inferred as universally
correct from that raw name. Some Chinese top-eight groups lose one to three hits
per 1,000 cases. These losses are retained in entity-conversation-comparison.json.

The glyph-only comparison separately preserves all Space output across 16,208
audited inputs and all 130,233 English tokens in the conversation run. It removes
the measured static-source first-choice errors without changing native frequencies.
Native unusual readings, spelling expansion and source-frequency mismatches remain
diagnosed in GLYPHS.md; this release does not claim to resolve every rare candidate.

## Cost and verification

A separate serial desktop check uses the same 934 hash-selected queries before and
after, outside the concurrent coverage runs. Load time is 1.39 → 1.89 seconds;
lookup p50 is 1.35 → 1.47 ms and p95 is 6.72 → 7.17 ms. This is a single component
comparison, not end-to-end typing latency or a demonstrated speed improvement.

On phone RFCR91GWXLX, language-pack load is 5.83 s versus 4.17 s in 0.6.3. Geography
load is 3.43 s versus 3.48 s. Approximate retained heap is 53.2 MiB versus 42.4 MiB,
an increase of about 10.8 MiB. Fixed geography-query p50/p95 is 1.83/6.84 ms. The
partial-input component test records 1.27/1.95 ms with an 84.56 ms maximum; its
source-derived query sample changes with the asset and cannot support a before/after
speedup claim. Worker scheduling avoids performing lookup on the UI thread, but
does not remove load cost, memory use or delayed results. These remain optimization
targets.

- Core: 19,526 assertions pass, including source-derived probes, apostrophes,
  partial matching, glyph order, explicit selection and asynchronous acceptance.
- Desktop: full corpus and old/new lookup comparisons complete before Android.
- Source integrity: offline rebuild is byte-identical; Rudy geography is unchanged;
  Japanese/POJ rows and existing Taiwan reading aliases are preserved. Human types
  and changed-name Traditional source labels are checked against pinned metadata.
- Android: debug APK, instrumentation APK and lint build successfully. Lint has
  zero errors and 17 warnings. Four native ABIs, packaged data and unchanged base
  model hash are verified.
- Phone: all 21 editor, Rime, add-on, candidate-stability and component-cost tests
  pass. Previous IME and preferences are restored; display verified Dozing/asleep.
- Delivery: the public ZIP GET returns HTTP 200 and exactly matches the local ZIP
  checksum. See package.json and download-verification.json.

No layout, permissions, sync, personal-learning format, base model or native Rime
weights change. All dictionaries remain offline. Build/runtime tests do not establish
linguistic accuracy. Raw outputs, negative experiments, exclusions and source hashes
are retained so the remaining limitations can be investigated without example tuning.
