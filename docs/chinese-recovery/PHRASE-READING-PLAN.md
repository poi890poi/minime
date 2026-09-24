# Pronunciation shares from existing phrase evidence

September 24, 2026, before producing or evaluating this variant. Isolated data
model experiment using only the retained McBopomofo source already in production.
No additional vocabulary, MOE-derived production data or manual exceptions.

The production importer copies a whole-glyph occurrence count to every reading.
Curated source priorities improve some defaults but cover only a subset of
heteronyms; demoting one unusual reading can expose another unsupported leader.
The independent modern-reading check documents that limitation. Rather than
combining parser, tier and log-discount changes, test one new statistical prior
against the unchanged production data.

For every positive-frequency multi-Han word, retain all distinct source-supplied
toneless Pinyin reading sequences having one unit per glyph. Split that word's
count equally across its distinct sequences; repeated tone variants of the same
Pinyin sequence do not multiply evidence. Each glyph occurrence contributes that
share to its observed reading. No evaluation text, reference labels, optional
dictionary or Google output enters the estimator.

For each single glyph, normalize these counts over its existing distinct Pinyin
readings. Allocate its original whole-glyph frequency according to those shares.
A reading with no supporting multi-glyph evidence receives zero effective count
when other readings have evidence; it remains an exact selectable dictionary
entry. If no reading has evidence, use a uniform distribution over the existing
readings. One-reading glyphs and every multi-glyph row stay unchanged. Use exact
rational accumulation; no tuned smoothing, score boost or per-reading threshold.

This estimates Pinyin reading shares, not tone shares: source rows sharing a
toneless Pinyin reading share the resulting count. That matches the present
Pinyin deduplication boundary. It does not claim to fix Zhuyin tone ranking.
Dictionary phrase frequency is imperfect evidence of individual-character use;
ambiguous phrase readings receive equal mass, not independently observed speech.
Report training omissions and absent evidence explicitly.

Required structural controls: input-order invariance, duplicate-tone invariance,
frequency-mass conservation by glyph/Pinyin, no new/deleted identities, unchanged
multi-glyph counts, and all existing exact single-glyph readings reachable.

Primary screen: the frozen 405-query MOE complete-reading reference comparison.
Require more supported Space choices, fewer reading mismatches in the first
eight, and no aggregate loss of supported first-eight glyph slots. Preserve
per-query gains/losses and distinguish uncovered glyphs, phrases, literals and
partial selections. Stop if this screen fails; do not tune the estimator to it.
If it passes, evaluate all frozen broad/chat and English probes, with separate
genre/condition results, before any production import or app build. Do not demand
that every old rank survive a deliberate pronunciation correction; require
unchanged full-spelling reachability and report every displaced reference.
Independent modern-reading evidence must support any ranking tradeoff; later
latency/resource/device and release-rights gates still apply.
