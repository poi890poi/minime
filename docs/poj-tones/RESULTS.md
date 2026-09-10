# Missing POJ words, including unmarked input

The importer discarded entire attested words, not merely their tone marks. A user
typing plain letters could not retrieve those words because the entries never
reached the dictionary. Two causes were verified in the pinned source CSV files:
iTaigi's aligned slash-separated variants were rejected as one invalid input, and
Unicode tone marks in an input field failed an ASCII-only eligibility test.

The shared parser now imports aligned variants from all three existing sources and
creates a plain-letter alias from supported source POJ spelling. Existing numeric
aliases remain. Original output spelling, tone marks and CH orthography remain.
No words, tone permutations, frequency exceptions or individual promotions were
handpicked. Input-field inconsistencies do not silently authorize invented numeric
tones; those entries gain a plain-letter alias only.

Complete asset comparison against 5bcb90f:

- 4,336 added rows: 4,309 iTaigi and 27 beginner-vocabulary rows.
- 1,555 newly available distinct POJ spellings; 81,844 total POJ spellings.
- Zero removed rows, zero other-language row changes, zero unattested added outputs.
- Existing Han pairing policy yields 113 additional examples and six changed
  examples when recovered entries supply an eligible source; no pairing is lost.
  There are 51,421 pairs. This is source familiarity policy, not community consensus.

## Retrieval evidence

The exhaustive inventory contains 170,056 reading/output/category targets. Runtime
evaluation freezes every recovered spelling under **complete unmarked input** plus
a SHA-256-selected source-wide control sample, before inspecting ranks. The partial
sample halves the input length, with a fixed hash selection. It does not select
production data by query success. Queries reuse production-source readings and
spellings: this is source retrieval, not independent accuracy, commonness, fresh
holdout, conversation or essay coverage.

| Condition | Targets | First eight before | First eight after | Unavailable after |
| --- | ---: | ---: | ---: | ---: |
| Complete unmarked reading | 2,278 | 687 | 2,278 | 0 |
| Half of unmarked reading | 898 | 139 | 263 | 431 |

All recovered spellings are covered by the full-input probe set. There are no lost
full-input results. Partial lookup remains weak: five previously available results
are displaced as recovered words compete within the existing bounded candidate
budgets. The net first-eight improvement does not erase those regressions. No
ranking weights or candidate budgets were adjusted to these targets. Full results
and category breakdowns are retained in `retrieval.json` and the compressed TSVs.

Remaining boundaries: unsupported punctuation/annotations, unaligned source lists,
and overlong forms are still excluded rather than guessed. Sources may omit modern
everyday words or dialect readings. The three-source parser reports 389 rejected
records/variants; this is not 389 distinct missing words. Source inventory cannot
prove the absence of an intended word from Taiwanese generally. Initials and mixed
complete/incomplete syllables retain existing core regression coverage; this new
audit measures full and half input only. It does not certify all partial patterns.

An attempted exhaustive runtime traversal was stopped because it also ran costly
partial completion for each exact lookup. Its incomplete timing/output file is not
used as a benchmark. The bounded frozen comparison is reproducible; its first/second
lookup microseconds are diagnostic only, not a matched performance experiment.

## Verification and reproduction

Five parser regressions cover aligned variants, malformed Unicode input fields,
preserved tones, refusal to guess unaligned variants, and punctuation boundaries.
Shared core passes 34,128 assertions, including source-derived add-on composition,
acceptance and language-isolation checks. Source contracts pass 19 tests; asset
verification and deterministic paired-form checks pass. Pinned desktop Rime baseline
completed 13,014 inputs / 11,272 uncached native queries before building the phone
test; this data-only POJ change leaves Rime assets and runtime logic unchanged.

Save `5bcb90f:app/src/main/assets/addons.tsv` as
`artifacts/poj-tones/baseline.tsv` using a byte-preserving git extraction. Run
`tools/audit_poj_inventory.py` to reproduce inventory/probe files, then
`PojInventoryAudit` with baseline asset, current asset, `probes.tsv.gz` (or
`partial-probes.tsv.gz`) and output TSV arguments. `tools/report_poj_inventory.py`
reproduces the summary. The new corpora are registered as previously evaluated
source regressions in `sources/evaluation.json`.
