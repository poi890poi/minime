# Independent focused-dictionary search budgets

Behavior change: focused packs get 24 word choices per index, matching the core
reading-unit choice budget, instead of sharing an eight-choice budget with Chinese
culture/geography. Keep the existing Japanese expression/general-vocabulary split;
Taihoa general vocabulary gets the same separation from everyday POJ. Character
lookup remains bounded; no unbounded expansion or per-word scores. Chinese-only
pack lookup remains eight choices. This removes cross-language retrieval starvation.

The search.jsonl.gz run changes lookup policy on unchanged 0.7.3 assets, after the
mode-only policy stage. final.jsonl.gz then changes data with the same search rules.
Each contains 3,922 conditions x six modes; coverage-results.json separates all
stages, genres and errors. Reference labels never enter lookup/ranking.

The 24-choice bound increases lower-ranked reachability, not necessarily first-row
hits. The broad 128-probe Japanese half-reading availability rises from 96 to 125
with Chinese secondary; both secondaries then have the same focused coverage.
Full source-common Japanese retrieval stays 22,953/22,953. Its larger partial audit
rises from 224 to 418 of 1,024, and initials from 796 to 948 of 1,019. Top-three and
top-eight counts in that audit are unchanged: newly reachable choices are lower
in the expanded candidate list. These are retrieval tests, not language accuracy.
