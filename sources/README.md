# Source management

The catalog records editorial judgments separately from source identity and typing
frequency. A dataset's size, Traditional characters, `zh-tw` tag or GitHub stars
cannot establish that it contains natural Taiwanese vocabulary. Prefer original
Taiwan-authored or locally curated data. Never turn a Simplified Chinese corpus
into Taiwan vocabulary by conversion. Preserve source spelling and POJ orthography.

This framework extends the existing dictionary, add-on, Rudy and Rime manifests.
Those manifests remain responsible for source URLs, original-byte hashes, licenses,
extraction rules and detailed counts. This catalog adds stable source IDs, editorial
evidence, known coverage, limitations, intended uses and lifecycle decisions.
It is build tooling; it neither runs on Android nor changes candidate scores.

## Decisions

| Decision | Meaning |
|---|---|
| retain | Continue the reviewed use of an established source. Other uses need review. |
| pilot | Worth an isolated audit/lookup experiment; no production inputs permitted. |
| hold | Missing access, license, coverage or editorial evidence. |
| replace | Existing release inputs and add-on contributions are frozen while a better source is evaluated. |
| exclude | Unsuitable for the proposed use. |
| reference | Evaluation/discovery evidence; not production vocabulary. |

`replace` is migration debt, not a quality endorsement. It does not remove the
currently shipped 0.6.4 entries. The input ledger and per-source add-on row hashes
prevent silent refresh or expansion of those sources. Changing the decision needs
an independently reviewable catalog change with new evidence; a lock refresh alone
cannot approve changed frozen data. There is no automatic promotion from pilot.

## Files and commands

- `catalog.json`: source-level decisions and evidence. No handpicked entry list.
- `release-policy.json`: unresolved distribution-rights questions. Separate from
  editorial retain/replace decisions; compilation cannot close a rights review.
- `lock.json`: source fingerprints, audit fingerprints and packaged provenance
  groups. Text line endings are normalized only for the ledger digest; original
  source manifests continue to check original bytes. Binary hashes are exact.
- `evaluation.json`: corpus roles, genres, exposure, selection, overlap limitations
  and required result breakdowns. Existing test manifests remain authoritative.
- `../docs/sources/AUDIT.md`: measured coverage, source quality, decisions and gaps.
- `../docs/sources/DECISIONS.md`: generated overview of the catalog.

```sh
python tools/sources.py check
python -m unittest discover -s tools -p test_sources.py
python tools/sources.py report
python tools/fetch_source_audit.py  # network, cached, audit only
python tools/audit_source_inventory.py  # offline; cached or committed audit files
```

The compiler entry points check the catalog and locked sources before extracting.
The context compiler additionally checks decompressed training bytes against the
existing UD source manifest; those cached training files need not be present in CI.
CI verifies the ledger and source-contract tests in addition to existing asset
verification. Unknown add-on provenance, unknown add-on manifest inputs, ambiguous
owners, changed pins and unaudited sources fail verification.

For a reviewed source refresh, update its snapshot, detailed manifest and catalog
evidence first, then run `sources.py lock`. Rebuild through the existing compiler,
compare the old/new output and update the ledger again. Frozen sources cannot take
this route without a documented source decision change. Review the complete diff;
the tool does not decide whether new evidence is convincing.

## Audit contract

Record who creates and reviews content, correction practices, original language,
source lineage, material reuse terms, snapshot/commit, selection boundaries,
category and time coverage, field completeness, duplicates, pronunciation support,
aliases and exclusions. Review a frozen stratified sample for editorial quality;
do not use complaint examples to choose production entries or weights.

Measure inventory coverage, reading resolution and complete/initial/mixed lookup
separately. Compare common-conversation output and English competition before
promotion. A high-quality specialist name can be rare: dictionary inclusion does
not authorize top-candidate promotion. No source here supplies automatically trusted
frequency weights. Keep each source independently removable and attributable.

## Essays and conversations

Both are useful evaluation genres. Essays exercise specialist names, terminology,
longer phrasing and punctuation in sustained context. Conversation exercises short
turns, fragments, colloquial wording, code switching and contractions. Report them
separately, including category, language, complete/initial/mixed input and imprecision
condition. An optional aggregate must declare its weights and retain those separate
results; corpus size must not silently decide the product's priorities.

Reuse the existing GUM genre labels: its essay and conversation rows already have
separate identities. The old `conversation-ranking` Chinese corpus is substantially
encyclopedic, despite its folder name. The catalog records that limitation rather
than changing historical results. Taiwan.md's frozen essay samples can support a
new evaluation experiment after target/readings and overlap checks; the current
audit measures source structure, not IME accuracy.

Split by original document or conversation before extracting phrases. Keep related
documents, aliases and all simulated keystroke variants within the same split.
Fingerprint both raw documents and normalized passages; check shared upstream
lineage and near-duplicates, not only file paths. If a dictionary is derived from a
test article, report that article as a seen-source retrieval/regression test. Obtain
a new independent holdout for generalization; never relabel inspected material as
fresh. Readings must come from independent annotations or reviewed reference data,
with ambiguity retained, rather than the decoder being scored.

Apply missing/adjacent/transposed-letter and timing/touch simulations consistently
to both genres with frozen seeds and separate error conditions. These simulations
test tolerance under a specified error model; real interaction still requires
device observations. Do not train conversational frequencies from essay counts
without a separate, explicitly evaluated decision.

The checker pins evaluation artifacts, requires genre and exposure declarations,
rejects an inspected corpus labelled as a fresh holdout, and rejects a registered
evaluation file also listed as a production input. It does not perform semantic
overlap detection, enforce result formatting inside every old benchmark, or make
the current Taiwan.md sample an annotated typing benchmark.

Catalog checks are structural, not a semantic proof of good taste or complete
provenance. Existing native Rime and context-model limitations remain explicit.
The framework covers the registered language-data inputs and add-on provenance;
native dependency security and a universal software supply-chain catalog are outside
its scope. Dataset-specific parsing and source-rights details stay with their owners.
