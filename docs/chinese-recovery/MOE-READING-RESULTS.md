# Independent Taiwan pronunciation reference

The official Ministry of Education concise and elementary dictionaries provide
independent pronunciation annotations. They are reference data only. Neither is
an adult conversation-frequency model, and reference absence does not establish
that an alternative is impossible or meaningless.

The [official concise download](https://language.moe.gov.tw/001/Upload/Files/site_content/M0001/respub/dict_concised_download.html)
and [elementary download](https://language.moe.gov.tw/001/Upload/Files/site_content/M0001/respub/dict_mini_download.html)
link the June 26, 2026 archives. Original archives, spreadsheets and full usage
notices are preserved locally. Attribution: 中華民國教育部. The declared licence
is CC BY-ND 3.0 Taiwan; this audit does not approve derivative production data.
No definitions, edited reference entries or per-query joins are redistributed.
`modern-reading/source-summary.json` records original archive/XLSX hashes.

| Reference inventory | Concise | Elementary |
|---|---:|---:|
| Source rows | 45,130 | 4,719 |
| Single-Han rows | 6,657 | 4,718 |
| Distinct single-Han glyphs | 6,028 | 4,310 |
| Glyphs with multiple tone-bearing readings | 554 | 365 |
| Unparsed single-Han Bopomofo rows | 0 | 0 |

The elementary table has one non-single-Han title; it is counted outside the
single-glyph scope. All alternate readings are preserved. Neutral tone moves to
the same canonical position, omitted first tone is made explicit, and Pinyin
tone accents are removed while preserving ü as v. No synonym or glyph conversion
is performed. Four structural tests cover normalization, invalid inputs, Han
code points and metric partitions.

Of 528 primary-priority source pairs, concise supports 490, omits 33 readings on
covered glyphs, and does not cover five glyphs. Elementary supports 466, omits 27
on covered glyphs and does not cover 35 glyphs. These are source discrepancies,
not a reason to insert per-glyph overrides. All tiers are audited in the receipt.

The complete-reading probe freezes all supplied concise single-glyph Pinyin:
405 spellings and 6,402 glyph/toneless-reading pairs. 6,257 pairs are reachable
in both production and the corrected source-priority trial. This source was
inspected before evaluation and overlaps earlier queries; it is not a fresh
holdout. Labels are joined only after lookup.

| Complete-reading interpretation | Production | Corrected source priorities |
|---|---:|---:|
| Space output supported by the reference, of 405 | 305 | 314 |
| Supported whole-input glyph slots among first eight | 1,531 | 1,530 |
| Covered glyph slots without that complete reference reading | 265 | 235 |

The last row includes legitimate spelling completions: whole-input consumption
does not mean a fully typed pronunciation. Likewise, phrases, literals, prefix
selections and uncovered glyphs are separate categories. This metric is not
semantic error rate. Corrected source priorities remain outside production:
the support gain accompanies 12 Space gains, three losses and almost unchanged
first-eight supported coverage. The [phrase-evidence experiment](PHRASE-READING-PLAN.md)
tests a separate general model; it does not tune these source lists.

The older 2,000 full-word reference inputs contain 1,606 supplied spellings,
nine absent spellings on covered words, and 385 uncovered words. Their highest-
frequency tie rule cannot independently resolve pronunciation. Old labels and
scores remain intact; the new reference does not silently rewrite them.
