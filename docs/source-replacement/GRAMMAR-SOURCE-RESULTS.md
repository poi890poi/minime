# Tokenizer exceptions are insufficient as the sole grammar source

September 25, 2026. **Retain the licensed source, do not replace production flags
with this extraction.** The broad typing screen passes, but the independent
annotation screen exposes coverage omissions that ordinary word probes miss.
No runtime, production asset or release-rights decision changes.

## Controlled source change

The [metadata-only plan](GRAMMAR-SOURCE-PLAN.md) uses the pinned MIT-licensed spaCy
English exception table through the already audited bounded reader. It never
executes upstream Python. Of 812 entries, structural filtering yields 159 distinct
internal-apostrophe forms; **62** meet the unchanged accepted lexical/frequency
eligibility rule, versus 52 old flags. Fifteen flags are added and five absent.
All bare-word protection bytes, dictionaries, scores and context counts remain
fixed. This differs deliberately from the earlier 71-form MASC-frequency pilot.

The metadata SHA-256 is
`73710596c7758e9ce167cc3a36f3174f2c30e7bff2670163dd3678c96a9c1dc7`.
Source and notice pins are retained in SPACY-CONTRACTIONS-PLAN.md. The generated
table and spelling-level audit remain local, with the upstream notice.

## Broad pipeline screen

The new paired configuration receipts verify all inputs identical except the
explicitly declared `spelling` metadata. Among 6,144 word/prefix episodes, all
3,072 English-mode outputs/ranks/Space results are identical. Chinese-mode English
first-one/five/eight/anywhere coverage and every Space result are unchanged;
two half-prefix inventories change below those target metrics. All 31,527 broad
Chinese and 4,608 edited-chat inventories, order, spans and Space are unchanged.
These are reused development sources; no independent conversation accuracy claim.

An initial comparison against the older saved output mixed enabled/disabled
optional dictionaries. It is excluded and corrected in the separate
[configuration audit](../chinese-recovery/ENGLISH-CONFIGURATION-CORRECTION.md).
This experiment uses fresh, matched receipts. No supposed loss/gain from that
configuration mistake enters the decision here.

## Independent grammar annotation screen

The [reference plan](GRAMMAR-REFERENCE-PLAN.md) extracts every eligible
apostrophized multiword surface from pinned EWT dev/test and GUM test annotations,
without querying a candidate or filtering by old/new flag membership. The table
counts **occurrences whose surface is included in the grammar flags**, out of all
eligible source occurrences in that category. It is not automatic-acceptance
accuracy or user intent; repeated occurrences are not independent vocabulary.

| Source / annotation category | Eligible occurrences | Old included | Trial included |
|---|---:|---:|---:|
| EWT dev / AUX-bearing | 207 | 204 | 200 |
| EWT test / AUX-bearing | 233 | 232 | 231 |
| GUM test / AUX-bearing | 206 | 197 | 197 |
| EWT dev / VERB without AUX | 9 | 8 | 9 |
| EWT test / VERB without AUX | 4 | 4 | 4 |
| GUM test / VERB without AUX | 11 | 10 | 11 |
| EWT dev / nonverbal | 67 | 1 | 0 |
| EWT test / nonverbal | 57 | 0 | 0 |
| GUM test / nonverbal | 76 | 1 | 0 |

The trial loses five AUX-bearing EWT occurrences while gaining two non-AUX
verbal occurrences across sources. GUM AUX coverage is unchanged. Non-AUX verbal
forms can be legitimate contractions and must not be counted automatically as
false positives. Nor does an AUX annotation prove a spelling safe to restore
automatically among Chinese initials. EWT shares annotation/source lineage with
the old training flags; GUM is independent of that training but already exposed
evaluation material. Per-GUM-genre and unique-surface results remain in the
aggregate receipt. No source sentences or reference spellings are published.

The cause is a source-boundary mismatch: tokenizer **exceptions** are not an
exhaustive grammatical lexicon. Their absence does not mean a form is invalid;
ordinary tokenization can handle forms absent from this explicit table. Do not
patch the omitted words or claim the broad unchanged score proves completeness.

## Disposition

Keep spaCy as attributed grammar evidence for a separately declared design, not
a standalone replacement. A future design must explain both source coverage and
the grammar-versus-typing-intent boundary before combining sources or changing
lexical admission. The existing MASC counts and this trial are not combined.
No phone test or app build is justified for this incomplete source replacement.

`grammar-source/` contains aggregate English/broad/chat/reference reports. Raw
data stays under ignored `artifacts/source-audit/grammar-source/`. The bounded
source-reader tests and annotation-category fixture pass. The existing EWT
context/spelling dependency and public-release rights item remain open.
