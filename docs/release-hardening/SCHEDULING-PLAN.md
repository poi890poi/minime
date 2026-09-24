# Decoder scheduling experiment

September 24, 2026. Type: performance experiment. Baseline b47bb2a retains an
8 ms delay before each asynchronous lookup. Trial changes only that delay to
zero; the single worker, queued cancellation, provider order, request revisions
and main-thread stale-delivery guard remain unchanged. This is intentional
scheduling behavior, not a claimed correctness defect.

Hypothesis: removing the fixed wait gives current candidates more time to reach
the screen before the next input. Risk: very short bursts start more obsolete
provider work, increasing CPU use or delaying subsequent work. No data, ranking,
learning, acceptance, editor or privacy rule may change. Do not add word-specific
exceptions, cache cross-query results or use benchmark labels in production.

First run shared-core and pinned desktop verification. Use the previous loop's
corrected targets and next-release observation boundaries. Compare the same
frozen eight development queries, four modes and 150/60 ms intervals on both
APKs; repeat surviving trials in reverse order. Keep raw data, actual intervals,
missing observations, raw editor timing and conditional candidate timing.
Require repeated responsiveness gains without lost input, stale acceptance or
material extra work at the ordinary typing pace. Inspect burst work separately;
do not label an increase acceptable just because idle-query latency improves.

Read existing aggregate provider/request counters only after each replay so
resource reporting adds no hot-path instrumentation. Run a separate stage-hook
baseline/trial to distinguish request-to-delivery from callback/font/render cost.
Absolute stage-hook times are diagnostic; unhooked replays decide visible gains.
Existing cancellation and close tests remain mandatory. A controlled real-data
burst test reports provider calls and work under 0/4/12 ms dispatch gaps while
the main thread withholds callbacks; this is a stress case, not a human-typing
or battery measurement. Both binaries must suppress all superseded callbacks.

Input corpus and behavior checks are already-used development data, not fresh
conversation/essay holdouts or vocabulary accuracy. Existing 50 ms candidate
p95, raw-frame and physical-touch release gates stay unchanged. Preserve
negative results and reject the runtime trial if benefits do not reproduce.
Phone operations require a fresh explicit SHINE handoff plus shared mutex;
restore APK/preferences/IME, verify display OFF and explicitly release.

## Prepared, device validation pending

Shared core passes 888,180 assertions. Pinned desktop passes 13,014 inputs and
11,272 native queries with output SHA-256 identical to the preceding loop.
Baseline, trial and test APKs build locally; only classes3.dex differs between
the app APKs outside signature metadata. [Exact identities](schedule-binaries.json).
The one-line [trial patch](pending-zero-delay.patch) is archived, and production
retains its original 8 ms delay until phone evidence supports a change.

Automatic approval review rejected the phone command twice before execution:
it treated SHINE's explicit reservation acknowledgement as untrusted tool
output and would not accept it as the required coordination evidence. A fresh
acknowledgement did not resolve the rejection. User approval to accept that
handoff and run the prepared comparison was requested. No phone command ran in
this window. The new Android tests compile but have not run; no timing,
resource, cancellation or performance improvement is claimed for the trial.
