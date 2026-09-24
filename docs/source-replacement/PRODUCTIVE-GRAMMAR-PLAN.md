# Source-defined pronoun contractions without new vocabulary

September 25, 2026, before collecting the pronoun inventory or evaluating this
method. Offline grammatical-metadata proposal, no runtime policy change. Three
finite source inventories omit at least one annotated form. Their omissions are
not permission to patch that spelling. English contractions can be productive;
test a general source-defined construction over **already attested vocabulary**.

Collect the complete Wiktionary English-pronoun category graph with the same
bounded, twice-stable API snapshot procedure as the contraction category. Reuse
the pinned spaCy exception table as a source of suffix attachment shapes: take
every two-component entry whose first ORTH is ASCII letters only, whose second
ORTH starts with an apostrophe followed only by ASCII letters, and whose ORTHs
concatenate exactly to its surface. Deduplicate those source-defined suffixes;
do not manually write a clitic or pronoun whitelist.

Proposed grammar evidence is the complete Wiktionary contraction inventory plus
each pronoun/suffix combination from those two sources. Intersect with the fixed
accepted base apostrophized lexicon or twice-observed English unigrams **before
export**. Thus no generated word, alias, frequency or candidate enters the
dictionary: the only proposed change is grammatical status of an existing entry.
Keep valid-bare-spelling bytes and every production asset unchanged during trials.
Do not use EWT/GUM labels, missing spellings or corpus frequencies to decide which
pronouns/suffixes to collect or combine. No fallback to old EWT flags.

Risks: lexical categories can include historical/dialectal pronouns; suffix
attachment alone does not establish agreement or intended language. Existing-word
intersection prevents novel forms, but still requires independent annotation and
engine-level evaluation. The Wiktionary category part can contain non-AUX lexical
contractions; keep those separate in reports rather than pretending all share
one grammar precision metric. No source admits an entry solely by matching labels.

First inspect acquisition/shape contracts and preserve source/derived hashes and
all exclusion counts. Then freeze a new document-level grammar holdout before
running this method: consider the pinned GUM development split only if local
source/evaluation records establish it has not already been used. Keep original
references local; if previously exposed, label it reused and seek another source.
Do not modify the method after seeing the holdout without retiring that holdout.

Compare old flags, category-only flags and this one proposal on the existing
grammar references and new holdout. Require no loss of AUX-bearing surface
coverage within source/genre, report non-AUX verbal/nonverbal ambiguity separately,
and retain every changed case. Surviving proposals need full source-derived
apostrophe/casing/raw recovery checks and fresh matched-receipt English plus
broad/chat Chinese comparisons. Preserve English-mode behavior and first-eight/
Space reference coverage within source/genre. Core/native/package/device checks
and source attribution follow only after semantic survival. Context-count
replacement remains separate; no current rights or release gate is cleared.
