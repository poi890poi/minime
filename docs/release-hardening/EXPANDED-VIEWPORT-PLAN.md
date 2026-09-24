# Expanded candidate allocation trial

September 24, 2026. Performance/intentional presentation change, independently
of the held font-page experiment. Baseline is accepted runtime `1ea175a`.

Problem: the complete expanded grid allocates and measures every candidate view
at once. The prior detached 46-prefix diagnostic measures expansion p95 at
529.95/526.41 ms over its first/repeat passes. Full-grid allocation dominates;
moving font checks alone does not remove it. These are detached UI costs, not
physical expansion-tap latency.

Trial boundary: reuse the established strip's incremental allocation approach
for the vertical grid. Allocate a conservative two-viewports-sized batch derived
from keyboard width, maximum expanded height and the existing 48 dp minimum
candidate touch size. Append the next batch near the bottom. Keep all candidates
in the existing ordered engine snapshot; no vocabulary, font rule, score, default,
source cap or decoder change. Do not combine the held font-page core change.

The visible ordering, full text/alternate annotation, tap/hold identity and
composition ownership must remain unchanged. Every tail candidate remains
reachable by scrolling. A new input/mode/editor or a rebuilt expanded viewport
starts with bounded allocation; incremental renders keep current scroll position.
In-flight gestures continue using the existing deferred-render ownership rule.
Repeated scroll callbacks may request only one batch until it is rendered.
Accessibility nodes are allocated as their page becomes reachable, following the
existing strip model. No row virtualization, hidden candidate cap or new package.

Verification before admission:

- Reuse the identical 46-prefix diagnostic APK on baseline and trial; compare full
  candidate/default hashes and initial expansion costs, cold/repeated separately.
- Android integration with generated short/long/paired candidates: initial bound,
  actual viewport reachability of the tail, correct deep tap/hold, new-input reset,
  collapse/reopen, pending replacement and stale-choice protection. Distinguish
  allocation from real visibility; wait for viewport state, not arbitrary sleeps.
- Record page-append work and actual expansion/scroll frame timing on the phone;
  savings cannot hide a later scroll stall or lost selection. Portrait and landscape
  must both overflow enough to request later pages.
- Reuse broad typing workload to check ordinary typing/acceptance remains stable.
  No new language accuracy claim; core ranking and desktop outputs must stay equal.

Land only a repeatable expansion improvement with preserved reachability and no
typing/selection regression. Record negatives and archive a rejected trial.
No prototype has been implemented by this plan; release gates remain open.
