# Decision after the first layout-imprecision pilot

The four tested adaptations do not establish a better replacement layout. Retain
them as comparison baselines. This result does not establish QWERTY as optimal:
the generator has no learned grip, reach-dependent noise, training cost, or actual
human movement time. No production keyboard, correction setting, or dictionary
has changed.

| Layout | Decision | Reason |
| --- | --- | --- |
| QWERTY | Retain baseline | Lowest forced spatial/frequency error in the primary mild-scatter conversation and essay conditions. It still has dictionary-member confusions such as `of` → `if` and `in` → `on`. |
| Colemak | Retain for comparison; do not ship as an improvement | Slightly fewer raw errors, but no consistent corrected-word advantage. It does improve the essay result under directional bias, so the result is not a blanket rejection. |
| Dvorak | Retain for movement/ambiguity tradeoff studies | Higher modeled alternation and shorter within-word thumb travel, but more corrected-word errors. Movement savings alone do not identify a winner. |
| Split QWERTY, 8 mm gap | Do not adopt this fixed-width adaptation | Its narrower pitch worsens raw accuracy at both widths. This does not reject every split layout or a split keyboard on a wider device. |

The conservative decoder improves over literal input while avoiding most damage,
but cannot repair dictionary-member errors. The forced decoder repairs some of
those errors and has lower aggregate word error, while damaging correctly entered
unfamiliar words. Its perfect-center failure is exactly the 738 out-of-vocabulary
occurrences, not evidence for auto-replacing every unknown spelling. Neither
experimental policy is ready to become the app's default.

## Next experiment

Generate candidate arrangements from production/source-development data using two
separate objectives: thumb movement only, and movement plus word distinguishability.
Use no complaint-specific letter/word weights. Freeze the candidates before
evaluation. Compare them on fresh document/conversation sources with more speakers
and authors, and a physically measured two-thumb error distribution; the present
two conversation documents and one essay document are insufficient for that claim.

Keep geometry, dictionary resources, decoder budget, and assistance policy matched.
Use newly gathered sessions or independent error models to avoid evaluating a
layout only with the model that optimized it. A successful simulation candidate
still needs correction-inclusive human task timing and learning-cost measurement.

## Verification and provenance

- Protocol and input hashes were frozen before the first result. Existing GUM
  regression text and the existing English dictionary were reused; no corpus text
  entered training or production vocabulary.
- Decoder self-checks pass for exact-center identity, reproducible draws,
  exhaustive search versus an independent brute-force calculation, and unknown
  literal retention. All clean literal controls pass in the full run.
- All 120 conditions completed with all 7,200 genre/decoder/seed result rows.
  Per-document counts, all observed dictionary-member confusion pairs, layout
  definitions, hashes, and the runnable harness are retained.
- Source management identifies GUM as evaluation data and AOSP as a decoder input;
  the source catalog's production admission decisions are unchanged.
- This is offline tooling. No Android build or phone session was needed; no GitHub
  Actions were used. No human accuracy, WPM, or Android latency claim.

Read [numeric results](RESULTS.md), [layout diagrams](index.html), and the
[frozen protocol](PLAN.md). The full output can be regenerated with the commands
in RESULTS.md; generated summaries preserve each genre and each noise seed.
