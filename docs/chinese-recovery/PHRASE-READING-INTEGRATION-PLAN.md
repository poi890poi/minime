# Integrate only after verifying the pronunciation model boundary

September 25, 2026. The isolated phrase-share model passed its declared primary
screen and the new Google diagnostic. It is still outside production. Next is
a potential data behavior change, not a new runtime prediction mechanism.

Before refreshing production, compare the current native/core merge with and
without the already-frozen trial TSV on the same broad and chat corpora. Keep
every other asset, runtime class and native source fixed. Preserve all exact
Pinyin and tone-bearing Zhuyin single-glyph reading access, all dictionary row
identities, phrase frequencies, English vocabulary and optional-language data.
Any loss of exact source access rejects integration. Evaluate English retention
and native target/prefix ranks separately; do not call source identity coverage
language accuracy. Record all changes rather than relying on aggregate gains.

The TSV count is shared by Pinyin and Zhuyin. Its allocation distinguishes
toneless Pinyin readings, not tone variants: it changes weights between readings
in both lookup indexes but cannot resolve tone-frequency differences within one
Pinyin spelling. This is an explicit compatibility boundary, not a claim that
the Zhuyin board is unaffected. Rime's own source files and scores stay fixed.

If these checks survive, integrate the deterministic estimator into the existing
dictionary compiler, register the production decision before refreshing, and
require byte-identical reproduction of the frozen candidate hash. Rebuild binary
assets, run core and pinned desktop checks, then test the packaged candidate on
the authorized phone with restoration/display-OFF verification. Compare measured
load/query cost and artifact size; no release-latency certification follows from
an offline-only estimator. Keep this data change in its own commit.

Completeness ordering is a separate follow-up. Current conversion merges exact
and incomplete paths by score, deduplicates text, then the composition layer
sorts by span, learning and score again. Sorting only the first layer would not
fix presentation. Before modifying either owner, reproduce a generated case
where a fully typed syllable loses to a high-frequency completion; include an
initial-only control and a learned-choice control. Freeze its scope and gate
before coding. Do not tune phrase shares or introduce per-query exceptions.
