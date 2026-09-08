# Source decisions

Generated from sources/catalog.json. Retain is scoped to the listed use; pilot is not production approval. Replace preserves the existing locked release while prohibiting silent refresh or expansion. Counts do not establish taste, frequency or coverage outside the source inventory.

| Source | Decision | Intended use | Reason |
|---|---|---|---|
| McBopomofo | retain | vocabulary, readings, frequency | Retain the Taiwan input-method foundation. |
| Rudy / OSM Taiwan POI | retain | vocabulary, readings | Preserve the high-quality geography source and separate ODbL pack. |
| AOSP LatinIME English | retain | vocabulary, frequency | Retain English-specific source. |
| ChhoeTaigi iTaigi | retain | vocabulary, readings | Retain original POJ and source attribution. |
| Taiwanese beginner vocabulary | retain | vocabulary, readings | Retain POJ; do not infer modern popularity. |
| JMDICT | retain | vocabulary, readings | Retain Japanese-specific sources. |
| JMNEDICT | retain | vocabulary, readings | Retain Japanese-specific sources. |
| WanaKana | retain | tooling | Retain language tooling. |
| CC-CEDICT legacy Taiwan filter | replace | vocabulary, readings | Freeze existing contribution pending Taiwan-authored replacement. |
| Wikidata legacy cultural labels | replace | vocabulary, discovery | Freeze existing contribution; use specialist authored catalogs for wording. |
| Wikipedia category-derived Taiwan pack | replace | vocabulary, discovery | Freeze current data; stop treating volume and locale tags as quality. |
| Legacy build-time OpenCC detector | replace | tooling | Freeze until replacing the mixed-source import path. |
| Rime Luna / Essay / Prelude | replace | vocabulary, readings, frequency | Retain frozen native behavior during source evaluation; replacement requires broad ranking evidence. |
| UD context training | replace | frequency, evaluation | Freeze model; keep train/dev/test roles distinct. |
| TaiCOL Taiwan species | pilot | vocabulary | First candidate for nature-sector replacement. |
| 如月的現代台灣華語補足典 | pilot | vocabulary, readings | Good local usage supplement; audit parser and full/partial lookup before import. |
| Taiwan.md | pilot | discovery, evaluation | Pilot source discovery and essay evaluation; reviewed prose has a separate role from conversational tests or production vocabulary. |
| CIP Indigenous community names | pilot | vocabulary | Strong identity/romanization candidate; preserve community names and scripts. |
| BAMID music awards catalogs | pilot | vocabulary | Candidate for musicians/works; awards do not become frequency weights. |
| Taiwan Biographical Database | hold | discovery | Promising historical identity source; obtain a reusable snapshot and terms first. |
| National Museum of Taiwan Literature databases | hold | discovery | Prefer specialist names/works over broad category ancestry; establish dataset access first. |
| National Cultural Memory Bank / Taiwan encyclopedia | hold | discovery | Evaluate compatible datasets individually, never scrape the whole portal into phrases. |
| MOE / Moedict dictionary data | reference | evaluation | Use as a reference; establish permissible derivative use before adding an importer. |
| UD English GUM evaluation | reference | evaluation | Reuse existing genre-separated regression evidence. |
