# Taiwan encyclopedia source snapshot

Chinese Wikipedia contributors: category membership metadata, titles, page IDs and
category redirects, CC BY-SA 4.0. Wikidata structured entity identities/types,
occupations and dates: CC0 1.0. No article prose, photographs, lyrics or biographies
are packaged. See source.json for retrieval time, scope and content hashes.

Regenerate with tools/fetch_taiwan_entities.py, then
tools/fetch_taiwan_entity_types.py and tools/fetch_taiwan_traditional_labels.py.
Successful requests are cached locally; request
pacing and pagination apply. A finite category-graph depth is explicit, and every
unvisited frontier is recorded. Hard and template-based soft redirects are followed.
No individual name/ID allowlist or modern citizenship eligibility rule is used.
Countries and dates are diagnostic metadata; they are not ranking inputs.

Compile with tools/compile_addons.py, then tools/verify_taiwan_entities.py and
tools/write_addon_notices.py. The build is offline after source retrieval. The
compiler validates human identities for people/performer sectors, removes list and
disambiguation entities, and reports exclusions and unresolved pronunciation.

Readings use longest unambiguous CC-CEDICT/McBopomofo units. Unambiguous untoned
Pinyin may be retained when tones disagree; guessed Zhuyin is never emitted. This
does not establish independent correctness of a proper name's pronunciation.
OpenCC Python 0.1.7, Apache 2.0, detects titles that would change under script
conversion. Such names require explicit Wikidata zh-tw/zh-hant labels; generic
conversion can corrupt surnames and is not applied. Unresolved variants are
reported. Its complete source and licence are in ../opencc_python.

Source page IDs preserve automatic attribution without a per-name article-link
list. The complete normalized derivation and unresolved records are retained under
docs/taiwan-quality; runtime rows reference source IDs. Topic ancestry is not a
popularity score or a guarantee of encyclopedic completeness. Existing geography
comes from the separate ODbL Rudy source and is unchanged.

Sources: https://zh.wikipedia.org/ ; https://www.wikidata.org/wiki/Wikidata:Licensing
Licence: https://creativecommons.org/licenses/by-sa/4.0/
