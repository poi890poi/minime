# Shared add-on partial matching

329,599 unique pack/form/input/output cases, 221,728 unique inputs. Every eligible source row is included; no success-selected examples. Complete aliases and outputs are unchanged (56,869 normalized language-asset records verified equal). All packs use the same bounded prefix index and reading-unit trie as core Chinese matching. Japanese units come from pinned WanaKana conversion tokens; POJ and Chinese boundaries come from source separators.

Top-eight **lookup retrieval**, not real-world language accuracy. All four packs enabled simultaneously. Each source-compatible spelling counts separately, including Kana/Romanization/Kanji outputs. Initials can collide with many words; eight slots cannot contain every target. “Isolated ceiling” assumes an ideal oracle allocating all eight slots to the tested pack.

| Pack/form | Cases | Before top 8 | After top 8, all packs | After top 8, pack alone | Isolated ceiling |
|---|---:|---:|---:|---:|---:|
| geography/full | 35,645 | 98.25% | 98.25% | 98.25% | 98.25% |
| geography/initial | 16,327 | 95.76% | 95.90% | 95.96% | 95.97% |
| geography/mixed-left | 16,327 | 0.05% | 99.28% | 99.74% | 99.98% |
| geography/mixed-right | 16,331 | 0.05% | 99.19% | 99.67% | 99.91% |
| geography/prefix | 35,639 | 0.01% | 54.42% | 54.91% | 84.62% |
| japanese/full | 5,809 | 99.98% | 99.98% | 100.00% | 100.00% |
| japanese/initial | 5,789 | 0.43% | 97.58% | 100.00% | 100.00% |
| japanese/mixed-left | 5,794 | 11.70% | 99.86% | 100.00% | 100.00% |
| japanese/mixed-right | 5,790 | 1.88% | 99.26% | 100.00% | 100.00% |
| japanese/prefix | 5,809 | 0.00% | 99.38% | 99.90% | 100.00% |
| poj/full | 39,014 | 99.99% | 99.99% | 100.00% | 100.00% |
| poj/initial | 19,288 | 0.06% | 46.45% | 53.69% | 62.24% |
| poj/mixed-left | 34,828 | 1.99% | 98.47% | 98.73% | 99.62% |
| poj/mixed-right | 33,786 | 0.71% | 96.02% | 96.35% | 98.79% |
| poj/prefix | 39,003 | 7.72% | 99.05% | 99.38% | 99.52% |
| taiwan/full | 4,299 | 89.11% | 89.11% | 99.88% | 99.88% |
| taiwan/initial | 1,942 | 76.52% | 76.52% | 99.95% | 99.95% |
| taiwan/mixed-left | 1,942 | 0.31% | 98.61% | 100.00% | 100.00% |
| taiwan/mixed-right | 1,942 | 0.31% | 99.28% | 99.95% | 100.00% |
| taiwan/prefix | 4,295 | 0.07% | 49.71% | 53.48% | 80.58% |

Complete-key retrieval does not regress. POJ initial lookup remains substantially ambiguous; the oracle ceiling distinguishes slot collisions from search/ranking losses. Prefix cases also include truncated legacy Chinese initial aliases, which are not prefixes of full readings. The same search supports partial source units; no language-specific minimum input length or runtime expansion list is used.

Desktop component costs: exact-only load 545 ms / 26.3 MiB retained; indexed load 1235 ms / 45.1 MiB retained. All-pack lookup p50/p95 changed from 1/3 µs to 1595/5981 µs. Approximate heap after GC; large-run timing overlaps other validation and is not an isolated speed benchmark or Android latency. This is a real resource cost, not a claimed optimization. The Android path therefore runs base and optional lookups on the same worker and publishes one revision-bound result. Existing switches still isolate optional data; loaded indexes remain cached.

Reproduce with `python tools/make_addon_matching_corpus.py` and `AddonMatchingBenchmark <output-prefix>`. The baseline uses 146bf1e classes with the identical boundary-enriched asset (normalization parity verified), and the shared-index run uses the new matcher. Evaluation labels never enter lookup. Exact aliases are tried before incomplete matches; partial matches use existing omitted-letter penalties and source-order ties. Corpus/asset hashes and raw ranks are retained alongside this report.
