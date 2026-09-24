# MASC source audit plan

September 24, 2026. Data-source investigation only; no production changes and no
model trained. Taskmaster alone lost reference coverage on unrelated conversation
and essay data; PersonaChat lacks apostrophes in the inspected distribution.
Seek an independently licensed source with both genres and retained orthography.

The publisher describes MASC as approximately 500,000 words across written and
spoken American English, available under CC BY 3.0 US:
https://anc.org/data/masc/ . Its research description explicitly includes commercial
reuse of the text, not only annotation reuse:
https://www.cs.vassar.edu/~ide/papers/masc-collab-wordsense.pdf . Direct website
requests timed out here; the indexed primary page is discovery evidence, not a
substitute for the downloaded distribution's notice.

Audit the official NLTK data mirror at pinned gh-pages revision
`550b6625bcef1f2abff2ff770a5a0d272c9c6b2a`, package `masc_tagged.zip`.
Expected package SHA-256 from its published index:
`678a5141cf3381bedb1839c58a330507337be07c7c71603279c0ef5337032304`.
Inspect notices, provenance/genre boundaries, tokenization and apostrophe retention
before selecting any model experiment. Do not execute corpus code or blindly
extract archive paths. Download into the ignored source-audit cache and retain
aggregate evidence and exact pins in source management.

No current evaluation documents may become training data. Before a later pilot,
declare document/conversation-level splits, deduplication and overlap exclusions
against existing GUM and other evaluation corpora. Keep reference-hit reporting
separate by genre; it does not label all non-reference suggestions as nonsense.
This discovery does not clear the existing EWT rights decision, establish current
phone-message style, or demonstrate improved coverage. A licensed source can still
be unsuitable for the product.

## Structural result

The pinned tagged archive matches the published SHA-256. It contains 392 text
documents and 592,469 parseable tagged tokens across 20 labelled genres, plus
three files missing from its category map. All files were audited; no names or
words were handpicked. There are 9,573 tokens containing an apostrophe, unlike
the inspected PersonaChat export, but Penn-style tokenization separates many
contraction pieces. Joining tagged tokens with spaces would corrupt orthography.
There are also 67 whitespace-separated units without a tag separator.

Retrieved the publisher's data-only archive identified by the download page:
`https://www.anc.org/MASC/download/masc_500k_texts.zip`, SHA-256
`0c3f1fd3314ee5ec09830d6f6b217c19a6899d1cabd22c42264578c3348b2d01`.
The publisher's certificate is expired. The public page/archive were fetched
with certificate validation bypassed for those individual audit requests only;
they are quarantined evidence, not an authenticated production source refresh.
The independently pinned NLTK archive and package notice used normal verified TLS.

386 tagged documents have a same-basename raw counterpart; 344 of those pairs
have exactly the same character sequence after removing whitespace and tags.
The other 42 pairs require systematic version/annotation analysis, not automatic
repair. Six tagged files have no raw counterpart. Neither archive includes a
license/README member. The pinned distributor package notice explicitly permits
linguistic development including commercial development; the publisher's indexed
primary page separately states CC BY 3.0 US. Keep those distinct evidence scopes.

Decision: **hold for format/version and source-provenance reconciliation**, not a
rejected language model. No model trained or evaluated, no split declared yet,
and no production pins or vocabulary changed. A useful next audit must obtain
an authenticated original-text distribution/notice, reconcile all file/genre
differences by general rules, and freeze whole-document splits and overlap
exclusions before using this as a context or contraction source. Spoken face-to-
face/telephone data, essays, fictional scripts and spam must stay separate in
evaluation; their union is not a representative phone-conversation distribution.

Reproduce structural results with `masc-audit.py`; `masc-manifest.json` retains
every file's identity, hash, genre and comparison result without republishing text.
