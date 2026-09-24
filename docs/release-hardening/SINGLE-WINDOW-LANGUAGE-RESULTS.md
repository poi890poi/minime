# Broader replay confirms gains, with Chinese and Taiwanese tails still open

September 25, 2026. The admitted single-window runtime (`e8bc0cf`, publication
`2558304`) completes the [frozen eight-run comparison](SINGLE-WINDOW-LANGUAGE-PLAN.md).
No further runtime or dictionary change is made by this measurement. All eight
tests and exact action inventories pass; original APK, preferences, learning and
IME are restored and display OFF verified after every session. A separately
acknowledged diagnostic follows; these results do not claim public-release clearance.

## What changed and what this measures

The previous loop replaced the repeatedly resized composition popup with an
in-window raw-text chip, retaining its visible text, tap action and touch-through
geometry. Its causal trace and repeated small-screen admission are documented in
[the original result](SINGLE-WINDOW-RESULTS.md). This follow-up uses broader
language-specific inputs with the same unhooked test APK on both app builds;
all 24 language assets are identical. It tests whether the benefit extends beyond
the earlier eight-spelling screen.

There are 180 distinct queries, each run at 150 and 60 ms requested key-release
intervals on both builds: **5,384 injected actions**, comprising 4,648 letters and
736 Space actions. Every raw-text and Space submission is observed. Each build
contributes 2,692 actions. These are not vocabulary-accuracy or physical-touch
counts, and frame callbacks are not proof of screen presentation.

Each latency cell is **p95 / p99 milliseconds**: 95% / 99% of observed delays are
at or below those values. Candidate percentiles exclude unobserved frames, whose
denominators remain visible. “Before next release” means strictly before the next
key release, including the terminal Space; it does not mean the suggestion is correct.

| Mode / interval | Old candidate p95/p99 | New candidate p95/p99 | Observed, old → new / letters | Before next release, old → new / letters |
|---|---:|---:|---:|---:|
| Chinese / 150 ms | 60.75 / 82.82 | 45.43 / **81.29** | 332 → 332 / 332 | 332 → 332 / 332 |
| Chinese / 60 ms | 56.66 / 61.21 | 41.55 / 57.59 | 328 → 332 / 332 | 322 → 329 / 332 |
| Taiwanese / 150 ms | 60.92 / 77.42 | **52.17** / 79.67 | 446 → 447 / 447 | 446 → 447 / 447 |
| Taiwanese / 60 ms | 57.84 / 68.49 | 43.02 / 62.22 | 435 → 437 / 447 | 426 → 430 / 447 |
| Japanese / 150 ms | 54.89 / 57.36 | 37.26 / 38.68 | 254 → 254 / 254 | 254 → 254 / 254 |
| Japanese / 60 ms | 56.40 / 58.70 | 38.66 / 39.62 | 254 → 254 / 254 | 254 → 254 / 254 |
| English / 150 ms | 20.11 / 27.89 | 19.28 / 19.83 | 133 → 133 / 133 | 133 → 133 / 133 |
| English / 60 ms | 19.56 / 20.27 | 20.06 / 22.49 | 133 → 133 / 133 | 133 → 133 / 133 |

Bold cells still exceed the candidate budgets, p95 ≤50 ms and p99 ≤80 ms.
Chinese, Taiwanese and Japanese p95 improve at both cadences; English remains
near 19–20 ms. The aggregate Chinese ordinary-cadence p99 **does not pass**.
An interim conversational update mistakenly called all Chinese budgets passing;
it was corrected once p99 was inspected. This table is the complete result.

Raw-text and Space tails remain within their 33/50 ms p95/p99 budgets in all
aggregate cells of this sample:

| Mode / interval | New raw p95/p99 | New Space p95/p99 |
|---|---:|---:|
| Chinese / 150 ms | 14.37 / 18.85 | 18.09 / 30.12 |
| Chinese / 60 ms | 16.46 / 17.53 | 17.07 / 17.76 |
| Taiwanese / 150 ms | 14.53 / 17.36 | 13.74 / 15.14 |
| Taiwanese / 60 ms | 15.71 / 16.91 | 15.92 / 16.53 |
| Japanese / 150 ms | 14.31 / 14.87 | 14.97 / 16.59 |
| Japanese / 60 ms | 15.48 / 16.35 | 15.99 / 17.21 |
| English / 150 ms | 20.97 / 22.06 | 20.70 / 28.51 |
| English / 60 ms | 20.22 / 22.21 | 16.75 / 20.46 |

## Tradeoffs and remaining slow cases

Preserve the unfavorable changes: ordinary Taiwanese candidate p99 rises
77.42 → 79.67 ms; fast English candidate p95/p99 rises 19.56/20.27 → 20.06/22.49 ms.
Fast English raw p95/p99 also rises by 0.67/1.50 ms. Smaller increases occur in
ordinary Chinese Space p99 (+0.47 ms) and ordinary English raw/Space tails.
This single pair per language cannot distinguish those differences from session
variation; it is not grounds for claiming every metric improved.

Source breakdowns expose additional tails hidden by pooling. Ordinary Chinese
edited-chat p95/p99 is 49.63/84.38 ms, versus essay 41.74/69.46 ms. Ordinary
Taiwanese authored situational examples are 56.69/71.86 ms; historical authored
examples are 52.17/85.22 ms. Initial-only Chinese strata remain slow, with small
denominators. Japanese casual-chat and service-roleplay p95 stays about 37–39 ms
at both cadences. English source/cadence p95 stays about 19–21 ms, while tails
vary. Full source/genre/condition distributions remain in the aggregate files;
four Space observations per condition/cadence are too few for stable tail estimates.

There is one >100 ms observation: the old Chinese essay/transposition candidate
at action 315, two letters into its input, takes 101.34 ms. The same new-build
action takes 98.07 ms. That small difference does not resolve the underlying
slow case. No new-build raw/Space/candidate observation exceeds 100 ms in these
runs. Unhooked timing cannot attribute the cause; the
[separately frozen combined-stage diagnostic](POST-WINDOW-STAGES-PLAN.md) examines
the entire shard, including this case, without selecting or tuning its spelling.
The ten unobserved fast Taiwanese candidate frames also require classification;
they are not automatically touch misses or absent dictionary entries.

Candidate comparisons restricted to keystrokes observed in both builds remain
available alongside unrestricted observations, so omitted frames cannot silently
be scored as fast. All starts have thermal status 0. Battery temperatures in run
order are 31.2, 32.8, 33.9, 33.9, 33.8, 33.8, 33.9 and 33.8 °C. Cooling and
counterbalanced language order reduce some variation; they do not eliminate it.

## Evidence and next decision

[Comparison](single-window-language/comparison.json) contains paired aggregates,
genre/condition results, observed-in-both comparisons, outlier counterpart, source
hashes and cleanup receipts. Per-session reports retain mean, median, p95, p99,
maximum, missing counts and actual cadence. The
[reproduction notes](single-window-language/README.md) and binary pins keep raw
typing data and device dumps local. No new assertion count is presented as accuracy.

This is one previously exposed shard and one pair per language. Chinese chat is
edited excerpts; Taiwanese data is authored examples; Japanese has casual chat
and service roleplay, with no essay claim. English conversation/essay sources stay
separate. These runs do not satisfy ≥10,000 actions per mode across ≥3 sessions,
physical human hit rate, displayed-frame deadlines, new conversation holdouts,
Android 16/16 KB runtime, Play delivery or the unresolved source-rights decision.

Keep the admitted single-window implementation. Diagnose remaining queue/provider,
font/application and frame delay before proposing another production change.
Do not relax budgets, tune to a slow spelling, or revive rejected optimizations
without a separately declared causal experiment. Public release remains uncleared.
