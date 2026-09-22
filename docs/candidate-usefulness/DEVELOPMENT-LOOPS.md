# Development loop reports

Each loop records the problem, evidence-backed cause, attempted change, measured
gain or regression, and decision. A passing test is not a language-quality gain.
Percentages must identify their numerator, denominator and input conditions.

## 2026-09-19: reading-match evidence — committed as `9875177`

- **Problem:** ranking could not distinguish syllable abbreviations from forward
  spelling predictions consistently across providers.
- **Cause:** the syllable matcher reused the completion marker; English completion
  omitted it; equal-score duplicate paths lost the stronger match evidence.
- **Change:** preserve the distinction through matching, copies and deduplication.
  Keep the existing conservative rules for Space and dictionary promotion.
- **Result:** contract defect fixed. Candidate lists and Space remain identical on
  31,527 Chinese episodes and 6,144 English word/prefix episodes. Visible suggestion
  quality has **not** improved from this commit alone.
- **Rejected attempt:** treating abbreviations as fully typed pushed 670 intended
  phrases out of the first eight among 24,244 labeled tasks and changed 3,578 Space
  choices. It was removed.

[Full report and verification](match-evidence/RESULTS.md)

## 2026-09-19: stronger reading evidence for the preview — withdrawn

- **Problem:** a forward prediction might lead a better phonetic match.
- **Hypothesis:** prefer a match covering every source syllable in the existing
  Chinese preview slot, without moving English positions or changing Space.
- **Change tested:** exchange two existing Chinese candidates when that condition
  occurs. No additional words, guesses or score weights.
- **Result:** zero Chinese-order changes in 31,527 episodes; five Chinese-list
  changes among English probes, with no target-coverage improvement. The affected
  branch did not explain the measured quality gap.
- **Decision:** remove the extra rule. Keep its patch and evidence for review.

[Full report and verification](match-evidence/preview-experiment/RESULTS.md)

## 2026-09-19: contextual backoff — withdrawn (`ab5a1f7` records evidence)

- **Problem/cause:** a sparse two-character context bypasses available evidence
  from the last single character.
- **Change tested:** smooth the longer context with the shorter one, preserving
  the frozen counts and existing constants.
- **Result:** intended first choices gain 30 / 2,400 encyclopedic probes and
  292 / 40,386 essay probes, but lose one / 105 authored conversation probes.
- **Decision:** reject under the declared genre gate. The conversation sample is
  only eight scenarios, so broader conversation improvement remains unproven.

[Full report, individual gains/losses and source lineage](../context-backoff/RESULTS.md)

## 2026-09-19: shared context-score computation — accepted

- **Problem/cause:** two complete score passes repeat identical work after their
  preceding-character states converge.
- **Change:** share the repeated calculations while preserving both original
  floating-point sums and the unchanged model.
- **Improvement:** context-scoring elapsed time falls 15.3% in ten alternating
  desktop benchmark rounds, from 0.954 to 0.808 µs per score difference. This is
  one computation component, not an overall typing-latency improvement claim.
- **Quality:** all 42,891 candidate lists/orders/spans remain identical; core and
  pinned desktop gates pass. No better language coverage is claimed.

[Full report, timing boundaries and exact-score verification](../context-boundary/RESULTS.md)

## 2026-09-21: Rudy source reading boundaries — accepted

- **Problem:** source-backed names fail initial and mixed Pinyin lookup.
- **Cause:** compact upstream tags are indexed as one reading unit; derived
  geography readings already preserve syllable boundaries.
- **Change:** add aliases only for unique syllabification respecting source
  separators and Han glyph count. Keep all existing names and readings.
- **Improvement:** among 2,359 supported name/readings, initial-only top-eight
  retrieval rises from 15 to 2,153; mixed spelling rises from 212 to 2,283.
  Every original full tag still retrieves its target.
- **Costs:** +0.43 MiB asset, approximately +1.63 MiB desktop heap; no material
  pack-lookup latency change. Two shortened geography queries lose their targets
  under the fixed result cap. Broad Chinese first-eight coverage gains three and
  loses one; one English prefix in Chinese mode moves from eighth to ninth.
  English-mode candidates and all measured Space outputs remain unchanged.
- **Scope:** this fixes a data-processing defect, not general conversation ranking.

[Full report, losses, resource measurements and reproduction](../rudy-reading-boundaries/RESULTS.md)

## Next loop

The September 22 release-hardening loop adds 4,608 chat-derived conditions and
rejects extending the duplicate-source guard: 15 first-eight Chinese gains versus
44 losses, with no first-choice gain. Production ranking remains unchanged.
[Problem, cause, trial and measured outcome](../release-hardening/QUALITY.md).

Investigate the remaining candidate-cap losses with precision and recall together.
Do not raise limits without evidence about the displaced alternatives. Broader
natural Taiwan conversation evidence is still needed before replacing context
scores; keep their data unchanged until a supported replacement passes.

## 2026-09-23: Off-screen candidate allocation — accepted

- **Problem:** Chinese typing delays raw text, even with decoding on a worker.
- **Cause:** the collapsed strip eagerly creates views for up to 1,651 candidates.
- **Change:** allocate visible/nearby candidates in batches and extend on scroll;
  preserve every candidate, its order and acceptance span.
- **Improvement:** Chinese raw-frame p95 is 26.8/33.2 ms at 150/60 ms key intervals,
  versus 148.8/559.3 ms in the repeated baseline. Returning to the old build
  reproduces the delay. Fourteen final phone checks pass.
- **Costs and limits:** other modes' p95 increases roughly 2–7 ms in these short
  replays. Candidate latency still misses its target; expanded-grid creation is
  unchanged. These are injected-input/frame-submission measurements, not physical
  touch accuracy or completed performance acceptance.

[Full report, all four runs, tradeoffs and remaining gates](../release-hardening/RENDER-RESULTS.md)
