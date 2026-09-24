# Language-specific typing workload

September 24, 2026. Test/data tooling, no runtime or dictionary change. Freeze
selection before querying either the accepted build or held font-page trial.
Reuse the existing corpus registries and timing observer instead of creating a
new quality oracle. The shared eight-spelling replay remains historical evidence.

Inputs: existing `language-contract-benchmark/inputs.tsv` for English conversation,
English essay and Taiwan.md essay readings; its `conversations/inputs.tsv` for
Japanese RealPersonaChat/ASDC and Taiwanese SuiSiann/historical authored examples;
`release-hardening/chat-inputs.tsv.gz` for edited MozTW chat excerpts. Carry source
file hash, exact line, document/example identity, source role, genre, input
condition and original reading. Never consult output/rank/target availability for
selection. These sources are previously inspected/evaluated material, not a new
language-quality holdout. Historical authored Taiwanese examples are not natural
conversation; there is no Taiwanese/Japanese essay claim.

For each mode/source/condition stratum, deduplicate identical input spellings,
retain the lowest salted SHA-256 identity for ties, and select the first 16 by
SHA-256 using seed `minime-language-timing-20260924-v1`. Include full, half,
initials, mixed, transpose, neighbor and omission conditions when already present
in that source. Do not invent missing variants or balance by performance. Filter
only to 1–32 ASCII lowercase letters supported by the current physical letter-key
observer. Report all exclusions and sparse/missing strata, including punctuation,
tone digits, longer input and English initial/mixed conditions. No per-word edits.

The device must select inputs by active mode, preserve stratum labels in every
raw sample, and support bounded shards without changing the observed actions.
Start with a correctness/feasibility smoke shard; then compare matching frozen
shards and conditions on A/B with balanced order and thermal/power records.
Validate manifest/asset equality and exact shard coverage before phone operations.
Continue recording raw/Space missing frames, conditional candidate timing and
next-release deadlines. Retain the same injected key timing and submitted-frame
observer; no stage hooks or per-key main-thread lookup in timing comparisons.

Report letter/Space counts and mean/median/p95/p99/max by mode, source/genre,
condition and cadence. Empty or tiny cells stay unmeasured/provisional. Single
query resets do not establish multi-turn context, switching, Backspace, gesture,
Chrome/Keep, physical touch, or the 10,000-actions-per-mode release gate. Those
need their own prescribed runs. A larger input inventory alone is not a measured
improvement. Any timing acceptance decision must examine missed observations and
acceptance costs as well as candidate percentiles.

Frozen inventory: 720 queries in 45 mode/source/condition strata, 16 per stratum.
Each stratum is distributed by its frozen position modulo four: every mode has
four shards containing four queries per source/condition. Chinese has 128 queries,
English 144, Taiwanese 224 and Japanese 224. Corpus SHA-256:
`5c316cebea4ed88352a946fafe01fcda4e922c49a47b12783996a53ec1995f49`.
Each shard runs both existing 150/60 ms release-spacing conditions; its source and
condition cells therefore have only four Space samples, insufficient for stable
tail estimates. The host partition test checks uniqueness, completeness and mode
isolation. Public test methods are `TouchLatencyTest#test{Chinese,English,Taiwanese,Japanese}Shard{0,1,2,3}`;
run one method per guarded phone session so reports cannot overwrite one another.

The same-text Space observer correction is independently documented in
SPACE-OBSERVER-FIX.md. It is applied identically to A and B, with legacy typing
methods retained. The first Taiwanese shard is a feasibility baseline, not an A/B
result or tuning set. Corpus/source/condition labels are written after the replay;
no dictionaries or candidate lists are queried by timing hooks. The reporter uses
unique query IDs and original sample adjacency to avoid connecting identical
spellings from separate source episodes.
