# Phone validation and remaining gaps

RFCR91GWXLX, MinIME 0.8.5-review1 (33), September 12, 2026. SHINE explicitly
acknowledged the exclusive window and its extension. All operations used
`tools/phone-lease.ps1`; each session restores settings, learning preferences and
the prior Samsung IME, then verifies display OFF. No AOD or viewport changes.

## Google Zhuyin reference

Google 2.4.5.164561151 arm64, version code 2451413. The same frozen 18 cases ran
for both providers: 12 Chinese prose-derived full/initial/mixed/partial inputs and
six English words. The private test editor requests no personalized learning;
Google's proprietary historical state was not cleared. All 36 records were
observed, with screenshots immediately after entry and after a fixed one-second
delay. Candidate labels changed in zero of those delayed pairs for either IME.
These snapshots cannot rule out flicker between frames or during ongoing typing.

`phone/observations.json`, `comparison.json`, all screenshots and their hashes are
retained. Examples from that unchanged sample:

| Input | Google Space | Review Space | Interpretation |
|---|---|---|---|
| guzaowei | 古早味 | 古早味 | MinIME now visibly highlights 古早味 first; previously it was hidden behind prefix choices. |
| wanrenci | 萬人次 | 萬人次 | Stored whole phrase preserved. |
| zhongyangshebaodao | 中央社報導 | literal input | Null has no whole stored clause; 中央社 remains selectable. Coverage debt remains. |
| qzs | 請遵守 | 泉州市 | Stored-candidate ranking still differs; 請遵守 remains visible. No special promotion was added. |

All six English-word cases accepted their raw English spelling in MinIME's mixed
Pinyin mode, with a trailing space. This small sample does not establish general
English accuracy. Stored Chinese alternatives can still appear in mixed mode.

## Android integration

The first run passed seven editor tests, three native Rime tests, height stability
and overlapping-key input. Two old UI tests failed: automatic 我們明天見 assembly
is intentionally absent, and prefix 你 moved out of the collapsed nihao strip.
The updated tests retain the sentence target through explicit stored selections
and open the expanded list to select a prefix. Both passed on the next run.
The remaining assembly-dependent UI checks were also updated to preserve their
sentence targets through explicit selection. This is a deliberate contract change,
not a claim of unchanged whole-clause coverage or effort.

The final complete default Android integration suite passed **53 tests** in
286.834 seconds: seven editor, 43 visible keyboard and three native Rime tests.
`android/` retains the initial failures, successful follow-up and final logs,
native probe reports, and cleanup evidence. The final session was
`d8280cc4-8fc5-4d09-80c7-82d20b8aa427`; preferences, prior IME and display OFF
were verified before the explicit phone release.

## Injected input and frame timing

`touch/` contains all 464 raw samples and the per-mode report. Eight frozen queries
per mode, 150 ms and 60 ms key-start intervals, 25 ms down/up duration, center-key
OS injection. All 400 letter samples reached the expected editor text and an
editor frame callback; all 64 Space samples reached an editor frame callback.
This does not measure physical digitizer latency or natural human touch hit rate.

| Mode | Editor frame p95, 150 / 60 ms stream | Candidate frame p95 at 150 ms | Candidate frames observed, 150 / 60 ms |
|---|---|---|---|
| Chinese/English | 22.71 / 22.57 ms | 104.60 ms | 50/50; 0/50 |
| English | 31.06 / 31.09 ms | 28.54 ms | 50/50; 50/50 |
| Taiwanese/English | 23.58 / 22.44 ms | 77.73 ms | 49/50; 28/50 |
| Japanese/English | 20.89 / 22.24 ms | 83.34 ms | 49/50; 35/50 |

Endpoints run from injected key-up to frame-commit callback delivery, not physical
display presentation. Candidate timing is conditional on observing a current,
completed prediction before the next action; unchanged rows and supersession are
not distinguished. Missing observations are not zero latency or successful frames.
In particular, the rapid Chinese stream supplies no per-key candidate latency
estimate. Candidate responsiveness still needs work; this run does not certify
the performance acceptance requirements or establish a speedup over the old APK.
