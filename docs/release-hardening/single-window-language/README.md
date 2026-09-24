# Broader single-window comparison evidence

The [frozen plan](../SINGLE-WINDOW-LANGUAGE-PLAN.md) fixes both binaries, the
common unhooked test APK, language shard 0, two cadences and run order. This is
previously exposed development input, not a new language-quality holdout.

`run.ps1` requires an explicit acknowledged deadline, the shared phone mutex,
cooled starts and 17 minutes of headroom before each session. It refuses existing
logs, restores original APK/preferences/learning/IME through the established
runner, verifies display OFF, and stops expansion on failed tests, workload
mismatch or missing raw/Space frames. `summarize.py` retains all raw, Space and
candidate denominators plus source/genre/condition strata and >100 ms observations.

`compare.py` requires all eight distinct completed sessions, unchanged raw/log
hashes, actual display-OFF records and cool starts. It compares exact paired
action sequences, preserves all observations, and additionally compares candidate
latencies for the same observed keystrokes in both builds. Source/genre summaries
are computed from raw episodes; percentiles are never averaged across strata.
Budget flags describe observed p95/p99 only, not missing-frame, human-touch or
whole-release acceptance. Small strata remain provisional.

Scripts read/write ignored `artifacts/single-window-language` and require local
raw evidence and the frozen corpus. The archived versions only relocate their
repository-root/script paths; `scripts.json` records both local and archived
source hashes. `binaries.json` verifies all 24 language assets are identical and
the common test contains the frozen corpus. No raw spelling, corpus, device dump,
private preferences or screenshot is included here. No hosted CI is involved.

The complete results report links the aggregate comparison and per-session
reports. None of these fixture APKs is a Play or user distribution.
