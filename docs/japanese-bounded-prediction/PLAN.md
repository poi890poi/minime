# Bounded lexical prediction experiment

Type: isolated performance/availability experiment, outside Android. Baseline is
the pinned upstream lexical predictor with full-input prefix and the audited
posting-offset index. Preserve its cost ordering and complete candidate lists.

Before evaluation: freeze an 8 ms cooperative deadline and 8,192 work checkpoints
per query. Check traversal, reading expansion, token expansion and sorting. On
exhaustion discard the entire result and report unavailable; never show a partial
enumeration as a ranked result. Existing MinIME remains the fallback provider.
The elapsed deadline is soft: an individual upstream primitive and OS scheduling
can overshoot. Measure this; do not claim hard real-time guarantees.

Reuse all 384 frozen development probes from japanese-predictive-review, unchanged.
Report source, clause/word and full/partial strata separately. Run three passes;
compare every completed list against the uncapped upstream predictor. Labels are
used only afterward for scoring. This is a development screen, not fresh quality
validation. No weights, dictionary entries, budget sweeps or complaint cases.

Screen gates: zero incomplete lists exposed, exact ordered parity for completed
queries, p95 engine <= 10 ms and maximum <= 16 ms per isolated desktop pass.
Report unavailable counts and partial hit rates even when latency passes. This
cannot replace the focused-language provider merely by returning empty quickly.
An Android integration needs shared-core/provider, lifecycle and phone gates.
