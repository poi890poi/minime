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
