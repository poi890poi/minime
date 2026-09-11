# Conversation benchmark: stronger references, visible phrase gaps

Baseline: 5dd9bb8 (app code d353be4 / 0.8.2). This is an evaluation change. No vocabulary, ranking weight, production code or Android package was changed.

The corpus adds **61,941 controlled input conditions from 7,775 source references**. Japanese uses 192 human conversations: 96 casual chats and 96 accommodation-service role-plays. We retain all 4,848 turns from those conversations. Taiwanese adds 1,063 modern authored aligned examples from 18 files, plus 1,024 historical authored examples; these are not spontaneous multi-turn conversations. Sampling words and clauses yields more references than original examples.

Sources, licenses, pinned revisions, reading conversion and exclusions are in [CONVERSATION_DATA.md](../CONVERSATION_DATA.md), [manifest.json](manifest.json) and [NOTICE.md](NOTICE.md). No entries were handpicked. Inputs and references regenerated identically from the pinned sources; gzip content was compared after decompression.

## Complete input: initial holdout and historical regression

Exact target matching after NFC and case folding. Tone marks, spacing, hyphens, kana/kanji and inflection remain significant. Top-three/eight count ordinal suggestion slots excluding raw input; they are not measured screen rows/pages. Only complete-consumption matches count. Japanese reading annotations for kanji are silver, and grammar or alternate valid wording is not judged.

| Source / evaluation role | Unit | References | Default | Top 3 | Top 8 |
|---|---|---:|---:|---:|---:|
| Japanese casual chat / holdout | word | 768 | 70.1% (538/768) | 91.5% (703/768) | 94.1% (723/768) |
| Japanese casual chat / holdout | two-token | 256 | 52.0% (133/256) | 56.6% (145/256) | 59.8% (153/256) |
| Japanese casual chat / holdout | clause | 256 | 25.4% (65/256) | 30.5% (78/256) | 30.5% (78/256) |
| Japanese service dialogue / holdout | word | 768 | 66.8% (513/768) | 90.4% (694/768) | 91.3% (701/768) |
| Japanese service dialogue / holdout | two-token | 256 | 34.8% (89/256) | 40.2% (103/256) | 41.8% (107/256) |
| Japanese service dialogue / holdout | clause | 256 | 27.3% (70/256) | 36.3% (93/256) | 36.7% (94/256) |
| Modern Taiwanese situations / holdout | word | 720 | 45.0% (324/720) | 80.8% (582/720) | 87.6% (631/720) |
| Modern Taiwanese situations / holdout | clause | 580 | 4.7% (27/580) | 5.0% (29/580) | 5.0% (29/580) |
| Historical Taiwanese examples / regression | word | 1024 | 58.1% (595/1024) | 90.3% (925/1024) | 94.3% (966/1024) |
| Historical Taiwanese examples / regression | clause | 1024 | 26.6% (272/1024) | 26.7% (273/1024) | 26.7% (273/1024) |

A good word score does not establish usable phrase composition. Complete-clause retrieval is especially weak for modern Taiwanese. These failures do not prove that every returned alternative is meaningless, and they do not measure completing the same sentence through multiple selections. Stateful composition and human acceptability need additional evaluation.

## Prefixes and imprecision

| Source | Unit | Condition | Top 8 |
|---|---|---|---:|
| Japanese casual chat | word | half | 48.6% (373/768) |
| Japanese casual chat | word | three-quarter | 59.0% (453/768) |
| Japanese casual chat | word | initials | 44.4% (341/768) |
| Japanese casual chat | word | mixed | 44.4% (341/768) |
| Japanese casual chat | word | transpose | 0.1% (1/734) |
| Japanese casual chat | word | omission | 15.6% (117/749) |
| Japanese casual chat | word | neighbor | 0.0% (0/768) |
| Japanese casual chat | clause | half | 6.2% (16/256) |
| Japanese casual chat | clause | three-quarter | 9.0% (23/256) |
| Japanese casual chat | clause | initials | 0.4% (1/256) |
| Japanese casual chat | clause | mixed | 0.8% (2/256) |
| Japanese casual chat | clause | transpose | 0.0% (0/249) |
| Japanese casual chat | clause | omission | 5.1% (13/256) |
| Japanese casual chat | clause | neighbor | 0.0% (0/256) |
| Japanese service dialogue | word | half | 46.1% (354/768) |
| Japanese service dialogue | word | three-quarter | 57.6% (442/768) |
| Japanese service dialogue | word | initials | 42.1% (323/768) |
| Japanese service dialogue | word | mixed | 42.1% (323/768) |
| Japanese service dialogue | word | transpose | 0.0% (0/722) |
| Japanese service dialogue | word | omission | 16.7% (122/730) |
| Japanese service dialogue | word | neighbor | 0.0% (0/768) |
| Japanese service dialogue | clause | half | 23.8% (61/256) |
| Japanese service dialogue | clause | three-quarter | 26.6% (68/256) |
| Japanese service dialogue | clause | initials | 0.8% (2/256) |
| Japanese service dialogue | clause | mixed | 1.2% (3/256) |
| Japanese service dialogue | clause | transpose | 0.0% (0/254) |
| Japanese service dialogue | clause | omission | 12.1% (31/256) |
| Japanese service dialogue | clause | neighbor | 0.0% (0/256) |
| Modern Taiwanese situations | word | half | 17.8% (128/720) |
| Modern Taiwanese situations | word | three-quarter | 48.2% (347/720) |
| Modern Taiwanese situations | word | initials | 20.0% (144/720) |
| Modern Taiwanese situations | word | mixed | 35.7% (257/720) |
| Modern Taiwanese situations | word | transpose | 0.0% (0/633) |
| Modern Taiwanese situations | word | omission | 5.7% (38/668) |
| Modern Taiwanese situations | word | neighbor | 0.0% (0/720) |
| Modern Taiwanese situations | clause | half | 1.9% (11/580) |
| Modern Taiwanese situations | clause | three-quarter | 4.5% (26/580) |
| Modern Taiwanese situations | clause | initials | 1.9% (11/580) |
| Modern Taiwanese situations | clause | mixed | 4.5% (26/580) |
| Modern Taiwanese situations | clause | transpose | 0.0% (0/545) |
| Modern Taiwanese situations | clause | omission | 0.9% (5/578) |
| Modern Taiwanese situations | clause | neighbor | 0.0% (0/580) |
| Historical Taiwanese examples | word | half | 21.4% (219/1024) |
| Historical Taiwanese examples | word | three-quarter | 63.4% (649/1024) |
| Historical Taiwanese examples | word | initials | 23.5% (241/1024) |
| Historical Taiwanese examples | word | mixed | 41.0% (420/1024) |
| Historical Taiwanese examples | word | transpose | 0.0% (0/896) |
| Historical Taiwanese examples | word | omission | 8.4% (78/927) |
| Historical Taiwanese examples | word | neighbor | 0.0% (0/1024) |
| Historical Taiwanese examples | clause | half | 25.4% (260/1024) |
| Historical Taiwanese examples | clause | three-quarter | 26.6% (272/1024) |
| Historical Taiwanese examples | clause | initials | 26.1% (267/1024) |
| Historical Taiwanese examples | clause | mixed | 26.7% (273/1024) |
| Historical Taiwanese examples | clause | transpose | 0.0% (0/969) |
| Historical Taiwanese examples | clause | omission | 8.7% (89/1024) |
| Historical Taiwanese examples | clause | neighbor | 0.0% (0/1024) |

Error rows above exclude controls that leave the original input unchanged. Japanese incomplete units are source morphological tokens; Taiwanese units are supplied-reading syllables. These are diagnostic controls, not observed gesture frequencies. The reference JSON retains all controls and their strata.

## Taiwanese explicit-tone strata

Unmarked means no explicit combining tone mark in the target, not a linguistic assertion of neutral tone. All input is mechanically derived plain POJ. This separates the missing-unmarked-word concern without assuming every unmarked form is neutral.

| Source | Word target | Top 8, complete input |
|---|---|---:|
| Modern Taiwanese situations | marked | 86.9% (524/603) |
| Modern Taiwanese situations | unmarked | 91.5% (107/117) |
| Historical Taiwanese examples | marked | 93.6% (748/799) |
| Historical Taiwanese examples | unmarked | 96.9% (218/225) |

## Paired proposal effects on the new corpus

| Isolated variant | Default changes | Case-folded default gains / losses | Top-8 gains / losses | Any-slot gains / losses | Order changes |
|---|---:|---:|---:|---:|---:|
| baseline | 0 | 0 / 0 | 0 / 0 | 0 / 0 | 0 |
| span | 0 | 0 / 0 | 0 / 0 | 0 / 0 | 0 |
| english-context | 0 | 0 / 0 | 0 / 0 | 0 / 0 | 0 |
| evidence-rank | 1434 | 1 / 100 | 0 / 0 | 0 / 0 | 0 |
| dedup | 0 | 0 / 0 | 0 / 0 | 0 / 6 | 900 |
| unit96 | 0 | 0 / 0 | 0 / 0 | 0 / 0 | 0 |
| prefix-cache | 0 | 0 / 0 | 0 / 0 | 0 / 0 | 0 |

No prototype was retuned after these results. Full gains and losses by source, split, unit, condition, annotation status, explicit tone mark, effective error and exact addon-text overlap are in [results.json](results.json). Changed cases are retained in `changed-outputs.tsv.gz`; their labels never feed generation.

## Independence and limitations

- Japanese splits are 32 development and 64 initial-holdout conversations per source, with schema-inspected dialogues excluded. Speakers may recur. Modern Taiwanese splits are 6 development and 12 initial-holdout source files; related authors and vocabulary remain shared.
- 287 distinct held-out target strings also occur in development. Natural recurring greetings are retained, and no phrase-disjoint claim is made. The initial holdout is now consumed; it cannot be called fresh in a later tuning run.
- Japanese surface forms come from actual source turns. Kana-only references and Sudachi-generated kanji readings are scored separately. OOV tokens and readings that fail independent romanization round trips are excluded and counted, so hard/unknown vocabulary is underrepresented.
- Taiwanese readings come from the authors. Modern Tâi-lô is converted to POJ using pinned Taiwanese tools, retaining CH/chh. Failed conversions are excluded, not guessed. Historical examples share production-source lineage and remain regression data.
- Exact `addons.tsv` target overlap is a diagnostic, not proof that non-overlapping text has independent upstream lineage. It excludes generative kana output and other core vocabulary.
- Whole ordered turns are retained, but this run evaluates isolated compositions with no preceding conversation context. It does not benchmark sustained multi-turn learning, topic coherence or physical touch.
- Japanese accommodation dialogue and historical Taiwanese teaching sentences remain separate from casual chat. The Taiwanese spontaneous-conversation source remains on access/rights hold.

## Verification

Pinned reference-tool versions, source download hashes, deterministic regeneration, input SHA-256, row alignment, nonempty references, and conversation/file split disjointness were checked. All seven isolated variants completed all 61,941 conditions. See the parent [proposal report](../REPORT.md) for core/Rime regression gates and timing boundaries.
