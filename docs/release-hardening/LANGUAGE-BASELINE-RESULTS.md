# Accepted build: broad input is responsive; candidate tails still fail

September 25, 2026. Runtime `6bffd17` / publication `e89a337`. Verification only;
no runtime change or speedup claim. Four language-specific shard-0 phone sessions
pass against the [frozen plan](LANGUAGE-BASELINE-PLAN.md). All action sequences
match their source/genre/condition inventories exactly. The phone is restored,
display OFF verified, and shared ownership explicitly released.

The workload covers **180 queries, 2,324 letters and 368 Space actions: 2,692
injected actions total**, at both 150 and 60 ms requested key-release intervals.
Chinese uses 32 queries, English 36, Taiwanese and Japanese 56 each. Every raw
editor and Space submission is observed. These counts are not vocabulary accuracy
or physical touch hit rates; submission callbacks are not screen presentation.

Each latency cell below is **p95 / p99 milliseconds**: the values at or below
which 95% / 99% of the observed latencies fall. Candidate percentiles exclude
unobserved frames; the next column preserves observed/all-letter denominators.
The last column requires candidate submission strictly before the next release.

| Mode / interval | Raw | Space | Candidate | Candidate observed | Timely candidate |
|---|---:|---:|---:|---:|---:|
| Chinese / 150 ms | 17.55 / 19.96 | 29.44 / 31.76 | 62.81 / 85.67 | 332/332 | 332/332 |
| Chinese / 60 ms | 17.44 / 20.41 | 25.48 / 25.84 | 56.78 / 60.31 | 330/332 | 326/332 |
| English / 150 ms | 20.76 / 24.28 | 19.77 / 19.82 | 20.36 / 28.77 | 133/133 | 133/133 |
| English / 60 ms | 20.16 / 20.73 | 16.75 / 18.26 | 19.39 / 19.72 | 133/133 | 133/133 |
| Taiwanese + English / 150 ms | 19.38 / 21.79 | 28.42 / 36.80 | 74.51 / 95.32 | 446/447 | 446/447 |
| Taiwanese + English / 60 ms | 20.16 / 23.39 | 28.30 / 30.79 | 69.96 / 76.10 | 421/447 | 385/447 |
| Japanese + English / 150 ms | 20.98 / 23.85 | 29.94 / 31.41 | 68.12 / 72.83 | 254/254 | 254/254 |
| Japanese + English / 60 ms | 20.28 / 22.20 | 25.58 / 27.85 | 70.68 / 78.74 | 254/254 | 232/254 |

All aggregate raw/Space cells are within the 33/50 ms p95/p99 budgets in this
sample. Chinese, Taiwanese and Japanese candidate p95 exceeds 50 ms at both
cadences; Chinese/Taiwanese slow candidate p99 also exceeds 80 ms. English stays
within the candidate budgets. This diagnoses a candidate-update gap, not missing
touches. Missing candidate frames can include unchanged rows, lateness or
supersession; they do not establish missing dictionary entries.

All starts have thermal status 0 and battery temperatures 32.3, 33.9, 33.8 and
33.9 C in execution order. Cooled starts do not eliminate scheduling/thermal
variation. Earlier samples used different inputs/builds/sessions, so no speedup
is inferred. The observer is unhooked and supplies no causal stage attribution;
do not blame a provider, font operation or dictionary solely from these tails.

## Scope and evidence

The same accepted sources were built locally with a test-only non-exported editor
fixture and no debugging/Internet permission. All 24 assets match the accepted
ordinary release and the embedded timing corpus matches its frozen SHA-256.
[Binary identities](language-baseline/binaries.json) and
[session/cleanup receipt](language-baseline/sessions.json) are retained.

Full counts, actual cadence, mean/median/p95/p99/max and source/genre/condition
strata: [Chinese](language-baseline/chinese.json),
[English](language-baseline/english.json),
[Taiwanese](language-baseline/taiwanese.json),
[Japanese](language-baseline/japanese.json). Each small source/condition cell has
only four Space actions per cadence, so its p95/p99 is a maximum, not a stable
tail estimate. Raw spelling/device telemetry stays local.

Chinese edited-chat excerpts and essays are separate; English conversation and
essay references are separate; Japanese casual chat and service roleplay are
separate. Taiwanese authored examples are not spontaneous conversations. All are
reused development sources, not fresh holdouts. This is one shard of four and one
session per mode, below the prescribed 10,000 actions per mode/three sessions.
No Chrome/Keep, human touch, physical display, Android 16/16 KB or Play delivery
certification follows. Public release remains blocked by the recorded gates.
