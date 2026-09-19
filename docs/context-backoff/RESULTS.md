# Development loop: context backoff — rejected

**Problem:** sparse two-character context bypasses evidence in the one-character
context. **Change tested:** hierarchical smoothing with the existing mass 20,
unchanged source counts, score clamp, boundary weight, vocabulary and budgets.

Counts below mean the intended dictionary word appeared first or within the first
eight results after the correct preceding context had already been accepted.
They do not measure free-running conversations or semantic nonsense rates.

| Genre | Probes | First choice before → after | First eight before → after |
|---|---:|---:|---:|
| Encyclopedic regression | 2,400 | 1,451 → 1,481 | 2,076 → 2,107 |
| Taiwan essays | 40,386 | 19,242 → 19,534 | 32,132 → 32,232 |
| Authored conversation scenarios | 105 | 70 → 69 | 96 → 97 |

The essay first-choice change comprises 509 gains and 217 losses; encyclopedic
results comprise 59 gains and 29 losses. Conversation had one first-choice loss
and no gain. The experiment therefore fails the predeclared no-genre-regression
gate. **The estimator was removed.** Do not tune its constants to the lone loss.

The conversation set contains just eight previously authored scenarios, expanded
to 35 source-covered spans and three reading conditions. Essays use 64 frozen,
hash-selected documents. All text was previously evaluated. The broad result
suggests context estimation matters, but does not establish that this estimator
is appropriate for everyday conversation. Genre-matched evidence remains missing.

All 42,891 candidate/span inventories were unchanged; 10,345 orders changed.
Trial core suite: 370,462 contract assertions passed, not an accuracy figure.
29,345 unique host lookups had p50/p95 0.530/2.043 ms before and 0.576/2.565 ms
after. These are single runs, not a proven slowdown or phone touch measurements.
No native/device gate was run for the rejected model.

`summary.json` separates full/initial/mixed conditions and reports gains/losses.
`changes.jsonl.gz` preserves every changed target rank and its first eight choices.
`rejected/` retains the exact trial and focused test. Corpus generation and reporting
are reproducible with `tools/make_context_backoff_corpus.py`,
`ContextRankingEvaluation` and `tools/report_context_backoff.py`. The source counts
were neither retrained nor expanded; source replacement decisions remain intact.
