# Phone touch response diagnostic — 0.7.6

Typed text responds much sooner than Chinese suggestions in this diagnostic.
This measures OS-injected input through the actual visible IME and editor rendering;
it does not certify physical touch-to-photon latency or section 28 acceptance.

Phone: RFCR91GWXLX, SM-G781B, Android 13, build G781BXXSIHYJ1. Production baseline
5bcb90f / 0.7.6, before the POJ dictionary repair. Rime enabled; POJ and Japanese
packs enabled; geography and Taiwan entity packs disabled; clean test settings.
EditorTestActivity uses the same process as the IME. These results do not cover
Chrome, cold loading, other devices, real fingers or imprecise target geometry.

The corrected run completed 400 letter actions and 64 Space actions in 118.733 s.
Each configured mode/pace has 50 letter actions and 8 Spaces. Query selection is
every 144th row of the previously evaluated latency corpus, filtered to 3–16 ASCII
letters, taking the first eight. Inputs are identical across modes. This small,
cross-language diagnostic sample is not a per-language conversational benchmark.

| Mode | DOWN interval | Raw frame callback p95 | Fresh candidate frame callback p95 | Observed candidate frames |
| --- | ---: | ---: | ---: | ---: |
| Chinese/English | 150 ms | 20.79 ms | 104.34 ms | 50/50 |
| Chinese/English | 60 ms | 21.92 ms | unavailable | 0/50 |
| English | 150 ms | 30.94 ms | 28.24 ms | 50/50 |
| English | 60 ms | 27.62 ms | 27.59 ms | 50/50 |
| Taiwanese/English | 150 ms | 21.57 ms | 86.74 ms | 29/50 |
| Taiwanese/English | 60 ms | 23.13 ms | 55.21 ms | 9/50 |
| Japanese/English | 150 ms | 21.52 ms | 82.08 ms | 29/50 |
| Japanese/English | 60 ms | 20.03 ms | 73.33 ms | 21/50 |

Origin is the injected UP event timestamp, with approximately 1 ms timestamp
resolution. End is main-thread delivery of the frame-commit callback. The callback
includes frame submission and callback scheduling; it is not the compositor's
presentation timestamp. [Android's API contract](https://developer.android.com/reference/android/view/ViewTreeObserver#registerFrameCommitCallback(java.lang.Runnable))
explicitly distinguishes rendering/submission from display presentation.

Candidate percentiles are conditional on observing a frame with the current raw
query and `predictionPending == false` before the next action. Unchanged candidate
content can require no new frame, and newer actions can supersede pending results.
The harness cannot distinguish these causes in its unobserved bucket. Never treat
missing frames as zero latency, successful fresh feedback, or proven dropped input.
The Chinese 150 ms condition has complete observations: median 62.62 ms, p95
104.34 ms, maximum 155.21 ms. That supports investigation of suggestions independently
of touch dispatch; it does not yet isolate worker time from queueing and rendering.

All 400 letter checkpoints reached the editor with the expected complete text.
Editor callback p95 was 5.56–6.09 ms across groups. One rapid Chinese key took
76.62 ms to its frame callback. All 64 Spaces reached a submitted editor frame;
group p95 ranged 14.77–40.00 ms. DOWN-to-pressed-frame p95 ranged 30.44–48.13 ms,
with two unobserved pressed frames during rapid Chinese input. Dwell was 25 ms.
These injected center taps cannot establish real-human hit rate or digitizer loss.

Decision: no runtime timing patch from this run. Raw feedback is usually quick,
but suggestion timing needs improvement and a trace separating worker backlog,
computation, applied state and display presentation. Performance acceptance remains
NOT CERTIFIED: the sample is small and physical/display endpoints are unmeasured.

## Rejected measurement

The initial observer registered frame callbacks in OnDraw, after Android had
captured callbacks for that frame. It sometimes reported the following frame and
produced raw latency that tracked the typing interval (about 164 ms at 150 ms pace,
82 ms at 60 ms pace). Those numbers are rejected. Registration now occurs in
OnPreDraw, before capture. The ordering is visible in Android 13's
[ViewRootImpl.performDraw](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-13.0.0_r1/core/java/android/view/ViewRootImpl.java).
No product timing change was made between the two runs. The corrected raw frame
measurements no longer follow the inter-key interval. Both raw reports are retained.

Reproduce with `TouchLatencyTest` through `tools/test-device.ps1`, then pull
`/sdcard/Android/data/dev.minime.ime/files/touch-latency.tsv`. Run
`tools/report_touch_latency.py` to reproduce `summary.json`. The test observes
existing views; no instrumentation ships in the release app. Both sessions restored
Honeyboard and settings/learning XML, verified restoration by readback, and verified
`mScreenState=OFF` and `mActualState=OFF`. No always-on-display settings changed.
