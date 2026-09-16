# Completion-on-Space experiment: reject automatic prefix expansion

Frozen before evaluation: `plan.json`. Runtime baseline is the engine at the
completion-boundary fix with English spelling correction **enabled**, no learning,
ordinary English field, production dictionary unchanged. The preceding spacing
fix does not affect these fresh, single-word episodes.

Data: the existing pinned UD English GUM test extraction and its document IDs,
`docs/conversation-ranking/corpus/gum-test.tsv` / `gum-manifest.json`. Previously
evaluated data, **not a fresh holdout**. No source text enters the production
dictionary here. 20,401 token occurrences, 57,139 condition/token episodes,
14,377 distinct input spellings. These are repeated-word weighted text results,
not independent user trials. Conversations and essays are reported separately;
all 15 source genres are retained in `summary.json`.

Conditions: complete spelling; drop the final character; keep the first half
(rounded up); replace the middle letter with its nearest QWERTY key (skip an
apostrophe at the midpoint to the next letter). The last
three apply to source words of at least four characters. These are synthetic input
conditions against independently authored target text, **not measured human error
rates**. Token/episode totals do not mean assertion-count language accuracy.

Runtime policies, with no threshold tuning:

1. Baseline: current Space acceptance, spelling correction enabled.
2. Top prefix: if baseline retains the raw spelling and it is not a valid source
   spelling or technical token, accept the highest-frequency completion.
3. Existing margin: same, but reuse the current correction gate, score >=100 and
   >=8 above second place (or no second choice). This tests whether the correction
   gate transfers to completion; **it does not**.

The selector sees only typed spelling and the production dictionary. Source words,
genre and error-condition labels are used only for scoring. No complaint-word
exceptions, reweighting, or model entries were added.

## Results

Here **correct/episodes** is recovery of the original source token over all inputs
under that condition. It includes unchanged inputs. **New correct/changes** is the
precision of extra substitutions introduced beyond the existing baseline.

| All genres | Baseline correct/episodes | Top prefix correct/episodes | Margin correct/episodes | Margin new correct/changes |
|---|---:|---:|---:|---:|
| Complete spelling | 20,237/20,401 | 20,167/20,401 | 20,206/20,401 | 0/31 |
| Missing last character | 4,283/12,246 | 5,972/12,246 | 5,376/12,246 | 1,093/1,194 (91.5%) |
| Half prefix | 0/12,246 | 1,288/12,246 | 742/12,246 | 742/2,246 (33.0%) |
| Adjacent-key slip | 9,126/12,246 | 9,126/12,246 | 9,126/12,246 | 0/29 |

| Genre / condition | Margin new correct/changes |
|---|---:|
| Conversation / missing last | 119/124 (96.0%) |
| Conversation / half prefix | 61/156 (39.1%) |
| Essay / missing last | 47/53 (88.7%) |
| Essay / half prefix | 34/114 (29.8%) |

The baseline cannot recover a half-length prefix through its one-edit correction
mechanism in these episodes. That limitation does not justify inserting the wrong
word: top-prefix expansion adds 3,137 wrong substitutions in half-prefix episodes;
the margin still adds 1,504. It also corrupts 31 complete tokens the baseline kept
correct. Existing spelling correction itself has errors on complete source words
(164/20,401); it is optional and remains unchanged here.

## Decision

Reject both automatic-prefix policies. Do not change defaults, reinterpret the
existing spelling-correction option, or add this rule as a hidden fallback.
Manual completion selection remains available, with the separately fixed spacing.
Google's completion-on-Space advantage is therefore **not yet closed**. A future
selector needs calibrated prefix confidence and context evidence, evaluated on a
new document-disjoint corpus after its rule is fixed; ranking a single frequent
completion is not sufficient evidence for accepting it automatically.

## Reproduce

After `tools/test-core.ps1` compiles the shared core:

```powershell
java '-Dfile.encoding=UTF-8' -Xmx1g -cp core/build/manual dev.minime.core.EnglishCompletionEvaluation docs/conversation-ranking/corpus/gum-test.tsv artifacts/english-completion-evaluation.tsv
python tools/summarize_english_completion.py artifacts/english-completion-evaluation.tsv docs/spacing-completion-study/completion/summary.json
```

Raw per-token outputs: `results.tsv.gz` (decompressed SHA-256 in `summary.json`).
`query_ns` times each first-seen spelling's full core composition and all policy
calculations together; repeated episodes reuse it. It is **not** a per-keystroke,
individual-method, phone, or touch-to-screen latency measurement. No latency
improvement is claimed.

Harness review corrected the midpoint-apostrophe case before finalizing these
results: apostrophes do not have a letter-key centre. The earlier run counted
9,090 baseline adjacent-key recoveries; the corrected run counts 9,126. All prefix
and complete-spelling results, and the policy rejection, are unchanged. This was
a simulator correction, not policy tuning.
