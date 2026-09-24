# Rejected dimension guard

This is an archived experiment, not production guidance. The trial removes a
redundant layout request but fails the repeated responsiveness gate. Read
[results](../LAYOUT-RESULTS.md) before reusing it.

To reproduce locally on runtime b47bb2a: copy BoardLayoutTest.java into
app/src/androidTest/java/dev/minime/ime, build the test APK, and apply
rejected-dimension-guard.patch only for the trial app. Baseline must fail both
unchanged-board-request assertions; trial must pass. Restore both files after
the experiment. Phone runs require explicit reservation and the guarded runner.

The compared APK/test hashes, input hash and raw phone sessions are recorded in
[layout-binaries.json](../layout-binaries.json). Timing reports use the unchanged
tools/report_touch_latency.py on each session's touch-latency.tsv. The archived
test is excluded from normal builds because the runtime trial was not accepted.
