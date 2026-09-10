# POJ source-reading recovery

Type: bug fix at the offline import boundary. At 5bcb90f the importer rejects
iTaigi fields containing slash-separated aligned readings and rejects input fields
containing Unicode tone marks. Thus attested words disappear before lookup; a
ranking change cannot recover them. The pinned CSV files establish the defect.

Apply one shared variant reader to iTaigi, Taihoa and beginner vocabulary. Split
aligned input/output lists, preserve original numeric aliases, and derive a tone-free
alias from supported original Unicode spelling. Preserve output tone marks and CH
orthography. Do not synthesize other tones, infer pronunciations, split phrases into
guessed words, or select/promote entries by complaint examples. Existing source pins,
licenses, native output and candidate ranking remain unchanged.

Coverage evidence is exhaustive source inventory/retrieval, not conversational
accuracy or proof of commonness. Source membership does not establish frequency.
Report unresolved unsupported/unaligned records, exact query ambiguity, first-eight
retrieval, and other-language row differences. Freeze baseline assets before refresh.
Source audit inputs have already been inspected; they are not a fresh holdout.

Initial dry run: 4,336 additional rows, zero removed existing headword rows, 1,555
new distinct POJ spellings across the three pinned sources. Confirm the final asset
diff including examples and paired Han output separately before landing.
