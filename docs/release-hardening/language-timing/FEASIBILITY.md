# First language-specific timing shard

September 24, 2026. The frozen Taiwanese shard 0 is operational, not release
clearance or a speedup. Accepted runtime `1ea175a`, harness `393ee56`; no app or
dictionary changes. Session `b0dadba8-66a0-4a57-87ee-8fbd01fd81e4` passes its
instrumentation run in 173.582 seconds. Offline validation confirms the exact
source-labelled action sequence: 56 queries at each of two cadences, 447 letters
and 56 Space actions per cadence, 1,006 actions total. No query was dropped.

| Key release spacing | Raw p95 / p99 ms | Space p95 / p99 ms | Candidate p95 / p99 ms | Candidate observations | Before next release |
|---|---:|---:|---:|---:|---:|
| 150 ms | 21.19 / 26.34 | 27.94 / 47.65 | 80.19 / 154.87 | 441 / 447 | 435 / 447 |
| 60 ms | 21.33 / 27.64 | 34.96 / 82.12 | 74.46 / 82.37 | 396 / 447 | 344 / 447 |

All 894 raw-editor and 112 Space submitted frames are observed. Ten Space text
callbacks are unobserved; they remain missing and are not replaced with engine
timestamps. The corrected Space frame predicate allows ended composition with
unchanged characters once the engine releases its spelling. Host regression
proves that transition; this run does not record the committed text, so it does
not independently attribute each missing text callback to same-text acceptance.

p95/p99 refer to the 95th/99th percentile of observed latency, measured from
injected key release to Android frame callback delivery. They are not physical
touch-to-photon latency. Missing candidate frames remain in denominators; a lower
conditional p95 at fast pace does not establish faster computation. Each
source/condition cell has only four Space actions. The input sources are authored
situational/historical Taiwanese examples, not spontaneous conversation. Full
source/genre/condition results and raw timestamps are in `tw-smoke-a/`.

Thermal status remains 0; battery temperature rises from 31.1 to 33.9 C. Original
APK/preferences/IME are restored, display OFF verified after environment reads,
and the SHINE reservation explicitly released. Public metadata is in
`tw-smoke-a/manifest.json`; private preferences and full system dumps stay local.

Next comparison is predeclared: unchanged held font-page APK, Taiwanese shard 0
B followed by accepted Japanese shard 0 A, Japanese shard 0 B and Taiwanese shard
0 A. Each starts thermal status 0 and battery below 34 C, with a ten-minute cooling
limit. Keep all outcomes, including raw/Space regressions and candidate misses;
do not tune the trial. This screens two language-specific workloads; it does not
complete the 720-query corpus or the 10,000-actions-per-mode/three-session gate.
