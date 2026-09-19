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

## Next loop

Investigate context and source-frequency ranking among already-retrieved entries.
Use separate natural-conversation and essay results; freeze data roles before
comparison. Measure useful first-row choices, target access and unrelated
suggestion exposure together. No model or source change is accepted yet.
