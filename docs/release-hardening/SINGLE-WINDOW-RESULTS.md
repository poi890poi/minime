# A single IME window improves repeated candidate timing

September 25, 2026. Decision: admit the single-window composition buffer after
the frozen repeated screen and final app checks. Public release is still not cleared.

**Problem and cause.** The floating raw-spelling buffer resized a PopupWindow on
each width change. The [isolated ablation](ANNOTATION-ABLATION-RESULTS.md) removed
729 relayouts and most queued-result waiting, but also removed the raw-text
affordance. The replacement keeps that affordance inside the existing IME window.
It adds a fixed transparent host and exact touch region, preserving the keyboard
body and editor resize boundary. No dictionary, ranking, acceptance or decoder
scheduling change. All 24 packaged language assets match the accepted build.
The normal release source-set build also passes after source transfer. Its
package contains the replacement and identical assets, with no test editor,
trace observer, profileable flag, debuggable flag or Internet permission.
The [unsigned packaging check](single-window/ordinary-package.json) is not a
final distribution package and retains version 0.8.8/code 38.

**Repeated result.** Same test APK and fixed development inputs, A-B-B-A order,
cooled starts, 150/60 ms injected key intervals. Each run has 464 actions: 400
letters and 64 Space actions. Every raw/Space frame is observed in all four runs
(1,856 total). Candidate-frame observations are 396/400 and 397/400 in baselines,
400/400 in each trial. These counts describe frame observations, not language
accuracy, physical hit rate, or actual display presentation.

| Mode | Key interval | Baseline candidate p95, A1 / A2 | Trial candidate p95, B1 / B2 |
|---|---:|---:|---:|
| Chinese | 150 ms | 75.98 / 76.60 ms | 70.78 / 68.41 ms |
| Chinese | 60 ms | 61.11 / 58.20 ms | 41.77 / 39.19 ms |
| English | 150 ms | 19.10 / 19.74 ms | 19.73 / 19.61 ms |
| English | 60 ms | 19.24 / 19.80 ms | 19.23 / 19.33 ms |
| Taiwanese + English | 150 ms | 53.37 / 55.57 ms | 37.72 / 37.52 ms |
| Taiwanese + English | 60 ms | 51.51 / 52.94 ms | 39.01 / 39.84 ms |
| Japanese + English | 150 ms | 55.16 / 57.02 ms | 37.09 / 36.77 ms |
| Japanese + English | 60 ms | 55.26 / 56.11 ms | 39.03 / 39.54 ms |

p95 means 95% of observed responses arrive at or below that delay. Each cell has
50 letter events; each corresponding Space cell has only eight events, so its
p95/p99 is the maximum and is not a stable population-tail estimate. Candidate
p95 gains repeat in all Chinese/Taiwanese/Japanese cadence cells. English is broadly
unchanged. No repeated raw/Space p95/p99 regression exceeds the predeclared 2 ms
screen limit. All trial raw p95 values are below 33 ms; maximum raw p99 is 35.66 ms
and maximum Space p99 is 26.24 ms, below the 50 ms screen budget.

Do not hide contrary observations: Chinese slow raw p99 is 23.09 -> 35.66 ms in
the first pair and 24.65 -> 24.61 ms in the second. English slow raw p95 increases
about 0.8/0.9 ms, and its candidate p99 is 20.15 -> 26.71 ms in one pair but
20.20 -> 20.13 ms in the other;
Japanese fast Space p95 worsens 4.68 ms in the first pair but improves 4.89 ms in
the second; Chinese slow candidate p99 improves in one pair and worsens 0.45 ms
in the other. Chinese slow candidate p95 remains 68–71 ms, over its 50 ms target,
and one trial's slow candidate p99 is 87.22 ms, over the 80 ms target. No shipping
latency certification or large-sample claim follows from this screen.

**Functional evidence.** Fourteen strengthened portrait/lifecycle/four-mode
checks pass, as do two landscape/recreation/height checks. Exact raw tapping,
stable key/body geometry, unchanged editor IME inset, visible transparency, and
OS touch-through beside the chip are checked. The same corrected keyboard-height
test passes on the unchanged accepted APK. [Portrait](single-window/portrait.json)
and [landscape](single-window/landscape.json) geometry are recorded.

All seven final app checks pass: Chrome omnibox conversion without submitting a
search; Keep title/body input in all four modes; English fast switching, mixed
mode composition/geometry, paired-mode return behavior, category swiping and
source-derived Japanese character selection. The synthetic Keep fields are
cleared afterward. This does not certify every Chrome/Keep field or version.

Two harness defects were retained and corrected. The old height helper measured
the accessibility touch envelope instead of the keyboard body; it now measures
the attached KeyboardView, retaining key/panel/mode assertions. UiAutomation
ROTATION_UNFREEZE enabled auto-rotation during the first landscape test; the
original locked-portrait state was repaired immediately, and both test-level and
outer-runner cleanup now restore explicit recorded rotation settings. The rerun
passes with restoration and display OFF verified. [All attempts](single-window/contracts.json).

The phone was restored and explicitly released after the four timing runs and
again after final app checks; rotation settings and display OFF were verified.
[Full paired aggregates](single-window/comparison.json) and
[package identities](single-window/binaries.json) retain the evidence. Raw typing
telemetry and screenshots stay local. The next performance gate uses the broader
frozen language-specific shards; this short screen uses the same eight mixed
development queries in every mode and cannot establish Taiwanese/Japanese
conversation coverage. Human touch, Android 16/16 KB, Play delivery and source
rights remain separate release gates.
