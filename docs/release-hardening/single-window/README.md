# Single-window composition trial

The trial keeps the raw spelling chip and its exact-input action but removes its
separate PopupWindow. Only KeyboardView and MiniMeService change in the app.
`KeyboardView.patch` and `MiniMeService.patch` preserve the runtime delta.
The [frozen plan](../SINGLE-WINDOW-PLAN.md) defines admission; a diagnostic ablation
is not itself a shipping improvement.

Reproduce from the pre-trial runtime with the geometry-test correction (`d7192ef`
in the local engineering branch or `8424196` in the publication branch). Run
`../system-queue/prepare-single-window.py` to create an ignored overlay. Its
anchors deliberately fail after the replacement has been applied. The generated
fixture init adds only the test editor to a non-debuggable release payload.
No trace hooks, profileable flag, Internet permission or upload credentials.
The archived CompositionSurfaceTest stays outside Android source sets until
the runtime is admitted. It verifies actual OS touch routing and screenshot
pixels as well as raw tap and keyboard/editor geometry.

Use the same final test APK for every timing comparison. `run-contracts.ps1`
records rotation settings before instrumentation and restores/verifies them in
an outer finally block, including test failure/timeout. Test-level restoration
also restores both settings explicitly. UiAutomation ROTATION_UNFREEZE alone
enables auto-rotation and is not restoration of a locked phone; the first rotation
attempt exposed this harness defect. It was repaired immediately before further
device work. Retain that failed attempt alongside the corrected pass.

`run-timing.ps1` executes A-B-B-A with the established cool-start requirements,
17-minute per-run headroom and normal APK/preferences/IME/display restoration.
Both scripts require an explicit acknowledged phone window and use the shared
mutex. `report.py` requires all four successful sessions, exact paired workload
identity and 464 actions per run. It reports all missing frames and paired tails.
Only compact aggregates and evidence hashes are published; raw typing data and
screenshots remain local.
