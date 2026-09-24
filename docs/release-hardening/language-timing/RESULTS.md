# Broader typing comparison: font paging stays out of this release

September 24, 2026. **Do not land the held font-page trial.** The earlier slower
Chinese signal does not establish a repeatable cross-language benefit. These
broader Taiwanese/Japanese runs add negative and inconclusive evidence, including
slower Japanese candidate/Space timing and substantial baseline session variation.
Accepted runtime remains `1ea175a`; no dictionary or app changes were shipped.

## Problem, cause, proposed change

The narrow eight-spelling replay used the same inputs in all four modes. It could
not settle the candidate-font paging tradeoff. We froze 720 source-attributed
language-specific queries without consulting candidate outputs. This screen uses
shard 0 for Taiwanese and Japanese: 56 queries per mode, separately labelled by
source, genre and full/partial/initial/mixed/error condition. Authored Taiwanese
examples are not spontaneous conversations; Japanese casual conversation and
service roleplay are separated in the detailed results. Existing corpora are
reused regression data, not fresh language-quality holdouts.

The runtime trial is unchanged: validate the Space default and first strip before
validating the rest of the font-filtered candidate list. Dictionary, ranking,
decoder, default policy and full expansion semantics remain unchanged. Only
classes.dex/classes3.dex differ between the app APKs outside signature entries.
Every replay uses the identical timing APK and embedded frozen corpus. The
test-only same-text Space observer fix applies equally to A and B.

## Results

Five completed sessions inject **4,258 actions: 3,698 letters and 560 Spaces**.
Exact mode/shard action-sequence validation passes for every run. All raw-editor
and Space frame submissions are observed. Candidate observations and deadlines
remain separate; missing candidates are never counted as zero latency.

Below, A is accepted runtime and B is the held trial. Each candidate/raw/Space
cell contains **p95 milliseconds**, the 95th percentile among observed frames.
Taiwanese has 447 letters and Japanese 254 per cadence, with 56 Spaces each.

| Run / key-release spacing | Candidate / raw / Space p95 ms | Candidate observed / letters | Submitted before next release / letters |
|---|---:|---:|---:|
| Taiwanese A1 / 150 ms | 80.19 / 21.19 / 27.94 | 441 / 447 | 435 / 447 |
| Taiwanese B / 150 ms | 80.95 / 20.46 / 29.40 | 442 / 447 | 437 / 447 |
| Taiwanese A2 / 150 ms | 101.98 / 24.46 / 32.92 | 429 / 447 | 426 / 447 |
| Taiwanese A1 / 60 ms | 74.46 / 21.33 / 34.96 | 396 / 447 | 344 / 447 |
| Taiwanese B / 60 ms | 70.43 / 21.85 / 34.47 | 384 / 447 | 341 / 447 |
| Taiwanese A2 / 60 ms | 74.55 / 29.36 / 82.00 | 338 / 447 | 280 / 447 |
| Japanese A / 150 ms | 71.89 / 22.70 / 30.05 | 254 / 254 | 254 / 254 |
| Japanese B / 150 ms | 88.35 / 25.53 / 31.85 | 254 / 254 | 254 / 254 |
| Japanese A / 60 ms | 71.19 / 21.54 / 27.25 | 254 / 254 | 222 / 254 |
| Japanese B / 60 ms | 86.77 / 30.00 / 35.79 | 221 / 254 | 161 / 254 |

The trial's lower Taiwanese fast conditional p95 accompanies fewer observed
candidates and fewer timely submissions than A1. A2 is materially slower without
any runtime change: especially Space p95, 34.96 to 82.00 ms. Therefore neither
the A2/B difference nor a pooled percentile establishes causal speedup. Japanese
candidate, raw and Space p95 are worse on B; the fast run loses 33 candidate
observations and 61 next-release deadlines. One Japanese pair cannot isolate the
cause, but it does not support shipping the additional mechanism.

All runs start thermal status 0 and battery below 34 C. Starting/ending battery
temperatures are A1 31.1/33.9, Taiwanese B 32.8/35.2, Japanese A 33.6/34.8,
Japanese B 33.7/34.4 and A2 33.7/34.9 C. Endpoint thermal status is 0 throughout;
cooldown polling after Taiwanese B briefly records status 1. CPU0 endpoint
frequency varies. These snapshots are not a continuous thermal/scheduling trace,
and the cooled-start rule does not eliminate session variability.

## Improvements and next step

The improvement in this loop is measurement reliability: source-specific input,
exact action validation, corrected same-text commit observation, explicit missing
frames and per-source/genre/condition results. It is not a claim that language
quality or app latency improved. Raw input meets the p95 target in these cells;
candidate/Space and broader release gates remain open. This is one shard in two
modes, not all 720 queries, the 10,000-actions-per-mode/three-session gate, human
touch accuracy, Chrome/Keep integration or display-presentation timing.

Stop expanding this font-page experiment for the current release. Preserve its
patch and evidence; keep accepted runtime. The independently declared next trial
targets the much larger measured cost of allocating the full expanded grid:
[expanded viewport plan](../EXPANDED-VIEWPORT-PLAN.md). It must preserve all
candidates and real scroll/selection reachability, without the font-page change.

`report-language-timing-trial.py` rebuilds results from completed guarded sessions,
validates input sequence and APK byte boundaries, and preserves raw samples,
aggregate provider work and public environment metadata in each run directory.
`comparison.json` contains machine-readable totals. Original APK/preferences/IME
were restored after every session, display OFF verified after environment reads,
and SHINE ownership explicitly released after the final client exited.
