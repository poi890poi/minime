# Taiwan source audit and management decisions

Snapshot audit: 2026-09-08. Runtime baseline: MinIME 0.6.4 (`3a566a1`).

This change establishes a common source catalog on top of the existing dictionary,
Rudy, add-on and Rime manifests. It does not import new vocabulary or change ranking.
The measured candidates below are isolated pilots. Existing mixed-source inputs
are frozen with explicit replacement tracks; they have not yet been replaced.

## Selection and evidence

The search followed Taiwan-authored dictionaries, specialist databases, community
knowledge projects and institutional catalogs across nature, communities, history,
literature, music and everyday language. Rudy is the quality reference for local
curation, not a requirement that every sector use geographic POIs. Neither government
ownership, a `zh-tw` tag, source size nor a permissive license proves editorial taste.

The audit downloaded complete structured snapshots where available. Taiwan.md uses
a complete pinned file inventory and eight hash-selected articles per topical
category, chosen before reading content, with seed `minime-source-audit-v1`. There
are 104 topical samples. About/resources evidence is preserved but excluded from
topical counts. Kemdict's completed `Words` section is counted in full; `Todo` is
excluded. All 80 CSVs linked in the official music catalog were fetched. The saved
fetch error inventory is empty. No selected word list feeds production.

[Measurements](audit-2026-09-08/measurements.json) record category counts, field
completeness, duplicate fields, source revisions and baseline asset hashes.
[Artifact fingerprints](audit-2026-09-08/artifacts.json) and the source ledger pin
the raw audit evidence. Scripts reproduce these measurements offline from committed
archives; the network collector is a separate, cached discovery operation.

## Measured candidates

| Source | Measured snapshot | Strength and decision | Main limits |
|---|---|---|---|
| [TaiCOL](https://ipt.taibif.tw/resource?r=taibnet_com_all&v=1.13) | v1.13: 111,267 taxon rows; 48,035 nonempty Chinese name fields; 45,616 distinct fields | Specialist taxonomy tied to literature; first nature-sector pilot | Accepted taxa and synonyms differ; a field is not necessarily one species or one alias; no Mandarin readings or typing frequencies |
| [如月的現代台灣華語補足典](https://github.com/kemdict/kemdict/tree/main/dicts/kisaragi) | 346 completed entries, 345 distinct headings; 314 entries with Zhuyin | Authored Taiwan Mandarin supplement; usage/reading pilot | Small, topical and sometimes dated; not a general frequency model; wrapper licenses do not relicense imported dictionaries |
| [CIP community inventory](https://data.gov.tw/dataset/156632) | 739 records; 708 distinct name fields; every record has original romanization | Community identity and spelling pilot | Administrative recognition omits historical/unrecognized settlements; repeated names need location identity; do not normalize away original scripts |
| [BAMID music awards](https://opendata.culture.tw/frontsite/openData/detail?datasetId=581) | 80 CSVs; 5,456 rows; 8 schemas; 3,196 distinct nonempty work fields | Attributed artist/work catalog pilot | Awards and nominations overlap and select only part of musical culture; companies share credit fields with people; song/album/title boundaries need parsing |
| [Taiwan.md](https://github.com/frank890417/taiwan-md) | 1,110 topical article filenames across 13 folders; 104 sampled articles | Taiwan-oriented discovery and essay-evaluation pilot | Explanatory titles are not canonical names; AI-assisted content and human-review status vary; not a ready lexicon or conversational frequency corpus |

TaiCOL contains 68,213 accepted, 36,345 invalid and 6,709 homotypic-synonym rows.
These are taxon rows, including those without Chinese names. Parsing must retain
the status relationship rather than promoting every field equally. Its published
v1.13 snapshot is dated 2024-07-31; that is the audited version, not a claim about
the most recent TaiCOL service. The official archive includes attribution and
CC BY 4.0 metadata. [Snapshot and metadata](https://ipt.taibif.tw/resource?r=taibnet_com_all&v=1.13)

CIP has 31 repeated nonempty name fields. Two fields end in `部` rather than `部落`;
that is a validation flag, not proof of an error or permission to invent a corrected
name. 737 records carry date 20220725, with one each at 20240725 and 20241211.
Administrative labels and original romanizations must remain linked to their
record identities. [Official dataset](https://data.gov.tw/dataset/156632)

The music CSVs cover editions 1–35. Five rows lack a year and four lack an edition;
the year field has no 91 and an unusually large count at 90. These are observable
metadata gaps, not yet a diagnosis of which individual records are wrong. There
are 1,261 work fields containing brackets or slash separators, and 2,123 distinct
credited-party fields including organizations. A current catalog page does not
make this a complete or current inventory of artists, songs or albums.
[Official catalog and linked files](https://www.bamid.gov.tw/OpenData.aspx?SN=0FBD3266D50B85F7)

Taiwan.md may be the open encyclopedia project recalled in the request. The pinned
inventory has People 290, Society 116, Culture 113, Food 89, History 87, Geography
82, Economy 77, Technology 68, Art 48, Lifestyle 46, Nature 41, Music 37 and Politics
16 files. These counts describe repository organization, not equal coverage of
Taiwan life or canonical entities. In the 104 samples, `lastHumanReview` is false
for 90 and true for 14; 28 have `curation: incubating`. Two contain no URL and one
has undefined footnote references. Metadata and link checks are not fact-checking
or a numeric accuracy estimate. [Project editorial process](https://github.com/frank890417/taiwan-md/blob/main/docs/editorial/EDITORIAL.md)

A qualitative spot inspection within the frozen sample covered 日治時期文學,
羅發號事件與卓杞篤, 大腸包小腸 and 拍謝少年. Broad reference links and Wikipedia
dependencies make passage-level source checks necessary. This small inspection
does not establish representative factual accuracy. One suspected genre error for
《鬥鬧熱》was rejected: the NMTL specialist entry identifies it as a short story,
so it is not evidence against Taiwan.md. [NMTL entry](https://db.nmtl.gov.tw/site2/dictionary?id=Dictionary02073)

## Existing dictionary overlap and pronunciation feasibility

The comparison uses literal strings from the three current MinIME assets. It does
not resolve entity identity, aliases or semantic equivalence. The final column
greedily segments an eligible label into units having one reading in McBopomofo;
it is parser feasibility, not independently verified pronunciation accuracy.

| Snapshot field inventory | Distinct nonempty fields | Exact strings already in MinIME | Literal Han names, length 2–24 | Eligible names with unambiguous reading units |
|---|---:|---:|---:|---:|
| TaiCOL Chinese name fields | 45,616 | 1,138 | 45,458 | 26,523 |
| Kisaragi completed headings | 345 | 128 | 269 | 246 |
| CIP community name fields | 708 | 43 | 689 | 495 |
| BAMID work fields | 3,196 | 239 | 1,128 | 842 |
| Taiwan.md topical filenames | 1,110 | 386 | 1,007 | 907 |

The Han filter is a comparable audit slice, not a proposed production exclusion:
legitimate romanization, kana, punctuation and mixed-script names must be handled
according to their source. Reading support is a major remaining constraint,
especially for nature and Indigenous community names. Kisaragi's supplied readings
are preferable to inferred unit concatenation where applicable. None of these
counts justify candidate promotion, demonstrate full/partial lookup success, or
measure common-conversation quality.

## Other sources and uncovered sectors

| Candidate | Audit outcome and next admission requirement |
|---|---|
| [Taiwan Biographical Database](https://tbdb.ntnu.edu.tw/) | Academic historical-person authority worth pursuing. A guide describes relation export, but this audit did not obtain a verified reusable bulk snapshot, period denominator or license. Hold; no historical-person coverage claim. |
| [NMTL literary dictionary](https://db.nmtl.gov.tw/site2/) and [author database](https://db.nmtl.gov.tw/site4/s7/index) | Specialist literary entries are useful evidence, including Japanese-era literature. Bulk access and derivative reuse terms remain unresolved. Hold; do not substitute modern citizenship gates for historical affiliation. |
| [National Cultural Memory Bank](https://tcmb.culture.tw/zh-tw/OpenApi) | Broad local/institutional collections; open API scope is narrower than the portal, detailed access requires a key, and rights vary by object. Hold individual datasets until their export and terms are verified. Image/caption titles cannot all become phrases. |
| [MOE / Moedict](https://www.moedict.tw/about.html) | Original edited dictionaries with readings; revised and concise editions have different purposes. Reference use only in this catalog; CC BY-ND terms are not removed by an open-source wrapper. No new production extraction approved. |

Film, books, food/beverages and historical figures still need independently
measurable source inventories. Music awards do not close film/book gaps; Taiwan.md
article counts do not close food or history gaps. Existing Rudy geography is retained
as its own source. Japanese and Taiwanese everyday dictionaries keep their separate
identities; they are not narrowed to Taiwan place names by this framework.

## Evaluation roles: essays and conversation

Essays are useful testing material. Preserve their strengths—specialist terms,
names, longer clauses and contextual punctuation—without treating their vocabulary
distribution as everyday conversation. Conversational testing needs separate short
turns, fragments, colloquial wording, code switching and contraction coverage.
Apply the same complete/initial/mixed input and controlled imprecision conditions,
but report results by genre, language and error condition with separate denominators.

`sources/evaluation.json` reuses the existing GUM corpus and its 15 genre labels,
including 193 conversation and 60 essay rows. It also registers the existing mixed
13,014-row regression corpus with an explicit warning that its Chinese Wikipedia
spans are not representative Taiwan conversation. The Taiwan.md sample is registered
as inspected essay audit evidence. No fresh typing benchmark or accuracy score is
claimed: target/readings, document/passage overlap and source lineage must be checked
before running a candidate-quality experiment.

Evaluation files are pinned apart from production inputs. Document or conversation
groups must remain together across splitting and simulated input variants. All
already inspected/evaluated corpora are marked accordingly. If essay-derived terms
later enter a dictionary, affected essays can still test seen-source retrieval;
generalization needs a new independently held-out source set. File separation alone
does not prove absence of copied or paraphrased passages.

## Migration and negative result

The catalog preserves Rudy, McBopomofo and established language-specific sources
for their recorded uses. CC-CEDICT-derived Taiwan usage, Wikidata labels, Wikipedia
category imports, the OpenCC detector, native Rime data and UD context training have
explicit replacement tracks. The ledger freezes their existing registered inputs
and add-on contributions; it does not claim all native derived outputs are frozen
or eliminate their current release vocabulary. Existing asset integrity checks
continue to cover packaged outputs.

An initial mechanical experiment tightened everything to `zh-tw` labels and
McBopomofo-only inferred readings. It was discarded before any asset compilation:
locale tags and conversion detection do not establish local authorship, editorial
quality or natural wording. That approach cannot answer the user's source-quality
request. No benefit is claimed for that rejected experiment.

Promotion requires an independent source decision, a reproducible whole-source
parser, explicit exclusions/identity handling, independently justified readings,
full/partial/mixed lookup evidence and common-language ranking checks. Source counts
or award status must not become frequency bonuses. Benchmark dictionary load time,
lookup latency and memory separately before shipping each optional add-on.

## Verification and scope

The source checker runs offline in CI and before the five existing compiler entry
points. It validates registered ownership, source admission, input fingerprints,
audit evidence, evaluation roles and packaged add-on provenance. Frozen data cannot
be approved merely by refreshing its lock. Contract tests inject drift, unknown
provenance, invalid admission, file-role collisions and false holdout declarations.

The add-on compiler rebuild produced unchanged release assets; existing asset,
model and Rime integrity verification passed. The context compiler also rebuilt
identically after checking its decompressed training hashes. All 19 source-contract
tests passed, and audit reproduction without the retrieval cache was byte-identical.
The ledger covers 24 sources, 60 production input files, seven add-on provenance
groups and three separately declared evaluation corpora. There are no Android behavior or
dictionary changes in this work, so there is no new APK and no phone session.
Source-quality accuracy, semantic overlap detection and new lookup performance
benchmarks remain outside the claims of this framework commit.
