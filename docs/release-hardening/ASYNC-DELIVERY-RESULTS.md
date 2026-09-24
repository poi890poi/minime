# Asynchronous delivery improves some candidates but costs Space response

September 25, 2026. **Reject and restore the accepted runtime.** The frozen
[delivery plan](ASYNC-DELIVERY-PLAN.md) rejects repeated raw/Space tail regressions.
Accepted production remains `6bffd17` / publication `e89a337`.

The trial marks only decoder-result Messages asynchronous on their existing
Handler/Looper. Scheduling stays at 8 ms; provider order, dictionaries and
generation/closed guards are unchanged. Four phone contracts pass, including
main-thread delivery, queued-result closure, cancellation and burst coalescing.
Core passes 1,770,165 assertions; pinned native output remains byte-identical
after 13,014 inputs. Assertions are contracts, not language accuracy.

## Repeated unhooked comparison

Execution order is A1, B1, B2, A2. A is accepted; B is the single-variable trial.
One unchanged instrumentation APK runs the same eight development queries in four
modes at requested 150/60 ms key-release intervals. Stage hooks are disabled.
All packages have identical manifests and 24 assets; only classes.dex differs
apart from signatures. They are non-debuggable, test-signed fixture APKs, never
Play/distribution packages. QueueTrace is absent from both app and test dex.

All four runs observe 400 raw-text and 64 Space submissions each: 1,856 injected
actions. Every cell has 50 letters and eight Space actions. This is not a physical
hit-rate or presented-frame measurement, and not the large-sample release gate.
The IME and test editor share a process; results do not establish Chrome/Keep
performance. Space p95/p99 equal the maximum of eight samples, a noisy tail estimate.

Each cell below is **raw-text p95 / Space p95, milliseconds**. p95 is the duration
within which 95% of observed updates completed. Columns show matched pairs;
execution order remains A1-B1-B2-A2.

| Mode / interval | A1 | B1 | A2 | B2 |
|---|---:|---:|---:|---:|
| chinese/150 | 17.89 / 27.44 | 18.43 / 28.03 | 19.43 / 28.00 | 17.59 / 27.05 |
| chinese/60 | 17.67 / 22.72 | 18.18 / 27.05 | 17.66 / 23.41 | 20.60 / 27.61 |
| english/150 | 20.17 / 12.65 | 20.20 / 14.51 | 20.64 / 13.89 | 19.48 / 13.07 |
| english/60 | 19.48 / 13.18 | 19.39 / 12.76 | 18.84 / 14.36 | 19.95 / 14.30 |
| taiwanese_english/150 | 19.19 / 21.75 | 20.06 / 26.69 | 21.41 / 23.93 | 19.44 / 27.73 |
| taiwanese_english/60 | 18.40 / 21.57 | 17.04 / 24.16 | 18.98 / 20.74 | 17.38 / 26.62 |
| japanese_english/150 | 17.27 / 27.92 | 18.45 / 23.80 | 17.99 / 25.54 | 17.33 / 23.83 |
| japanese_english/60 | 17.73 / 25.11 | 17.83 / 27.46 | 17.76 / 21.12 | 17.39 / 23.07 |

Fast Chinese raw p95 worsens by 0.51/2.95 ms and Space by 4.33/4.20 ms in the
two pairs. Taiwanese Space worsens by 4.93/3.80 ms at 150 ms and 2.59/5.87 ms at
60 ms. Fast Japanese Space also worsens by 2.35/1.96 ms. The trial's aggregate
raw/Space p95/p99 still meets the absolute 33/50 ms budgets in this small sample;
rejection follows the separate predeclared repeated-regression rule. A2 accepted
Chinese at 150 ms has a 51.27 ms raw p99 outlier, retained in the aggregate.
Session variability limits causal certainty; two small paired differences are
not a statistical proof of population harm.

Each next cell is **candidate p95 in ms; frames before the next key release / all
50 letters**. Missing and late results stay in the denominator.

| Mode / interval | A1 | B1 | A2 | B2 |
|---|---:|---:|---:|---:|
| chinese/150 | 76.85; 50/50 | 68.78; 50/50 | 86.14; 50/50 | 71.92; 50/50 |
| chinese/60 | 59.83; 50/50 | 57.68; 50/50 | 54.68; 49/50 | 55.81; 48/50 |
| english/150 | 19.49; 50/50 | 19.25; 50/50 | 19.58; 50/50 | 19.28; 50/50 |
| english/60 | 19.47; 50/50 | 19.43; 50/50 | 19.08; 50/50 | 19.75; 50/50 |
| taiwanese_english/150 | 57.61; 49/50 | 61.70; 50/50 | 60.54; 49/50 | 55.06; 49/50 |
| taiwanese_english/60 | 49.42; 50/50 | 49.70; 50/50 | 55.75; 47/50 | 52.78; 49/50 |
| japanese_english/150 | 49.30; 49/50 | 48.85; 49/50 | 54.03; 49/50 | 53.84; 50/50 |
| japanese_english/60 | 51.02; 49/50 | 46.45; 50/50 | 54.78; 49/50 | 54.76; 50/50 |

Slow Chinese candidate p95 improves in both pairs; fast Chinese does not. Average
candidate response improves in several mixed-mode cells, but does not excuse
the repeated acceptance-tail costs. Candidate results are conditional on observed
fresh frames, with missing counts preserved separately in the aggregates.

## Decision and next causal question

Restore both active trial source files and retain the exact proposal/tests under
`async-delivery/` outside Android source sets. Do not ship the new message flag or
claim that it proves synchronization barriers caused the measured queue delay.
The proposed instrumented causal follow-up and broader admission replays were
not run after the screen failed. The existing queue diagnosis remains valid;
this shortcut did not pass its admission conditions.

The next investigation should distinguish main-thread work from barrier waiting
and relate candidate application to raw/Space frames. Prioritize reducing work
that blocks input or rendering rather than changing result priority again.
Record the uncertainty in short-tail screening; do not silently reinterpret this
failed experiment as accepted or adjust its thresholds after seeing results.

Phone sessions ran under an explicitly acknowledged lease, with status-0 cooled
starts below 34 C. Every session restored original APK/preferences/learning and
Samsung IME; final display OFF was verified and ownership explicitly released.
`async-delivery/` retains aggregate distributions, comparison code, package hashes
and session/cleanup hashes. Raw editor samples and private backups stay local.
All larger quality, source-rights, human-touch, Android 16/16 KB and Play-delivery
release gates remain open.
