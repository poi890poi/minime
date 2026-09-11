# Source decisions

Generated from sources/catalog.json. Retain is scoped to the listed use; pilot is not production approval. Replace preserves the existing locked release while prohibiting silent refresh or expansion. Counts do not establish taste, frequency or coverage outside the source inventory.

| Source | Decision | Intended use | Reason |
|---|---|---|---|
| McBopomofo | retain | vocabulary, readings, frequency, evaluation | Retain the Taiwan input-method foundation. |
| Rudy / OSM Taiwan POI | retain | vocabulary, readings, evaluation | Preserve the high-quality geography source and separate ODbL pack. |
| AOSP LatinIME English | retain | vocabulary, frequency | Retain English-specific source. |
| ChhoeTaigi iTaigi | retain | vocabulary, readings, evaluation | Retain original POJ and source attribution. Recover all aligned slash-separated POJ readings and supported Unicode-input spellings through a shared parser; preserve source tones and numeric aliases. |
| Taiwanese beginner vocabulary | retain | vocabulary, readings, evaluation | Retain POJ; do not infer modern popularity. Recover all aligned slash-separated POJ readings and supported Unicode-input spellings through a shared parser; preserve source tones and numeric aliases. |
| JMDICT | retain | vocabulary, readings, evaluation | All source-common vocabulary without a word or part-of-speech allowlist; preserve reading/sense restrictions. Output source kana and compatible kanji only, retaining romanization as input aliases rather than generated output (docs/japanese-output/RESULTS.md). |
| JMNEDICT | retain | vocabulary, readings, evaluation | Retain Japanese-specific sources and their existing eligibility rules. Output source kana and compatible spellings, not generated romanization; preserve romanized input aliases (docs/japanese-output/RESULTS.md). |
| WanaKana | retain | tooling, readings, evaluation | Retain language tooling. |
| CC-CEDICT legacy Taiwan filter | replace | vocabulary, readings, evaluation | Freeze existing contribution pending Taiwan-authored replacement. |
| Wikidata legacy cultural labels | replace | vocabulary, discovery, evaluation | Freeze existing contribution; use specialist authored catalogs for wording. |
| Wikipedia category-derived Taiwan pack | replace | vocabulary, discovery, evaluation | Freeze current data; stop treating volume and locale tags as quality. |
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
| Taiwanese source-attested paired output | retain | vocabulary, readings | Optional direct alternative output for existing Taiwanese candidates; does not change ranking or primary phonetic output. |
| 台華線頂對照典 / Taihoa | retain | readings, vocabulary, evaluation | Dedicated Taiwanese mode needs an extensive authored dictionary; import original headwords and aligned variants without Mandarin gloss, length-in-syllables or frequency eligibility gates. Recover all aligned slash-separated POJ readings and supported Unicode-input spellings through a shared parser; preserve source tones and numeric aliases. |
| KANJIDIC2 | retain | vocabulary, readings, frequency | User requested very common kanji in Japanese mode. Fixed top-500 source-rank cutoff before evaluation; no per-character selections. |
| RealPersonaChat | reference | evaluation, discovery | Evaluation only; source/reading independence and rights audited in docs/language-contract-benchmark/CONVERSATION_DATA.md. |
| Accommodation Search Dialog Corpus | reference | evaluation, discovery | Evaluation only; source/reading independence and rights audited in docs/language-contract-benchmark/CONVERSATION_DATA.md. |
| 媠聲一千：阮家己的造句 | reference | evaluation, discovery | Evaluation only; source/reading independence and rights audited in docs/language-contract-benchmark/CONVERSATION_DATA.md. |
| Tsay / TAICORP spontaneous Taiwanese conversations | hold | evaluation, discovery | Evaluation only; source/reading independence and rights audited in docs/language-contract-benchmark/CONVERSATION_DATA.md. |
| Kazuma Naka kana-kanji-conversion-c-plus-plus | reference | evaluation, tooling | Isolated engine and completion evaluation; SOURCES.md records scope and no production promotion. |
| Google Mozc OSS dictionary and costs | reference | evaluation, tooling | Isolated engine and completion evaluation; SOURCES.md records scope and no production promotion. |
| AJIMEE-Bench Japanese conversion evaluation | reference | evaluation, tooling | Isolated engine and completion evaluation; SOURCES.md records scope and no production promotion. |
