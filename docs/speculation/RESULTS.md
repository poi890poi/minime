# Speculative candidate benchmark

Baseline: 146bf1e (0.6.2); provenance-only commit 679e668. All eligible CC-CEDICT entries, no handpicked cases. 108,895 unique dictionary targets, 108,981 reading pairs; 25,023 natural multi-token spans; 670,020 labeled cases / 373,181 distinct inputs. Attestation vocabulary: 292,038 entries. Labels and membership are used only after decoding.

## Origin and attestation

| Retained path | Candidate occurrences | Vocabulary-attested |
|---|---:|---:|
| Core multi-unit paths alone | 11,232,656 | 0.57% |
| Extra core multi-unit paths appended to Rime | 11,053,941 | 0.25% |
| Native Rime results (provenance unclassified) | 5,377,006 | 74.71% |

Membership is **not** semantic accuracy. Real sentences are often absent from dictionaries; an attested word can be the wrong interpretation. Native prefix choices are included in occurrence counts. Native results cannot be classified as lexical or composed through the public desktop API.

## Intended target retrieval

Percentages use each group's complete case count. Top-eight position includes prefix choices, matching the actual candidate list; a target must consume the full input. “No extra combinations” removes only proven core multi-unit additions, not Rime hypotheses.

| Group | Cases | Rime+core top 1 | Top 8 | No extra combinations top 8 | Any rank | No extra combinations any rank |
|---|---:|---:|---:|---:|---:|---:|
| dict-seen-full | 55,203 | 70.36% | 95.70% | 95.70% | 99.84% | 99.79% |
| dict-seen-initial | 55,203 | 15.29% | 28.78% | 28.78% | 48.34% | 48.34% |
| dict-seen-mixed-left | 55,203 | 35.89% | 59.08% | 59.08% | 93.67% | 93.66% |
| dict-seen-mixed-right | 55,203 | 32.64% | 59.05% | 59.05% | 92.57% | 92.57% |
| dict-seen-prefix | 55,203 | 14.32% | 27.72% | 27.72% | 93.58% | 93.49% |
| dict-unseen-full | 53,778 | 61.54% | 79.34% | 79.34% | 87.63% | 82.86% |
| dict-unseen-initial | 53,778 | 18.80% | 32.06% | 32.06% | 44.92% | 44.78% |
| dict-unseen-mixed-left | 53,778 | 41.94% | 51.73% | 51.73% | 71.57% | 69.68% |
| dict-unseen-mixed-right | 53,778 | 36.00% | 51.25% | 51.25% | 70.46% | 69.43% |
| dict-unseen-prefix | 53,778 | 18.57% | 25.21% | 25.20% | 47.51% | 31.40% |
| natural-dev-full | 13,058 | 57.66% | 59.21% | 59.18% | 90.49% | 59.56% |
| natural-dev-initial | 13,058 | 6.32% | 10.22% | 10.22% | 25.03% | 13.73% |
| natural-dev-mixed-left | 13,058 | 28.26% | 30.83% | 30.81% | 63.81% | 32.33% |
| natural-dev-mixed-right | 13,058 | 19.99% | 23.80% | 23.80% | 52.37% | 25.82% |
| natural-dev-prefix | 13,058 | 18.62% | 20.28% | 20.26% | 80.63% | 23.96% |
| natural-test-full | 11,965 | 56.64% | 58.19% | 58.19% | 90.38% | 58.60% |
| natural-test-initial | 11,965 | 5.67% | 9.29% | 9.29% | 24.66% | 13.11% |
| natural-test-mixed-left | 11,965 | 26.64% | 29.38% | 29.36% | 62.98% | 30.82% |
| natural-test-mixed-right | 11,965 | 18.64% | 22.69% | 22.69% | 51.53% | 24.79% |
| natural-test-prefix | 11,965 | 18.53% | 20.24% | 20.23% | 79.83% | 23.64% |

## Decision and limitations

Reject deleting composition from the fallback decoder. On natural-test full input, core-only top-eight retrieval falls from 81.65% to 6.10% without it. Instead, when the primary decoder supplies a whole-token result, retain its hypotheses and supplement only stored core entries. If native decoding is unavailable or supplies only prefix choices, retain the complete fallback. No frequency weights, entries or per-word exceptions change.

For the measured Rime+core lists, removing extra core combinations changes no first choice and loses 5 top-eight hits across 59,825 natural-test cases. Deep-list retrieval declines substantially (see table); the decision favors a smaller candidate list over those low-ranked recoveries. The candidate-origin trace supports blaming the extra fallback combinations, not calling every Rime/reference mismatch nonsense.

Natural spans are held-out annotated token windows, not a curated conversation corpus. Readings come from dictionary entries, and polyphonic alternatives can produce readings that are wrong in sentence context (for example the alternate readings of 家/會). Therefore these are controlled input-to-target retrieval measurements, **not** an estimate of real-world sentence accuracy. Chinese variant spellings are also not normalized away. Existing conversation corpora are an additional required gate. No absolute natural-language accuracy is claimed.

Core and native large runs overlap in wall time. Their timings include allocation and, for native mode, IPC; do not compare them as a speedup or as Android latency. Provenance parity independently verified 3,829 input-hash-selected queries and 121,624 candidates, with identical text/score/order signatures.

Reproduce: `python tools/make_speculation_corpus.py`; compile core/tests; run `SpeculationBenchmark artifacts/speculation-core 3`, then with `-Dminime.benchmark.native=true` for native mode. Native mode requires the pinned desktop bridge/Rime DLL and isolated synthetic user directories. Raw per-target results and input/source hashes are retained here; large candidate traces remain under artifacts with hashes in raw-artifacts.json.

Derived CC-CEDICT data is CC BY-SA 4.0 (MDBG/CC-CEDICT contributors); UD Chinese GSD is covered by its vendored source licence; McBopomofo data retains its vendored attribution. See third_party/cedict, third_party/ud and third_party/mcbopomofo. No evaluation corpus is bundled into the app.
