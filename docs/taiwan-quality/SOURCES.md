# Taiwan dictionary source quality

Baseline: 0.6.3 / 3f25366. The Rudy geography snapshot, extractor and runtime asset are unchanged byte for byte. Japanese and POJ rows are unchanged. Existing Taiwan usage aliases are retained, with misleading keyword-derived person/history labels replaced by `taiwan_usage`.

Taiwan outputs: **2,353 → 12,458** (10,105 new distinct outputs). These are vocabulary counts, not language accuracy or popularity.

## Source and identity controls

The Chinese Wikipedia snapshot contains 15,228 distinct topic-linked pages from 1,225 categories. Every visited category is fully paginated; hard and soft redirects are resolved; retrieval completed with no errors. Topic roots cover multiple historical periods, indigenous topics, arts, science, nature, food and cultural works. There is no modern-citizenship filter.

The graph depth is two descendant levels. Unvisited frontiers are retained in the source manifest; this is not the complete encyclopedia. The previous Wikipedia evaluation inventories now overlap the imported source and cannot be presented as unseen accuracy holdouts.

Wikidata sitelinks and structured types independently validate people/performer identity. An artist’s category can contain albums, so ancestry alone does not qualify a page as a person. List/disambiguation entities are excluded. Citizenship, dates and occupations are diagnostic metadata, never ranking weights.

Generic Simplified-to-Traditional conversion was rejected: it can change a correct surname, such as 余 to 餘. Unchanged source titles are preserved. If OpenCC would change a title, an explicit Wikidata `zh-tw` or `zh-hant` label is required; unresolved variants are reported. No conversion exception list or manually chosen name is used.

Readings come from longest unambiguous CC-CEDICT/McBopomofo units. Where only tones disagree, an unambiguous Pinyin key can be retained without emitting guessed Zhuyin. Source-derived readings are not independent verification of a proper name’s pronunciation.

## Included topic outputs

| Sector | Distinct imported outputs |
|---|---:|
| animals | 732 |
| beverages | 108 |
| books | 228 |
| films | 779 |
| foods | 505 |
| history | 767 |
| indigenous | 917 |
| people | 4,666 |
| performers | 2,711 |
| plants | 392 |
| songs | 259 |

Sectors overlap. These counts describe the new encyclopedia import, not the earlier keyword tags or all contents of the base model.

## Historical coverage denominators

| Source stratum | Source pages | Verified humans | Included humans | Base/add-on string presence before → after |
|---|---:|---:|---:|---:|
| 台灣日治時期人物 | 1,569 | 1,521 | 1,164 | 56 → 1,171 |
| 台灣清治時期人物 | 444 | 423 | 317 | 13 → 321 |
| 臺灣明鄭時期人物 | 55 | 51 | 41 | 2 → 42 |
| 台灣荷西殖民時期人物 | 70 | 67 | 38 | 3 → 38 |
| 台灣原住民人物 | 840 | 760 | 543 | 57 → 577 |

String presence does not prove entity disambiguation, pronunciation or first-choice ranking. These source strata do not establish demographic or political balance outside the retrieved category graph.

## Exclusions remain visible

| Result | Source pages |
|---|---:|
| category_topic_is_not_person | 716 |
| disambiguation_or_list_entity | 52 |
| included | 10,342 |
| index_or_edition_title | 8 |
| unresolved_person_identity | 138 |
| unresolved_reading | 2,654 |
| unresolved_script_or_name_variant | 810 |
| unsupported_script_or_length | 508 |

The full source inventory, identity claims, name normalization status, reading segments and failures are retained in entity-coverage.json. No per-person article-link list is curated.

## Verification and limits

The compiler reproduces identical assets offline. verify_taiwan_entities.py checks source-label identity, human-type constraints, old alias retention and byte-identical Rudy/Japanese/POJ data. The source corpus separates inventory, reading resolution and runtime lookup; it does not score itself as independent language accuracy.

The frozen lookup comparison covers every included entity’s complete, initial, mixed and prefix input plus a hash-selected sample of every legacy pack/form. The separate frozen conversation corpus tests changes to acceptance and top-eight visibility. Results and Android costs are recorded in RELEASE-0.6.4.md. No frequency overrides or per-word promotions were added.

Wikipedia metadata is CC BY-SA 4.0; Wikidata identities/labels are CC0; reading sources and OpenCC retain their licences. These are attributable community datasets, not a claim of authoritative completeness. Source bias, unresolved names, Indigenous names with unsupported punctuation/scripts and deeper unvisited categories remain explicit limitations.
