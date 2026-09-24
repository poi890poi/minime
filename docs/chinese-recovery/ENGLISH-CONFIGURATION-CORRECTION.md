# Correct the English comparison configuration

September 25, 2026. Verification defect found while testing an independent grammar
source. The saved completeness English output reproduces exactly, excluding
query timings, when the current evaluator runs **without optional dictionaries**.
The earlier comparison baseline enabled Taiwan/geography packs. Its reported
22 gained Chinese-mode English first-eight references therefore cannot be
attributed to the complete-match rule. Withdraw that improvement claim.

Evidence so far: a fresh accepted-runtime run with both packs differs from the
saved trial in 542 full candidate inventories. A fresh run without packs matches
all 6,144 saved episodes, including ranks and Space. CandidateOrder,
CompositionEngine and PhoneticDictionary disassembly is identical to the retained
trial classes. The discrepancy is configuration, not the new grammar flags.
Broad Chinese outputs reproduce exactly with both packs, so this finding does
not by itself invalidate that separate comparison or the packaged integration.

Before further conclusions, replay the pre-completeness and accepted runtime with
identical current assets, corpora, metadata and explicitly enabled packs. Compare
every English output and reference rank, retain all Chinese-mode Space changes,
and publish corrected aggregates. Stop any admission claim if this exposes a
regression under the original coverage contract. Neither replay may see labels.

Tooling change: the mixed-English evaluator must record its actual packs, input
hashes and loaded runtime identity beside each new output. The paired reporter
must reject unexplained configuration differences. Permit a declared individual
source-input change for an explicitly isolated data experiment, not a blanket
ignore-config option. Older reports without configuration receipts remain
historical/unverified for this check; never silently label them matched.

This changes verification only, not runtime or production data. Include a small
independent mismatch fixture to demonstrate that the original no-packs versus
packs mistake fails before counting improvements. Preserve the original report
as superseded evidence rather than deleting it.

## Corrected result

The fresh before/after replay uses the same evaluator and identical verified
corpus, Chinese/English/syllable/context/spelling/Taiwan/geography hashes. Only
the three loaded runtime classes differ, reverting the completeness rule on the
before side. All 3,072 English-mode episodes retain full inventories, ranks and
Space. Chinese-mode English first-eight coverage is **2,218 -> 2,218 of 3,072**,
with no gains or losses; first-one/first-five and anywhere coverage also match.
There are 28 Chinese-mode inventory/order changes and 21 Space changes, preserved
locally for audit. The original 22-gain claim is invalid, but the corrected
English-retention gate passes. No production change follows from this correction.

The new evaluator writes `.config.json` receipts binding enabled packs, actual
input hashes, loaded class hashes and output hash. The reporter rejects pack/key
mismatches, stale/one-sided receipts and undeclared changed inputs before counting
results. A source experiment may name its changed input, such as `spelling`;
runtime identity changes remain visible for algorithm comparisons. Five focused
tests pass, including the original pack mismatch and stale-output failure. Legacy
outputs with neither receipt are explicitly marked unverified, never certified
configuration-equivalent. Corrected aggregates are in
`completeness/single/english-corrected.json`; the old JSON remains superseded.
