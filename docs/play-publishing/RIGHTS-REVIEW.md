# Remaining source-rights decision — 0.8.4

Status: unresolved. This evidence packet narrows the question; it is not legal clearance.
No production data, extraction rule or ranking weight changed during release preparation.

The pinned UD English EWT r2.15 README grants CC BY-SA 4.0 for annotations and
database rights, while separately describing copyright in underlying source texts.
The [upstream notice](https://github.com/UniversalDependencies/UD_English-EWT/blob/r2.15/README.md)
and [LDC catalog](https://catalog.ldc.upenn.edu/LDC2012T13) were checked September 12,
2026. The annotation license alone does not answer every underlying-text question.

## Exact dependency path

1. tools/compile_context.py reads only pinned training splits: 12,544 English EWT
   sentences and 3,997 Chinese GSD sentences. It lowercases English, retains words
   and internal apostrophes, resets context at sentence punctuation, and exports
   unigrams plus adjacent two/three-token counts. Nonempty contexts require count
   at least two. Chinese entries use glyph contexts. Total export: 74,809 rows.
2. context.tsv SHA-256 is
   0627a69373c47f8ffca381245fd48292a50316ff8cbca53efbfa70e545f60952.
   The compiled model embeds those language-model inputs. Packaging source text
   separately from the app does not remove the derived-output dependency.
3. tools/compile_english_spelling.py intersects the complete AOSP vocabulary with
   apostrophe-stripped words from the base lexicon/context data; EWT training AUX
   annotations classify grammatical contractions. It emits metadata flags rather
   than full sentences. Exact counts and hashes are in
   docs/dictionary-impact/spelling-sources.json.
4. The source archive excludes .conllu.gz training/evaluation files but retains
   upstream notices, hashes and reproduction tools. This packaging exclusion is
   not evidence that distributing every derived table is cleared.

The app exports compact lexical/count data, not the original document IDs or
complete source essays. That is relevant to a qualified rights decision; it does
not establish a universal legal exemption. No permission from the underlying
rightsholders has been obtained in this task.

## Paths to close the gate

- Obtain a documented rights decision covering the exact exported counts,
  spelling metadata and compiled model, with applicable distribution jurisdictions;
  or obtain suitable permission.
- Alternatively replace the affected source systematically. Freeze development
  and independent conversation/essay evaluations first, rebuild context, spelling
  metadata and model together, and compare common-word ranking, apostrophe recovery,
  prefix coverage and latency. Do not simply delete the source and claim unchanged
  typing quality. Taiwan-authored sources remain required for Taiwan vocabulary.

Record the chosen evidence in sources/release-policy.json and sources/README.md.
The source gate remains open until then. All other per-source notices and data
source offerings still apply: original code is Apache-2.0; data licenses include
CC BY-SA, ODbL and LGPL terms described in LICENSING.md. The prepared five source/
data ZIPs preserve attribution and modifiable representations, but do not override
this outstanding decision. No proprietary Google Zhuyin APK/code/artwork is included.
