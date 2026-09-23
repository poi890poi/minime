# Candidate application cost: September 24 contract

Performance work; baseline runtime 995b535, evidence HEAD 0b2c754. The previous
phone profile attributes 7.09 ms p95 to learning reads and 21.47 ms p95 to remaining
application work, excluding font checks and render callbacks. These independent
percentiles are not additive. The diagnostic step precedes the trial below.

Use the same frozen development prefixes to sample the real core application
callback on the phone, with Android LocalLearning but without font/UI work.
Provider output is computed separately. ART sampling is diagnostic and perturbs
execution; its timings cannot certify typing latency. Capture a separate English
completion call for comparison. Trace only controlled test input. The parser's
format reference is the AOSP dmtracedump implementation:
https://android.googlesource.com/platform/art/+/002aca7141e7baf488b02f330b307f059c47a84d/tools/dmtracedump/tracedump.cc

Any optimization must preserve candidate identities, order, scores, consumption,
paired forms, Space acceptance, learning updates and privacy. No language data or
per-word weights may change. First verify equivalence on the shared core and
pinned desktop evaluator, then compare the corrected four-mode phone replay.
Retain only repeated end-to-end gains without material regressions; preserve
negative results. Keep test tooling and production slices in independent commits.

## Diagnosed trial

The diagnostic trace assigns 2,453,491 of 4,437,136 sampled application microseconds
to sorting (55.3% of the sampled application time, not 55.3% of typing latency).
Repeated context-key, span and comparator-key extraction appears within that
path. English completion is not the main cost for the large non-English prefixes.

Trial: compute partial-match and learned-vote keys once per candidate, and the
context key once per result list, then use a stable primitive-key comparator.
Keep the old comparator as an independent equivalence oracle. This adds temporary
key objects but removes repeated store reads and key construction; quantify on
the phone rather than assuming an allocation or latency gain. Keys must not be
cached across queries, learning updates, editors or privacy changes.

The comparator equivalence oracle uses generated lists, including exact ties,
numeric edge cases and refreshed votes. The desktop evaluator replays the same
frozen inputs before and after the trial. Neither is a fresh language holdout.
The runtime may use only existing candidates, composition state and permitted
local learning; profiler output and test labels never feed ranking.

Phone comparisons use `TouchLatencyTest#testTouchWithoutStageHooks` explicitly,
with the same test APK for both app APKs. Run A/B and then B/A to check session
order effects. The stage-hook method remains available for diagnosis; selecting
the whole class runs both methods. Preserve missing observations, separate all
four modes and both input intervals, and report raw editor and candidate timing.
These are short development replays, not physical touch or release acceptance
certification. Keep existing release thresholds unchanged.

Tooling verification: five independent trace-parser contract tests pass; the
baseline sampling test passes on the authorized phone. Its runner restores the
original APK, preferences and IME and verifies the display is off. The parser
rejects overflow, malformed records, invalid exits and backward clocks. The
raw trace and application-cost rows contain controlled test inputs only.

## Measurement correction after A1/B1/B2/A2

The four unhooked replays reproduce the slower-paced Chinese improvement but
give a contradictory fast tail: candidate p95 worsens despite a lower mean.
Raw telemetry exposes a confound. Each timed key locates its view through a
blocking `runOnMainSync` call before injection. A busier baseline therefore
receives a slower effective input stream: mean finger-up intervals are
74.4/75.2 ms in A1/A2 versus 72.8/72.1 ms in B1/B2 at the nominal 60 ms setting.
Do not interpret those as equal-load fast-typing comparisons.

Resolve the fixed key geometry once before each timed query, while retaining
the same physical targets, injection, observation boundary and input corpus.
This test-only correction removes the main-thread barrier from the timed loop;
it changes no app code. Preserve all earlier runs as diagnostic evidence, and
repeat both binaries with the same corrected test APK. Check actual injected
intervals, raw-editor delivery and missing candidate frames, not just p95.

The first fixed-target baseline (A3) injects at 60.2 ms mean in Chinese, but its
raw editor observer still stops at the next finger-down, just as the candidate
observer did before commit 43bd3b6. Give editor observation its own owner through
the next finger-up, including Space. Keep A3/B3 candidate evidence, but do not
use their conditional raw-editor timing as proof of touch loss or low latency.
Run both app APKs with the final observation method before accepting the trial.
