# Compiled validators: isolated saving, no accepted typing improvement

September 25, 2026. **Reject the runtime trial under its frozen gate.** Accepted
runtime remains `6bffd17` (publication `e89a337`). Production and active test files
are restored exactly. The [plan](VALIDATION-PATTERN-PLAN.md) required rejection
for repeated raw/Space tail regressions in any mode. This is a conservative
admission decision, not proof that compiled Patterns intrinsically slow typing.

## Problem, cause and intervention

The refreshed application profile found repeated compilation of fixed validation
expressions. The trial shares five immutable Patterns, with a fresh Matcher for
each call, retaining the original expressions, evaluation order and full-string
matching. No dictionary, ranking, learning, font, scheduling or UI change.

The complete core passes 1,770,165 assertions; a separate old-expression oracle
passes 89,160 boundary/concurrent comparisons. Those counts measure contracts,
not language accuracy. Pinned desktop output is byte-identical to the accepted
runtime: SHA-256 `1d2aa3693ae0ddbaf1c259252fc4d9689fcd458dfb23a214f9ed29c476fcde5d`.

On ART, ten alternating batches of 11,145 predicates per implementation have
median cost **107.822 ms old versus 25.042 ms compiled**. Three mechanism/profile
tests pass. Regex compilation is absent from the sampled trial leaves; that
does not prove absence from every execution path. The untraced application
callback median is 0.646 ms, p95 4.028 ms over 138 callbacks. Its earlier baseline
p95 was 3.691 ms in another session; no end-to-end speedup follows from either
the isolated predicate saving or cross-session callback medians.

## Matched phone comparison

One acknowledged lease covers cooled A1/B1/B2/A2 starts on RFCR91GWXLX. A uses
the accepted String.matches call sites; B uses the compiled validators. Both are
non-debuggable release-mode packages with the same test-only editor fixture,
manifest, 24 language assets and test APK. Only classes.dex differs outside
signature metadata. This fixture is absent from ordinary production releases.

Eight reused development queries, four modes, 150/60 ms requested key-release
spacing, no stage hooks. Every run records 400/400 raw-editor and 64/64 Space
submissions: **1,856 injected actions total**. These are not physical touch hits
or measured screen presentation. Fifty letters and eight Space actions per cell
are too few to certify a population tail; Space p95 is the sample maximum.

Each cell below is **raw-text p95 / Space p95, milliseconds**. p95 is the latency
at or below which 95% of that cell's observed actions fall. Columns are paired
for comparison; the actual execution order is A1, B1, B2, A2.

| Mode / interval | A1 | B1 | A2 | B2 |
|---|---:|---:|---:|---:|
| Chinese / 150 ms | 21.01 / 26.09 | 21.18 / 27.33 | 19.26 / 29.34 | 19.25 / 28.35 |
| Chinese / 60 ms | 19.55 / 24.09 | 16.52 / 22.47 | 19.61 / 24.15 | 19.51 / 24.01 |
| English / 150 ms | 21.75 / 13.49 | 20.48 / 13.56 | 20.61 / 12.82 | 19.53 / 13.23 |
| English / 60 ms | 20.09 / 13.03 | 20.07 / 12.73 | 20.09 / 12.73 | 20.50 / 13.73 |
| Taiwanese + English / 150 ms | 17.38 / 23.26 | 17.63 / 25.59 | 17.32 / 23.44 | 19.89 / 25.86 |
| Taiwanese + English / 60 ms | 16.42 / 21.28 | 17.27 / 22.94 | 16.56 / 19.48 | 17.87 / 22.32 |
| Japanese + English / 150 ms | 16.82 / 22.74 | 17.13 / 25.00 | 17.68 / 27.03 | 17.70 / 20.85 |
| Japanese + English / 60 ms | 18.23 / 25.68 | 19.09 / 29.49 | 19.12 / 21.84 | 22.51 / 22.05 |

Taiwanese fast raw p95 rises 0.84/1.31 ms and Space p95 rises 1.66/2.84 ms in the
two pairs. Fast Japanese raw p95 rises 0.86/3.39 ms and p99 rises 6.79/15.73 ms.
Chinese fast raw/Space timing improves, but does not excuse cross-language costs
under the predeclared gate. Device/run variability remains a causal limitation.

Each following cell is **candidate p95 in ms; submissions strictly before the
next key release / all 50 letters**. Percentiles condition on observed frames;
missing or late submissions remain in the denominator, never fast successes.

| Mode / interval | A1 | B1 | A2 | B2 |
|---|---:|---:|---:|---:|
| Chinese / 150 ms | 86.13; 50/50 | 77.43; 50/50 | 71.12; 50/50 | 77.58; 50/50 |
| Chinese / 60 ms | 59.35; 49/50 | 55.21; 50/50 | 60.29; 49/50 | 59.57; 47/50 |
| English / 150 ms | 20.04; 50/50 | 19.07; 50/50 | 19.51; 50/50 | 18.84; 50/50 |
| English / 60 ms | 19.41; 50/50 | 18.98; 50/50 | 19.21; 50/50 | 19.61; 50/50 |
| Taiwanese + English / 150 ms | 54.47; 49/50 | 51.40; 49/50 | 59.64; 49/50 | 59.44; 49/50 |
| Taiwanese + English / 60 ms | 54.57; 49/50 | 51.03; 49/50 | 56.66; 49/50 | 55.76; 49/50 |
| Japanese + English / 150 ms | 52.86; 49/50 | 55.24; 49/50 | 54.30; 49/50 | 54.44; 49/50 |
| Japanese + English / 60 ms | 54.62; 48/50 | 54.81; 48/50 | 55.25; 50/50 | 47.80; 47/50 |

The favorable final Japanese candidate percentile accompanies fewer observed
frames (50 to 48), so it cannot be presented as an unconditional improvement.
Fast Chinese timely submissions total 98/100 baseline and 97/100 trial. Taiwanese
totals are 98/100 for both. The harness does not distinguish unchanged rows from
lateness or supersession; no lost dictionary coverage is inferred from missing
frames. Reports retain mean, median, p95, p99, maxima and actual input intervals.

## Disposition and evidence

- Aggregate reports: [A1](validation-pattern/a1.json), [B1](validation-pattern/b1.json),
  [B2](validation-pattern/b2.json), [A2](validation-pattern/a2.json).
- [Isolated mechanism](validation-pattern/mechanism-summary.json),
  [binary identities](validation-pattern/binaries.json),
  [session receipt](validation-pattern/sessions.json).
- The runtime patch and added classes are archived under `validation-pattern/`;
  they are outside active source sets. Raw device and corpus telemetry stays local.
- All clients exited. Original APK, preferences, learning and Samsung IME were
  restored; actual display OFF was verified before explicit release to SHINE.
- No final package, Play upload or GitHub CI. This negative result changes no
  release threshold and clears no quality, rights, platform or human-touch gate.
