# Stable Japanese conversion ordering

The posting-offset optimization now passes exact ordered-candidate parity over
all **18,730 regression records**. Its repeat-pass engine p95 is **3.19–3.23 ms**,
against **36.35–36.69 ms** without the index. Retain this combination for the
optional conversion provider; Android integration and phone timing remain unrun.

## Cause and fix

The pinned converter used state memory addresses to break equal-cost A* ties.
Changing allocations could therefore reorder otherwise identical results. The
patch replaces only that final tie-break with a per-search insertion ordinal.
Costs, position/length priorities, graph traversal, top-eight limit, beam size,
dictionary bytes and acceptance adapter remain fixed. There are no vocabulary
exceptions or reference-dependent weights.

`TieRegression.cpp` exercises the actual upstream comparator. The original fails
when allocation order disagrees with insertion order; both patched builds pass.
The test also verifies that a lower cost still takes precedence. This establishes
allocation independence for this comparator, not universal cross-toolchain
determinism for every sort or traversal in the upstream project.

## Exact parity and historical changes

| Frozen regression set | Records | Stable vs indexed-stable differences |
|---|---:|---:|
| Conversation completion, development | 512 | 0 |
| Full/partial/error probes, development | 3,876 | 0 |
| Conversation completion, old holdout | 1,536 | 0 |
| Full/partial/error probes, old holdout | 12,606 | 0 |
| AJIMEE reviewed encyclopedia items | 200 | 0 |

The comparison includes candidate order, membership, target ranks, acceptance,
committed output and action counts; only timing fields are excluded. It does not
weaken the previous failed gate. Relative to the historical pointer-order run,
199 records change candidate lists. None changes the recorded target ranks,
completion/hit outcomes, action counts, preferred AJIMEE output or committed text.
This intentionally establishes a defined tie rule rather than preserving an
allocation accident.

Both conversation genres and encyclopedia items here were already evaluated.
The old `holdout` filename is lineage, not a freshness claim. This is a parity
regression, not new language-model accuracy evidence. See the original
[corpus and evaluator limitations](../japanese-engine-benchmark/README.md).

## Desktop performance and resources

Three separate process passes per variant, reverse variant order in pass two.
Each pass types all 1,311 keys from 64 frozen development clauses twice. No result
cache. Windows, GCC 10.3.0, Release libraries and `-O3` adapter; source and data
pins remain in [source-manifest.json](../japanese-engine-benchmark/source-manifest.json).
Native query timing excludes Python kana conversion, IPC and rendering; wall
timing adds evaluator/IPC overhead. These are not Android touch-to-display times.

| Repeat-pass metric | Stable, no index | Stable, posting index |
|---|---:|---:|
| Mean engine time | 9.56–9.61 ms | 1.00–1.01 ms |
| Engine p50 | 4.47–4.55 ms | 0.45–0.46 ms |
| Engine p95 | 36.35–36.69 ms | 3.19–3.23 ms |
| Engine p99 | 62.01–62.50 ms | 9.30–9.35 ms |
| Maximum engine time | 72.30–73.21 ms | 10.00–10.09 ms |
| Wall p95 | 36.53–36.89 ms | 3.32–3.37 ms |
| Model load | 299.6–301.8 ms | 315.4–318.3 ms |
| Loaded process RSS | 69.60–69.62 MiB | 72.45–72.47 MiB |

First-pass and repeat-pass distributions, maximums and process memory snapshots
are retained in `summary.json`, per-key `.jsonl.gz` and `*-load.json`. A short
parity-file read overlapped the first timing run; the separately repeated passes
show the same speedup. No compiler or other benchmark ran alongside timing.
The dictionary payload remains 39,535,732 bytes. RSS describes these desktop
processes, not the full multilingual Android keyboard or a predicted APK cost.

The earlier exhaustive posting audit still applies: all 745,964 reachable
posting lists and 1,289,075 token payloads agree. The upstream final unterminated
posting remains unreachable; fixing it would intentionally change coverage and
belongs in a separate change. See [the audit](../japanese-predictive-review/README.md).

## Reproduce and decision

Use the modern Python and pinned dependencies documented in the original
benchmark. The preparation script verifies the original source pin and creates
separate source copies under ignored artifacts.

```powershell
python docs/japanese-determinism/prepare.py
./docs/japanese-determinism/test-original.ps1
./docs/japanese-determinism/build.ps1 -Variant stable
./docs/japanese-determinism/build.ps1 -Variant indexed-stable
# Repeat for completion/probes, both roles, and ajimee for both variants:
python docs/japanese-engine-benchmark/run.py kazuma-stable completion --role development --output-dir docs/japanese-determinism
# Three sequential passes per variant; reverse order in pass two:
python docs/japanese-engine-benchmark/run.py kazuma-indexed-stable perf --pass-number 1 --output-dir docs/japanese-determinism
python docs/japanese-determinism/report.py
```

Land the isolated deterministic fix and retain the indexed converter for the next
provider-contract step. Do not replace partial prediction with whole-sequence
conversion. No Android source/assets changed, no APK was built and no device was
operated. Source provenance is registered; see [NOTICE.md](NOTICE.md).
