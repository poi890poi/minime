"""Generate packaged source summaries from the compiled dataset manifests."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parent.parent
assets=ROOT/'app/src/main/assets';docs=ROOT/'docs/addons-learning'
language=json.loads((docs/'source-manifest.json').read_text(encoding='utf-8'))
geography=json.loads((docs/'rudy-manifest.json').read_text(encoding='utf-8'))
counts=language['outputs_by_pack']
text=f'''MinIME optional dictionaries · dataset provenance

Every production entry comes from a reproducible source filter. No handpicked
name/phrase list or individual ranking override is used. Source versions, hashes,
rules, counts, licences and missing readings are recorded in the repository:
https://github.com/poi890poi/minime/tree/codex/close-ime-gaps/docs/addons-learning

Geography · {geography['included_names']:,} names, separately optional
Rudy Map / OpenStreetMap POIv3, {geography['database_metadata']['comment']}.
© OpenStreetMap contributors; packaged by Rudy Map. ODbL 1.0.
https://rudymap.tw/
https://www.openstreetmap.org/copyright
https://opendatacommons.org/licenses/odbl/1-0/
The complete category-based import covers hiking, nature, settlements, waterways
and historic sites. {geography['selected_records']:,} records were considered;
{geography['eligible_Han_names']:,} distinct Han names passed script/length rules.
{geography['unavailable_readings']:,} names still lack usable readings. No article
links are maintained per name. Dataset-level snapshots and rules make the import
reproducible. Pronunciation tags take precedence; fallback readings use exact or
unambiguous McBopomofo units (MIT). These are not independently verified local
pronunciations. The complete derived database is provided as geography.tsv under
ODbL, separately from the other language packs. No map styles or contours are used.

Taiwan language/culture · {counts['taiwan']:,} indexed outputs
CC-CEDICT contributors via MDBG, CC BY-SA 4.0.
https://www.mdbg.net/chinese/dictionary?page=cedict
https://creativecommons.org/licenses/by-sa/4.0/
Selection uses Taiwan/Taiwanese/Formosa/(Tw) source metadata. Explicit Taiwan
pronunciation is preferred when supplied. Categories are heuristic tags, not an
expert taxonomy or encyclopedic completeness claim. cedict:line:N is an automatic
record identifier in the pinned archive.
Wikidata (CC0) supplies film/book/song title identities from reproducible queries.
https://www.wikidata.org/wiki/Wikidata:Licensing
Title readings marked +cedict:derived use unambiguous dictionary units. Ambiguous
readings are omitted for review. These bounded snapshots are not popularity lists.
No definitions, lyrics, book text, article prose, routes or photographs are bundled.

Japanese · {counts['japanese']:,} indexed outputs, including Romanized raw spelling
JMnedict, Electronic Dictionary Research and Development Group, started by Jim
Breen; used in conformance with the Group's licence, CC BY-SA 4.0.
https://www.edrdg.org/edrdg/licence.html
JSON source: scriptin/jmdict-simplified, pinned release in source-manifest.json.
https://github.com/scriptin/jmdict-simplified
All source-tagged works, creative professions and Taiwan-related translations
are eligible. No entity allowlist. No measured Taiwan-popularity claim is made.
WanaKana {language['romanizer']['version']} (MIT) supplies build-time Romanized
aliases. Unsupported aliases are reported. This is not a Japanese sentence IME.
https://github.com/WaniKani/WanaKana

Taiwanese POJ · {counts['poj']:,} short expressions
iTaigi contributors via ChhoeTaigi/ChhoeTaigiDatabase, CC0.
https://itaigi.tw/
https://github.com/ChhoeTaigi/ChhoeTaigiDatabase
Original PojUnicode/PojInput fields preserve the entire Pe̍h-ōe-jī system, including
tones, ch/chh, oe/oa, o͘ and ⁿ. There is no Tâi-lô ts-to-ch conversion. Source keys
encode o͘ as oo and ⁿ as nn. Letter-board aliases omit tones and separators.
All expressions with 2–6 Han Mandarin labels, source frequency at least 1,000,
and at most four POJ syllables are selected, retaining all eligible variants.
Frequency comes from McBopomofo's MIT-licensed written Mandarin corpus; it is a
reproducible common-vocabulary proxy, not spoken Taiwanese usage measurement.

CC-CEDICT/JMnedict-derived language data remains CC BY-SA 4.0; CC0 data retains
its public-domain dedication. Geography data remains ODbL. Code and datasets are
separately licensed. Changes are filtering, normalization, aliases, dictionary-unit
reading derivation and packaging. Personal learning remains separate and local.
'''
(assets/'addon-sources.txt').write_bytes(text.encode('utf-8'))
notice=assets/'NOTICE.txt';base=notice.read_text(encoding='utf-8').split('\n--- MinIME optional source packs ---')[0]
notice.write_bytes((base+'\n--- MinIME optional source packs ---\n'+text).encode('utf-8'))
