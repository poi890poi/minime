---
pretty_name: Taiwanese Mandarin web n-grams
license: cc0-1.0
language:
  - cmn
language_bcp47:
  - cmn-Hant-TW
language_creators:
  - found
annotations_creators:
  - machine-generated
multilinguality:
  - monolingual
source_datasets:
  - extended
task_categories:
  - text-generation
task_ids:
  - language-modeling
size_categories:
  - 1M<n<10M
tags:
  - taiwanese-mandarin
  - taiwan-mandarin
  - ngrams
  - frequency
  - corpus-linguistics
  - web-corpus
configs:
  - config_name: token_1gram
    data_files: token_1gram.tsv
    sep: "\t"
    header: null
    names: [hosts, unigram]
  - config_name: token_2gram
    data_files: token_2gram.tsv
    sep: "\t"
    header: null
    names: [hosts, bigram]
  - config_name: token_3gram
    data_files: token_3gram.tsv
    sep: "\t"
    header: null
    names: [hosts, trigram]
  - config_name: token_4gram
    data_files: token_4gram.tsv
    sep: "\t"
    header: null
    names: [hosts, fourgram]
---

# Taiwanese Mandarin web n-grams

[![DOI](https://img.shields.io/badge/DOI-10.57967%2Fhf%2F9810-blue)](https://doi.org/10.57967/hf/9810)

Word 1–4-gram counts for Taiwanese Mandarin\* (臺灣華語, `cmn-Hant-TW`), computed over the
Taiwan slice of a large web crawl after variety filtering by
[twfilter](https://github.com/taiwan-corpora/twfilter) 0.1.0 with the published
[twfilter-tables](https://huggingface.co/datasets/taiwan-corpora/twfilter-tables):
every sentence behind these counts passed the 教育部 character-inventory gate, the
simplified-character round-trip, the mainland-orthography, mainland-lexicon,
written-Cantonese, Hong Kong and Singapore detectors, and block-level evidence of
Taiwan-specific usage. Counts only: no document, no sentence, no context.

## Intended uses

Frequency norms for Taiwanese Mandarin where existing resources describe mainland
usage or conflate the two varieties: lexicography and word-list construction,
stimulus selection and frequency covariates in psycholinguistics, n-gram
language models and smoothing baselines, segmentation and input-method
vocabularies, collocation extraction, and cross-variety comparison against
mainland, Hong Kong or Singapore corpora. The counts are host counts — the
number of independent websites using a form — so they measure how widespread a
form is across Taiwanese web authorship, not how often it is uttered.

Limitations to weigh before use: a single register (the open web as captured by
Internet Archive and Common Crawl crawls), no part-of-speech annotation,
tokenization fixed by one segmentation vocabulary, and the boilerplate residuals
documented below. For spoken-like or
edited-register norms, complement with a corpus of that register.

## Provenance

Source: [HPLT 3.0](https://hplt-project.org/) `cmn_Hant`, quality bins 7–10
(4 116 754 documents of 113 442 082 in the language). HPLT claims no rights in the
textual content and licenses its packaging under CC0; the counts here are derived,
not redistributed — see [NOTICES.md](NOTICES.md).

Admission gate — the union of three conditions, minus a hard exclusion:

```text
keep  ⟸  host ∈ *.tw  ∪  html_lang ∈ {zh-TW, zh-Hant-TW}  ∪  host ∈ allowlist
drop  ⟸  host ∈ *.cn ∪ *.hk ∪ *.mo ∪ *.sg ∪ *.my            (unconditional)
```

2 001 161 documents admitted: 541 467 by `.tw` host, 1 459 694 by `html_lang`.
The allowlist is empty — see the note at the end.

Pipeline: [twpipeline](https://github.com/taiwan-corpora/twpipeline) stages 02–09,
policy `corpus`, filtering by twfilter 0.1.0. Punctuation normalized to 教育部
《重訂標點符號手冊》; sentences segmented; characters checked against the three MOE
standard charts and an OpenCC-derived simplified round-trip; vocabulary checked
against corpus-verified mainland markers, written-Cantonese characters, Hong Kong
and Singapore forms and an erhua two-window rule; blocks required to carry
Taiwan-specific evidence; sentence types deduplicated; words segmented by Viterbi
over a unigram cost model with EM re-estimation.

| stage | in | out | lost |
| --- | ---: | ---: | ---: |
| 03_segment | 2 001 161 docs | 37 772 352 sentences | — |
| 04_script | 37 772 352 | 37 122 017 | 1.72 % |
| 05_lexicon | 37 122 017 | 36 455 533 | 1.80 % |
| 06_evidence | 36 455 533 | 29 089 952 | 20.20 % |
| 07_dedup | 29 089 952 | **12 339 761 types** | 57.58 % |

285 516 791 tokens over 149 022 hosts.

## What a count means here

**Each n-gram is counted once per host, not once per occurrence.** This is not a
detail; it is the difference between a frequency list and a spam census.

The web corpus is dominated by templated content whose repetition is
*sub-sentential*. The phrase 提供相關細節的諮詢服務 occurs 263 210 times inside
263 210 **distinct** sentences, spread over 222 900 documents and 1 948 hosts.
Sentence deduplication cannot see it — every string differs. Per-document counting
cannot see it — every document differs. Host capping cannot see it — the network
spans 11 691 throwaway domains, none individually large. In an occurrence-counted
build over the same admitted documents, 貸款 ("loan") ranked 7th among all words,
which is simply false as a fact about the language.

Counting once per host asks *how many independent sites use this phrase* rather
than *how many times a template was published*. For web-derived frequency this is
the correct semantics regardless of spam: a generated page is not an independent
act of authorship. Counted by host, 貸款 falls from rank 7 to rank 1 299, and the
unigram head becomes 的 是 有 一 在 了 我 會 也 要 不 個.

**Known residual.** Eleven of the top hundred four-grams are fragments of the
Chinese Facebook tagline 讓人們盡情分享，將這個世界變得更開闊，聯繫更緊密, carried
by the social-plugin embed on roughly 3 900 independent hosts at about one
occurrence each. Per-host counting cannot suppress it because those hosts are
genuinely independent; it is third-party interface text and would need upstream
boilerplate stripping or a stoplist. Loan-broker template phrases likewise
survive where the network is wide enough — 哪裡 可以 借 錢 spans 2 475 nominally
distinct hosts — so register-sensitive work should treat high-order n-grams
carried by commercial boilerplate with care.

## Files

| file | rows | contents |
| --- | ---: | --- |
| `token_1gram.tsv` | 35 149 | `hosts \t token` |
| `token_2gram.tsv` | 604 187 | `hosts \t token token` |
| `token_3gram.tsv` | 292 666 | `hosts \t token token token` |
| `token_4gram.tsv` | 101 914 | `hosts \t token token token token` |

Sorted by count descending, then by key, `LC_ALL=C` collation. `hosts` is the
number of distinct hosts on which the n-gram occurs. Tokens are separated by a
single space; columns by a tab. UTF-8, LF.

**Script purity.** Across all 1 033 916 published n-grams: zero simplified-only
characters, zero characters of mainland traditional orthography, zero characters
of written Cantonese, zero Hong Kong or Singapore word forms, and zero Han
characters outside the three MOE standard charts, verified against
[twfilter-tables](https://huggingface.co/datasets/taiwan-corpora/twfilter-tables)
after counting. One bigram, `信 息` at 42 hosts, is a segmentation artefact of the
documented 通信息 / 可信息 exception rather than the mainland word 信息.

## Publication floor

No n-gram below **40 hosts** is published. Combined with sentence-type
deduplication, that means 40 independent sites, not 40 repetitions of one
template — stricter than the Google Books Ngrams rule of 40 books — and it makes
reconstruction of any source document impossible by construction rather than by
assertion.

## Known properties of the source

**Gate quality.** Unique Taiwan-attested sentence types produced per admitted
document: `.tw` host 7.81, `html_lang` on a generic TLD 5.56. A 1.41× gradient,
not a cliff. The `html_lang` gate supplies 65.7 % of the corpus at about two
thirds of the per-document yield.

**Host concentration.** Top 10 hosts hold 8.8 % of sentence types, top 100
20.8 %, top 1 000 46.3 %, top 10 000 82.8 %. The single largest is
`chinatimes.com` at 3.3 %. Per-host counting neutralizes this for the published
figures.

**Allowlist.** 300 candidate hosts were derived automatically from the 1 927 814
documents that failed both gates, and then **not used**. Ranked by evidence
density the list is headed by localized zh-TW product pages of international
vendors, which pass every origin test because localization vendors use correct
Taiwanese vocabulary, but which are translated rather than natively composed.
They would have added 0.3 % more volume.

## Reproduction

```bash
twpipeline 01_ingest --input cmn_Hant/7_1.jsonl.zst --residue residue.jsonl > kept.jsonl
twpipeline run --from 02_normalize --to 07_dedup --drop --out staged < kept.jsonl
twpipeline run --from 08_tokenize --to 09_count --vocabulary segvocab.json \
  --orders 1,2,3,4 --floor 40 --once-per host --out ngrams < staged/07_dedup.jsonl
```

Use the staged form, not a shell pipe: with six processes on a pipe the whole
chain runs at about 3.5 of 18 cores because every stage blocks on a 64 KiB pipe
buffer, while the staged form reaches 8–11 cores per stage.

The full build runs on a single MacBook Pro (Apple M5 Max, 12 of 18 cores used,
128 GB unified memory) in under two hours wall time, peaking at 47.9 GB resident
during the external-sort count. Per-stage wall time, CPU seconds, parallelism and
peak memory are recorded in twpipeline's
[BENCHMARKS.md](https://github.com/taiwan-corpora/twpipeline/blob/main/BENCHMARKS.md).

## Position among Taiwanese Mandarin corpora

The corpora usually cited for Taiwanese Mandarin are the web corpus
[TaiwanWaC](https://www.sketchengine.eu/taiwanwac-chinese-corpus/) and, for
speech, the
[Sinica Taiwan Mandarin Conversational Corpus](https://tmc.ling.sinica.edu.tw/corpus_list_en/)
(TMC) and the
[NCCU Corpus of Spoken Taiwan Mandarin](https://spokentaiwanmandarin.nccu.edu.tw/).
The spoken corpora are complements, not alternatives: TMC records 43 hours of
conversation from 170 speakers (397 693 lexical words), NCCU a smaller set of
face-to-face conversations in discourse-analytic transcription, and neither is
a source of web-scale written frequency. The direct comparison is TaiwanWaC,
and the differences are of design rather than size:

| | TaiwanWaC | twngrams |
| --- | --- | --- |
| volume | 260 M words | 285 516 791 tokens over 12 339 761 sentence types |
| what a frequency is | occurrences in the crawl | independent hosts using the form |
| variety control | web collection of traditional-script text; no text-level filter published | per-sentence script, orthography, lexicon and evidence checks; per-stage losses published |
| post-hoc verification | — | zero simplified-only, mainland-orthography, written-Cantonese and HK/SG forms across all 1 033 916 published rows |
| deduplication | document-level (Corpus Factory pipeline) | exact sentence-type dedup (57.58 % of sentences removed) plus ≥ 5-host boilerplate flagging |
| access | Sketch Engine subscription, query interface | CC0 files, direct download |
| rebuildability | search-engine collection, not repeatable | versioned public crawl, published pipeline and per-stage benchmarks |

Two of those rows are measured claims, not positioning. *Variety:* language
identification cannot separate the four written standards that share
traditional script, and URL metadata is not sufficient either — in this very
corpus, sentences already admitted by a `.tw`-host or `html_lang: zh-TW` gate
still lost 1.72 % on character inventory, 1.80 % on lexicon and 20.20 % on
block-level Taiwan evidence. A corpus assembled from Taiwan URLs without a
text-level filter retains exactly that material. *Counting:* occurrence counts
inherit host concentration — the largest single host here holds 3.3 % of
sentence types and the top 100 hold 20.8 % — and sub-sentential template
networks inflate single phrases by two orders of magnitude (the 135× case
documented above). Host counts are immune to both by construction.

What TaiwanWaC has that this dataset deliberately does not: full sentence
context, concordances, word sketches and PoS annotation. Work that needs KWIC
lines should use TaiwanWaC or rebuild from a crawl with
[twpipeline](https://github.com/taiwan-corpora/twpipeline); work that needs
open, redistributable, dispersion-based frequency norms is what these tables
are for.

## Citation

```bibtex
@misc{taiwan-corpora-twngrams,
  title        = {Taiwanese Mandarin web n-grams},
  author       = {Patin, Den},
  year         = {2026},
  version      = {0.1.0},
  doi          = {10.57967/hf/9810},
  publisher    = {Hugging Face},
  howpublished = {\url{https://huggingface.co/datasets/taiwan-corpora/twngrams}}
}
```

Origin filtering by twfilter, archived as
[10.5281/zenodo.21761465](https://doi.org/10.5281/zenodo.21761465); its reference
tables are published as
[taiwan-corpora/twfilter-tables](https://huggingface.co/datasets/taiwan-corpora/twfilter-tables).

Author: Den Patin, [ORCID 0009-0009-6496-382X](https://orcid.org/0009-0009-6496-382X).

\* *Taiwanese Mandarin* (臺灣華語; BCP 47 `cmn-Hant-TW`) denotes the variety of
Mandarin in general use in Taiwan, as produced by its native and near-native
speakers, together with the lexical, phonological and morphosyntactic properties
that distinguish it from the codified standard of the ROC Ministry of Education
(標準國語, Standard Guoyu) and from PRC Putonghua (普通話). The term follows Her
(2009) and the naming of the Corpus of Contemporary Taiwanese Mandarin
(臺灣華語文語料庫, National Academy for Educational Research); much of the phonetic
literature calls the same variety *Taiwan Mandarin*. It is not 臺灣國語, the
Hokkien-accented sociolect of that variety, nor Taiwanese Hokkien (臺語).
Her, One-Soon 何萬順 (2009). 語言與族群認同：從台灣外省族群的母語與台灣華語談起
[Language and group identity: On Taiwan Mainlanders' mother tongues and Taiwan
Mandarin]. *Language and Linguistics* 10(2), 375–419.
