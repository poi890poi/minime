"""Generate coverage evidence with honest source and pronunciation denominators."""
from pathlib import Path
from collections import Counter
import gzip,json
r=Path(__file__).resolve().parent.parent;o=r/'docs/taiwan-quality'
c=json.loads((o/'coverage-summary.json').read_text(encoding='utf8'));e=json.loads((o/'entity-coverage.json').read_text(encoding='utf8'));s=json.loads(gzip.decompress((r/'third_party/taiwan_encyclopedia/snapshot.json.gz').read_bytes()).decode())
lines=['# Taiwan dictionary source quality','',
 'Baseline: 0.6.3 / 3f25366. The Rudy geography snapshot, extractor and runtime asset are unchanged byte for byte. Japanese and POJ rows are unchanged. Existing Taiwan usage aliases are retained, with misleading keyword-derived person/history labels replaced by `taiwan_usage`.',
 '',f'Taiwan outputs: **{c["before_taiwan_outputs"]:,} → {c["after_taiwan_outputs"]:,}** ({c["new_taiwan_outputs"]:,} new distinct outputs). These are vocabulary counts, not language accuracy or popularity.',
 '', '## Source and identity controls','',
 f'The Chinese Wikipedia snapshot contains {len(s["entities"]):,} distinct topic-linked pages from {len(s["nodes"]):,} categories. Every visited category is fully paginated; hard and soft redirects are resolved; retrieval completed with no errors. Topic roots cover multiple historical periods, indigenous topics, arts, science, nature, food and cultural works. There is no modern-citizenship filter.',
 '', 'The graph depth is two descendant levels. Unvisited frontiers are retained in the source manifest; this is not the complete encyclopedia. The previous Wikipedia evaluation inventories now overlap the imported source and cannot be presented as unseen accuracy holdouts.',
 '', 'Wikidata sitelinks and structured types independently validate people/performer identity. An artist’s category can contain albums, so ancestry alone does not qualify a page as a person. List/disambiguation entities are excluded. Citizenship, dates and occupations are diagnostic metadata, never ranking weights.',
 '', 'Generic Simplified-to-Traditional conversion was rejected: it can change a correct surname, such as 余 to 餘. Unchanged source titles are preserved. If OpenCC would change a title, an explicit Wikidata `zh-tw` or `zh-hant` label is required; unresolved variants are reported. No conversion exception list or manually chosen name is used.',
 '', 'Readings come from longest unambiguous CC-CEDICT/McBopomofo units. Where only tones disagree, an unambiguous Pinyin key can be retained without emitting guessed Zhuyin. Source-derived readings are not independent verification of a proper name’s pronunciation.',
 '', '## Included topic outputs','', '| Sector | Distinct imported outputs |','|---|---:|']
for sector,count in sorted(e['unique_outputs_by_sector'].items()):lines.append(f'| {sector} | {count:,} |')
lines+=['','Sectors overlap. These counts describe the new encyclopedia import, not the earlier keyword tags or all contents of the base model.','', '## Historical coverage denominators','', '| Source stratum | Source pages | Verified humans | Included humans | Base/add-on string presence before → after |','|---|---:|---:|---:|---:|']
for root in ('Category:台灣日治時期人物','Category:台灣清治時期人物','Category:臺灣明鄭時期人物','Category:台灣荷西殖民時期人物','Category:台灣原住民人物'):
    members=[x for x in e['records'] if root in x['roots']];humans=[x for x in members if any(v.get('id')=='Q5' for v in (x.get('wikidata') or {}).get('claims',{}).get('P31',[]))];a=c['roots'][root]
    lines.append(f'| {root.split(":",1)[1]} | {len(members):,} | {len(humans):,} | {sum(x["status"]=="included" for x in humans):,} | {a["old_base_or_addon_string_present"]:,} → {a["new_base_or_addon_string_present"]:,} |')
lines+=['','String presence does not prove entity disambiguation, pronunciation or first-choice ranking. These source strata do not establish demographic or political balance outside the retrieved category graph.','', '## Exclusions remain visible','', '| Result | Source pages |','|---|---:|']
for status,count in sorted(Counter(x['status'] for x in e['records']).items()):lines.append(f'| {status} | {count:,} |')
lines+=['','The full source inventory, identity claims, name normalization status, reading segments and failures are retained in entity-coverage.json. No per-person article-link list is curated.','', '## Verification and limits','',
 'The compiler reproduces identical assets offline. verify_taiwan_entities.py checks source-label identity, human-type constraints, old alias retention and byte-identical Rudy/Japanese/POJ data. The source corpus separates inventory, reading resolution and runtime lookup; it does not score itself as independent language accuracy.',
 '', 'The frozen lookup comparison covers every included entity’s complete, initial, mixed and prefix input plus a hash-selected sample of every legacy pack/form. The separate frozen conversation corpus tests changes to acceptance and top-eight visibility. Results and Android costs are recorded in RELEASE-0.6.4.md. No frequency overrides or per-word promotions were added.',
 '', 'Wikipedia metadata is CC BY-SA 4.0; Wikidata identities/labels are CC0; reading sources and OpenCC retain their licences. These are attributable community datasets, not a claim of authoritative completeness. Source bias, unresolved names, Indigenous names with unsupported punctuation/scripts and deeper unvisited categories remain explicit limitations.']
(o/'SOURCES.md').write_bytes(('\n'.join(lines)+'\n').encode('utf8'))
