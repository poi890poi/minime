# Preserve all readings within a declared priority tier

September 24, 2026. Isolated compiler bug experiment; no production refresh.

The pinned McBopomofo compiler reads each priority list into `glyph -> reading`.
Three glyphs have multiple distinct readings within one tier; four rows are
overwritten. The source format specifies one reading per line sorted by glyph,
not a last-row override. A list-order change therefore changes semantic output.
The proposed correction stores a set per glyph and tests membership. It changes
no source row, tier, score formula, floor, phrase score, runtime retrieval limit,
or source interpretation across tiers. Upstream source lists remain untouched.

Verify the diagnosis before compiling a new model: use small generated compiler
fixtures with multiple readings in each tier, reverse row order, and compare
the complete output maps. The pinned implementation should expose order-dependent
weights; the set version must preserve all declared same-tier readings and be
order invariant. A single-reading-per-tier control must be identical. The
existing missing-secondary-tier behavior remains fixed for this experiment.

Rebuild the unmodified pinned compiler/postprocessor first and require its final
data.txt to match the existing raw SHA-256 in weights-manifest.json. Only then
run the set change. Compare every source identity and score, record all changed
rows, and regenerate relative single-glyph priors through the existing audit
tool. No corpus target or Google choice may influence extraction or weights.

For any surviving variant, compare the two pinned prior TSVs in the shared core,
then compare the corrected variant to production on the frozen broad/chat inputs.
Preserve all earlier negative results. The existing 24-query Google observations
are now consumed diagnostic evidence, not fresh holdout. Admission still requires
source editorial validation, independent pronunciation evidence, and release
performance/resource checks. A successful parser fix cannot establish those.

Other findings stay separate: three tertiary-tier glyphs lack a secondary-tier
entry, and the compiler subtracts natural-log discounts from decimal-log source
scores. These are separate hypotheses; do not combine them with this set fix.
