# Isolate the grammatical-contraction source

September 25, 2026, before extraction/evaluation. Source-data experiment only;
accepted runtime is `6bffd17` / publication `e89a337`. Do not combine the held
grammar-gated lexical-admission rule or the rejected direct MASC substitution.

The existing English spelling table uses EWT AUX annotations to permit automatic
apostrophe restoration in mixed modes. Its bare-word protection comes from the
complete AOSP vocabulary. A maintained MIT-licensed English tokenizer table was
already pinned and structurally audited in SPACY-CONTRACTIONS-PLAN.md. Test that
alternative grammar evidence with all existing frequencies and lexical inventory
fixed, before attempting any context replacement.

Keep every `valid` metadata row byte-for-byte. Replace only `contraction` rows by
the full audited spaCy internal-apostrophe/multi-component inventory intersected
with the **current accepted** base English apostrophized lexicon or English
context unigrams observed at least twice. Do not use the MASC counts here. This
changes grammar evidence only, not eligibility, frequencies or dictionary words.
Verify source and MIT notice hashes; use the existing bounded AST interpreter,
never execute upstream Python. Keep all source-only/old-only differences in the
audit without adding exceptions. No production file is overwritten.

Load the separate metadata file through the existing evaluator argument. Compare
6,144 frozen English complete/prefix conditions in English and Chinese modes,
then all 31,527 broad Chinese and 4,608 edited chat episodes. Report complete
inventory/order/Space and first-1/5/8 target/prefix coverage by source, genre and
condition. Runtime cannot see labels. Reused development corpora are not a fresh
holdout. Do not call different non-reference candidates meaningless by default.

Stop if any English-mode output changes: grammar flags must not control that
mode. Reject losses of labelled Chinese Space or first-eight coverage without
equal-or-better coverage within its source/genre. Preserve each changed default,
including unlabelled ones, for a systematic grammar/intent audit. No source is
admitted solely because these broad word probes miss apostrophe cases: a surviving
trial still needs complete source-derived restoration tests, independent grammar
reference evaluation and mixed-mode intent evidence before production.

This removes no context-count dependency and does not resolve the entire rights
gate. It does not change learning, stored state, field policy, suggestions limits,
Chinese readings, Taiwanese/Japanese data, fonts or scheduling. Check source pins
and metadata bytes now; core/native/packaged verification follows only after a
semantic survivor. Keep raw corpus evidence local and publish aggregates only.
