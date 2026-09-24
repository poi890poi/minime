# Independent annotation screen for grammar flags

September 25, 2026, before extracting these references. Use every internal-
apostrophe multiword surface in the already pinned EWT dev/test and GUM test
annotations. No training text, candidate output or old/new flag membership may
select a reference. Verify decompressed source hashes against existing manifests;
keep document/sentence identity and genre. These are reused evaluation sources,
not fresh holdouts. GUM is independent of EWT training; EWT dev/test share lineage
with the baseline grammar annotation policy and are reported separately.

Normalize case and curly apostrophes only. Require the full ASCII-letter/internal-
apostrophe shape and all expanded token IDs. Count AUX-bearing, VERB-bearing
without AUX, and nonverbal expansions separately. AUX annotations provide a
narrow independent grammar reference, not proof of safe automatic restoration in
Chinese mode. Non-AUX verbal forms are not false positives by definition; a verb
plus pronoun can also be a contraction. Possessive/other ambiguous surfaces stay
in their separate category. Do not flatten this into one precision percentage.

For each source/genre/category, report occurrence and unique-surface counts plus
old/new flag inclusion, gains and losses. Keep spellings and source sentences
local; publish aggregates and hashes only. A broad word-probe pass cannot conceal
lost independent grammatical coverage. Do not add missing spellings by hand or
relax extraction rules based on a named failure. This is a source-coverage screen,
not complete engine acceptance, frequency, user-intent or language accuracy.
