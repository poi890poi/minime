# Propagate explicit Latin casing to completion lookup

September 24, 2026. Bug fix, independent of rejected lowercase case-inclusive
display and focused-priority trials. IntentClassifier already assigns Latin
intent to input containing an uppercase letter, but CompositionEngine passes
only prior Latin context or English mode to the complete-vocabulary lookup.
Consequently a fresh title-case/all-caps prefix cannot complete a capitalized
source word even though both its classified intent and typed casing are Latin.

Propagate that existing explicit casing signal to the completion lookup. Keep
the dictionary's existing title/all-caps/mixed-case validation and output casing.
Do not change lowercase Pinyin, Space/default policy, source data, scores,
completion limits, grammar admission or Chinese/focused-language ordering.

First reproduce the missing source-word completion through the real composition
engine using a tiny attributed-shape fixture. Require title/all-caps access on
every English-enabled board, correct output casing, raw Space preservation,
literal/private-field boundaries and unchanged lowercase Chinese behavior.
Run the standard core and pinned desktop checks before any Android build.
Compare frozen lowercase English and Chinese chat outputs with the accepted
baseline: complete inventory/order and Space must be byte-equivalent excluding
timing. This proves scope isolation, not language accuracy. Additional source-wide
case checks should compare the real engine's completion inventory with its existing
English-mode path. No new vocabulary or word-specific production exception.

For that source audit, include every ASCII title-case or all-caps AOSP entry with
at least three letters. Use its half-length prefix, rounded up with a two-letter
minimum, in title and uppercase forms; deduplicate queries before execution.
Use an English-only dictionary fixture loaded from the full unchanged production
English TSV, so the real engine exercises mode dispatch without conflating it
with Chinese ranking. Compare literal forward-completion identities and scores
against English mode on all four English-enabled modes. This checks one declared
prefix position per source word, not all partial lengths or natural usage.

This closes the explicit-casing inconsistency only. Lowercase proper-name recall,
grammar-source migration, quality/latency, rights and platform release gates remain
open. Device evidence must not be inferred from desktop checks.
