# Japanese output forms

Type: requested behavior/data change, baseline MinIME 0.7.5 (`57fcbd1`).
Japanese lookup currently offers generated romaji as an extra output alongside
the source kana and compatible kanji. Stop emitting that generated output from
both JMdict vocabulary and JMnedict name extraction. Keep the original source
spelling, reading/sense restrictions, romanized input aliases, kana/kanji, single
characters, English secondary and exact raw recovery. No word exceptions or
score tuning; no source snapshot, eligibility filter or other language changes.

This is an extraction decision registered in the existing source catalog before
regeneration. Pinned WanaKana still creates input readings; it no longer supplies
Japanese candidate output. Original Latin spellings in a source record are not
generated romanization, and user-authored shortcuts are not rewritten.

Remove the rows before indexes are built, so excluded outputs cannot occupy
bounded lookup slots. Compare all old/new rows and query keys to ensure only
generated Japanese romanization rows disappear and every old native-script row
remains. Validate extraction against pinned original sources, full and incomplete
shared-core candidate/Space behavior, existing frozen Japanese retrieval probes,
English scope controls and the pinned Rime evaluator before Android packaging.
Source retrieval and synthetic controls are not independent language accuracy or
visible-phone latency measurements. No phone UI change is planned.

## Results

The complete row/source audit removes 24,386 generated outputs (22,838 JMdict,
1,548 JMnedict). No rows were added. All native source spellings and every
reading/source/category key are retained; all other language rows are identical.
Japanese distinct output strings decrease from 62,547 to 40,942, leaving 43,921
Japanese rows. These are output forms/aliases, not distinct phrase counts.
Source archive pins, eligibility, single kana/kanji and runtime logic are unchanged.
See [extraction.json](extraction.json) for hashes and complete-diff checks.

The 24,996 previously frozen native-kana queries retain every reachable target,
and 201 additional partial targets become reachable within the existing budgets:

| Condition | Queries | Available before → after | Top eight before → after |
| --- | ---: | ---: | ---: |
| Complete reading | 22,953 | 22,953 → 22,953 | 22,819 → 22,848 |
| Half reading | 1,024 | 418 → 567 | 191 → 265 |
| Initials | 1,019 | 948 → 1,000 | 766 → 867 |

This reuses source-retrieval probes, not a fresh conversational holdout. Rank
positions are dictionary choices, not physical screen rows. No typing-latency
or memory improvement is claimed from the reduced inventory.

Validation: 34,116 shared-core assertions, including Space/default coherence,
full/partial source samples, English scope and kana-character controls. Pinned
desktop Rime passes 628 uncached queries; all 480 output records match 0.7.5.
Source contracts pass 19 tests. Asset verification passes. Paired Taiwanese forms
are byte-identical; their manifest records the new aggregate add-on asset hash.

Reproduce extraction with `python tools/audit_japanese_output.py` (also in CI),
adding `--baseline artifacts/japanese-output/baseline-addons.tsv` for the exact
row comparison. Run `tools/test-core.ps1` and `tools/test-desktop.ps1 -Corpus
docs/suggestion-latency/native-inputs.tsv`. `JapaneseCoverageAudit` uses
`app/src/main/assets/addons.tsv`, `app/src/main/assets/japanese-basic.tsv` and
`docs/japanese-coverage/inputs.tsv`; its archived output and comparison hashes are
in this directory. `python tools/report_japanese_output.py` reproduces the summary.
