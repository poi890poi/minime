# Follow-up: append enough content for one actual viewport

September 24, 2026. Freeze before building/measuring. The row-sized trial meets
the 33/50 ms scroll p95/p99 budgets (24.19/25.88 ms portrait, 31.48/33.73 landscape)
but increases scroll actions to reach the same tail from 344 to 728 and 254 to
342. Its small increments cannot keep up with the existing three-quarter-viewport
scroll workload. Reject that tradeoff; a speedup purchased with extra user actions
does not pass the stated scroll-effort requirement.

The two previous extremes used a row or two conservative maximum-height viewports.
This trial derives the increment from the actual ScrollView height: enough minimum
48 dp candidate rows to fill that viewport, multiplied by the same conservative
column count. Initial allocation, prefetch distance, presentation logic, scheduling,
engine candidates and test APK are unchanged. No measured percentile is used to
choose a constant or relax a budget. This is a geometry contract: each append
provides at least one viewport of minimum-sized candidate boxes.

Run the identical integration and frozen attached-frame workload. Require exactly
the accepted reference scroll counts/distances for each episode as well as complete
candidate/default identity and correct final selection. The 33/50 ms all-scroll
p95/p99 gate and subsequent repeat/typing requirements still apply. If the first
run fails, do not run an admission repeat or add another scheduling mechanism.
