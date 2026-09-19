# Glyph and phrase retrieval expansion — 2026-09-19

Baseline: 311b1cb. Type: evaluation expansion, followed only by evidenced fixes.
No word exceptions, source-weight tuning, or generated Han sequences. Preserve
exact spellings, explicit first-glyph acceptance/suffixes, private input, English,
Taiwanese and Japanese behavior. Use existing attributed Taiwan production data.

Before seeing outputs, freeze all stored reading/text pairs at complete spelling,
all single-glyph proper prefixes, and hash-selected phrase families with initials,
mixed full/initial units, multi-letter partial units, and explicit separators.
Split by text identity into development and reserved retrieval families so
alternate readings cannot cross the split. This is seen-source retrieval, not
an independent language-accuracy holdout. No expected output enters decoding.

Use an independent exhaustive source-unit matcher on a hash-selected development
subset to distinguish search truncation from out-of-dictionary phrases and normal
homophone ambiguity. Record target rank, first 8 availability, missing targets,
whole versus consumed-prefix identities and uncached lookup time. Rank is over
decoder results, not phone cells. Keep full and partial conditions separate.

Re-run the existing 31,527-episode Chinese recovery corpus with separate essay,
conversation, encyclopedic, mechanical and source-retrieval results. Existing
conversation data is small/reused and cannot support population accuracy claims.
Use frozen Google/MinIME phone cases for visible candidates and first-glyph
selection only after core and pinned desktop tests. An unavailable action is a
failure, even if the instrumentation process completes.

Accept only general fixes that recover independently matched source entries,
preserve previous whole-target availability, retain valid consumption spans, and
do not regress the shared language contracts. Measure latency separately; no
dictionary scan or unbounded search is acceptable on the per-keystroke path.
Retain negative results. Keep test and runtime changes independently reviewable.
