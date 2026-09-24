# Incremental expanded grid: opening improves, scrolling fails admission

September 24, 2026. **Reject the first full-refresh prototype.** Accepted runtime
remains `1ea175a`. The archived patch is an experiment, not a shipped change.
[Predeclared plan](../EXPANDED-VIEWPORT-PLAN.md).

## Problem and change

The expanded grid allocates and measures every candidate TextView on opening.
The trial initially allocates two conservative viewports, then appends a batch
near the bottom. All engine candidates, ordering, font checks and Space defaults
stay unchanged. There is no dictionary or core change, and the separate rejected
font-paging trial is absent. Only classes.dex/classes3.dex differ in the app ZIP
outside signatures; the three additional classes are KeyboardView lambdas.

## Result

The identical detached diagnostic APK evaluates 46 frozen prefixes twice.
All 92 full candidate/default signatures match between baseline and trial.
Opening measure/layout/bitmap-draw p95 improves from **356.02 / 360.43 ms** to
**42.30 / 37.64 ms** on first/repeated passes. These costs exclude real touch
and GPU frame submission. Other stages differ materially between these sessions;
the baseline ends at thermal status 1. Do not infer a general typing speedup.

The same attached-view test APK then runs 12 systematic prefixes (every fourth
of the original 46), two passes, in both orientations. Every episode reaches
and selects the visible final candidate. All lists/defaults match the independent
first-pass detached signatures. This harness uses Learning.NONE; the detached
harness learns from selection, so its second pass is not an ordering reference
for these independent episodes. The initial report assertion caught that mismatch;
the corrected comparison retains the first-pass reference for both frame passes.

The table gives **p95 milliseconds from the programmatic action to the first
subsequent submitted GPU frame**. This is not physical touch or panel presentation.

| Orientation / action | Baseline | Trial | Actions per build |
|---|---:|---:|---:|
| Portrait / open | 438.95 | 45.72 | 24 |
| Portrait / scroll | 16.32 | 83.07 | 344 |
| Portrait / select | 63.46 | 64.44 | 24 |
| Landscape / open | 429.95 | 49.36 | 24 |
| Landscape / scroll | 18.79 | 85.08 | 254 |
| Landscape / select | 67.99 | 70.27 | 24 |

Paging occurs on 76 portrait and 58 landscape scroll steps. Those steps have
submission p95 **93.46 / 102.54 ms**, maximum **98.80 / 107.94 ms**. Other trial
scrolls remain at 18.14 / 19.34 ms p95. The slowdown is concentrated in append
steps, not scrolling without new allocation. Endpoint thermal status is 0 in
both attached runs, but continuous temperature/frequency is not measured.
One pair is enough to reject the observed stall; no repeated speedup claim.

The traced append path calls the full render, changes the strip presentation key,
rebuilds the mode toolbar, and reconfigures every already allocated TextView.
The next experiment must isolate append work from unchanged view configuration;
this trace is a hypothesis for the excess work, not yet a measured component split.

## Integration and evidence limitations

The final integration run passes 20 checks: tail reachability, identity, reset,
collapse/reopen, paired holds, pending/stale selection, and existing stability
contracts. Two earlier runs are preserved: generated Han strings wrongly contained
digits, then the Taiwanese fixture omitted its enabled pack. Both were test setup
defects; the app APK was unchanged across all three runs. A host replay of the
exact corrected fixture produces all 256 paired choices through the shared core.
No synthetic fixture is a language-accuracy result.

The paging-specific tests and prototype are in `rejected-full-refresh.patch`.
The attached-frame harness works on both accepted and experimental runtimes.
`report-expanded-viewport.py` reproduces manifests, summaries and compressed raw
samples from guarded session artifacts. Each run restores the original APK,
preferences and IME, verifies display OFF, and releases the shared phone window.
Private preferences and full device dumps remain local. No hosted CI was used.

Broad typing nonregression and a second comparison pair were not run for this
rejected implementation. Opening gains cannot excuse the new scrolling stalls.
