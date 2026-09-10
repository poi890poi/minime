"""Generate packaged source summaries from the compiled dataset manifests."""
from pathlib import Path
import json, argparse
from collections import defaultdict
ROOT=Path(__file__).resolve().parent.parent
assets=ROOT/'app/src/main/assets';docs=ROOT/'docs/addons-learning'
language=json.loads((docs/'source-manifest.json').read_text(encoding='utf-8'))
geography=json.loads((docs/'rudy-manifest.json').read_text(encoding='utf-8'))
outputs=defaultdict(set)
for row in (assets/'addons.tsv').read_text(encoding='utf-8').splitlines():
    if row and not row.startswith('#'):
        pack,reading,output,source,category=row.split('\t');outputs[pack].add(output)
counts={pack:len(values) for pack,values in outputs.items()}
pair_count=sum(1 for row in (assets/'paired-forms.tsv').read_text(encoding='utf-8').splitlines() if row and not row.startswith('#'))
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
pronunciation is preferred when supplied. These entries are usage vocabulary,
not an entity taxonomy. cedict:line:N identifies a record in the pinned archive.
Chinese Wikipedia contributors, CC BY-SA 4.0, supply a separate paginated category
graph for historical people, arts, science, events, indigenous topics, nature,
food, beverages and cultural works. Source page IDs and snapshot hashes are retained.
https://zh.wikipedia.org/
https://github.com/poi890poi/minime/tree/codex/close-ime-gaps/docs/taiwan-quality
The graph has explicit depth/frontier limits, follows hard and soft category
redirects, and does not filter historical people by modern citizenship. Topic
membership is not a popularity or identity-verification score. Readings derive
from unambiguous CC-CEDICT/McBopomofo units; unresolved names are reported.
OpenCC Python 0.1.7 (Apache 2.0) detects titles requiring script review. Changed
titles require explicit Wikidata zh-tw/zh-hant labels; conversion is not guessed:
https://github.com/yichen0831/opencc-python
No proper-name vocabulary substitutions, individual name lists or scores are used.
Wikidata (CC0) supplies film/book/song title identities from reproducible queries.
https://www.wikidata.org/wiki/Wikidata:Licensing
Title readings marked +cedict:derived use unambiguous dictionary units. Ambiguous
readings are omitted for review. These bounded snapshots are not popularity lists.
No definitions, lyrics, article prose, routes or photographs are bundled.

Japanese · {counts['japanese']:,} indexed kana/Kanji outputs; Romanization is input only
JMdict and JMnedict, Electronic Dictionary Research and Development Group, started by Jim
Breen; used in conformance with the Group's licence, CC BY-SA 4.0.
https://www.edrdg.org/edrdg/licence.html
JSON source: scriptin/jmdict-simplified, pinned release in source-manifest.json.
https://github.com/scriptin/jmdict-simplified
Common vocabulary across parts of speech uses JMdict common readings and
compatible common spellings. JMnedict supplies works, creative professions and Taiwan-related
names. No entity allowlist or geographic restriction on everyday vocabulary.
WanaKana {language['romanizer']['version']} (MIT) supplies build-time Romanized
aliases. Unsupported aliases are reported. This is not a Japanese sentence IME.
https://github.com/WaniKani/WanaKana

Taiwanese POJ · {counts['poj']:,} distinct source spellings; {pair_count:,} Han examples
iTaigi contributors via ChhoeTaigi/ChhoeTaigiDatabase, CC0.
https://itaigi.tw/
https://github.com/ChhoeTaigi/ChhoeTaigiDatabase
Original PojUnicode/PojInput fields preserve the entire Pe̍h-ōe-jī system, including
tones, ch/chh, oe/oa, o͘ and ⁿ. There is no Tâi-lô ts-to-ch conversion. Source keys
encode o͘ as oo and ⁿ as nn. Letter-board aliases omit tones and separators.
Supported iTaigi and Taihoa headwords and aligned variants are imported without
Mandarin-gloss length or syllable-count gates, within the shared 96-character limit.
Plain input aliases are derived from source spelling; no tone variants are invented.
Beginner vocabulary and complete short examples also come from the 1956
A Basic Vocabulary for a Beginner in Taiwanese, CC BY-SA 4.0, via ChhoeTaigi.
Authors: Ko Chek-hoàn (高積煥), Tân Pang-tìn (陳邦鎮). Digitization/editing:
Lîm Bûn-cheng, Tēⁿ Tì-têng, Tân Kim-hoa, Chiúⁿ Ji̍t-êng.
All compatible headwords, aligned variants and complete examples up to six
syllables are eligible. Terminal example punctuation is omitted. This historical
source contains dated vocabulary; inclusion does not establish modern popularity.

Taihoa / 台華線頂對照典, via ChhoeTaigi at pinned revision
b33c6a1fcc2d11a2962e76b6055d528d11677c3b, CC BY-SA 4.0.
Foundation: Tēⁿ Liông-úi (鄭良偉). Additions and editing: Iûⁿ Ún-giân (楊允言)
and transcription/proofreading volunteers. Original POJ readings and supported
source variants supply vocabulary; source Han forms supply paired alternatives.
https://github.com/ChhoeTaigi/ChhoeTaigiDatabase

Single kana and common kanji: WanaKana supplies both kana scripts and input aliases.
KANJIDIC2, James William Breen / EDRDG, CC BY-SA 4.0, supplies frequency-ranked
kanji and Japanese on/kun readings. Source ranks 1–500 determine eligibility;
newspaper frequency does not establish conversational frequency.
https://www.edrdg.org/edrdg/licence.html
https://www.edrdg.org/kanjidic/kanjidic2_ov_legacy.html

Wikipedia/CC-CEDICT/JMdict/JMnedict/KANJIDIC2/Taihoa/beginner-derived data remains CC BY-SA 4.0; CC0 data retains
its public-domain dedication. Geography data remains ODbL. Code and datasets are
separately licensed. Changes are filtering, normalization, aliases, dictionary-unit
reading derivation and packaging. Personal learning remains separate and local.
'''
check=argparse.ArgumentParser();check.add_argument('--check',action='store_true');args=check.parse_args()
target=assets/'addon-sources.txt'
if args.check:
    assert target.read_bytes()==text.encode('utf-8'), 'Stale add-on source notices'
else:target.write_bytes(text.encode('utf-8'))
notice=assets/'NOTICE.txt';base=notice.read_text(encoding='utf-8').split('\n--- MinIME optional source packs ---')[0]
base=base.split('\n--- Third-party notices ---\n')[-1]
base=(assets.parents[3]/'NOTICE').read_text(encoding='utf-8').rstrip()+'\n\n--- Third-party notices ---\n'+base
content=(base+'\n--- MinIME optional source packs ---\n'+text).encode('utf-8')
if args.check:
    assert notice.read_bytes()==content, 'Stale combined notices'
else:notice.write_bytes(content)
