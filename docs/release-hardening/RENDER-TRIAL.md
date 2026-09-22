# Candidate strip rendering trial

Frozen baseline: runtime fd728d5, phone latency session
bfca5b37-cd44-4474-8866-02522236f9be. Same eight frozen queries, every prefix,
150 ms and 60 ms input intervals, all four modes. Preserve raw telemetry and
missing observations; this is not a human-touch or presentation benchmark.

Observed Chinese raw-frame submission p95: 158.47 / 544.55 ms. English:
30.91 / 28.48 ms. Host inspection of those same prefixes returns up to 1,651
base candidates. The collapsed Android strip creates/binds/measures a TextView
and divider for every candidate, and reuse scans its existing pool linearly.
This is a plausible main-thread cost, not yet a proven complete explanation.

Trial: materialize the collapsed strip in batches of 24, extending on forward
scroll near the loaded end. Reset the materialized range for a new input or mode.
Keep the full candidate list, order, consumption spans, expanded grid, literal
recovery, font filtering and acceptance logic. Defer updates during a candidate
gesture as before. Batch size is a view allocation policy, not a vocabulary cap.

Require: unchanged core/desktop contracts; all candidates remain reachable by
horizontal scrolling and expanded selection; a new query resets allocation;
stale touches cannot select a replacement; unchanged rows preserve identities.
Repeat the identical injected latency sample. Reject if Chinese raw latency does
not improve or other modes regress materially. Report cold/warm limitations and
missing matching frames rather than claiming lower conditional tails are a win.

Outcome: [accepted with measured costs and open gates](RENDER-RESULTS.md). The final
batch also covers two viewports on wide displays; it remains 24 on the test phone.

Expanded-grid construction remains eager in this trial. Do not claim that its
opening/scroll cost or physical touch latency is fixed.
