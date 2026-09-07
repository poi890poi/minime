# Candidate stability results

MinIME 0.5.4 removes the raw-only intermediate row between completed predictions.
Chinese suggestions stay visible during asynchronous computation; the current
spelling continues updating. Collapsed and expanded candidate containers and word
slots are reused. Identical renders leave the row alone. Candidate gestures keep
their touched word through result arrival and Android's posted click.

Choices are matched by text and literal intent against the current completed
query. If still present, the current decoder's phonetic alignment is used,
including whole-word to prefix transitions. If absent, no replacement word is
committed and no learning occurs. Composition/editor boundaries invalidate old
choices and display snapshots. Space still waits for the latest default.

## Evidence

- Baseline b9c426e fails both deterministic Android checks: the pending Chinese
  row disappears and an unchanged render replaces its views.
- Shared core: 11,726 assertions pass, including delayed/reordered/absent choices,
  partial alignment changes, queued typing, stale callbacks, editor resets and
  Space acceptance. The existing 411 frozen Pinyin continuity samples also pass.
  These are behavioral checks, not language-model accuracy measurements.
- Full desktop corpus: 13,014 inputs produce 21,044 records across evaluation
  modes, exactly equal to the retained production-order capital-vocabulary run.
  No candidate-output record differs; model bytes are unchanged.
- Focused fixed run: 11 phone tests pass in 31.857 seconds.
- Final 0.5.4 build: 18 phone tests pass in 72.295 seconds. Coverage includes the
  six deterministic candidate tests, attached-view DOWN/result/UP and cancellation,
  expanded candidates, real Rime partial selection, literal English recovery,
  horizontal scrolling, overlapping thumb taps, height, URL/password/numeric
  fields, and hide/restart lifecycle.
- Phone RFCR91GWXLX only. Both preference files match their saved hashes, Samsung
  IME is restored, and `mWakefulness=Dozing` is verified after every session.

The first attempt's failures are retained. It still recreated views when the
inline raw label changed and rejected valid prefix transitions. Its detached
touch fixture also inspected the result before Android could dispatch a posted
click. The final fixture uses an attached synthetic activity and waits for the
main queue; both touch completion and cancellation pass.

## Packaging and limits

Debug, test and unsigned release builds pass; packaged assets and four ABIs pass
provenance checks and both app APKs pass 16 KB ZIP alignment. Lint reports zero
errors and 16 warnings. The additional touch warning is a static-analysis limit:
CandidateWord delegates click detection to TextView.onTouchEvent, which invokes
its overridden performClick; both ordinary and accessibility selections are tested.
Hardware testing is ARM64 on the authorized phone.

The ZIP contains only the debug-signed installable APK. Its public Cloudflare
download is compared byte-for-byte with the local artifact. Hashes are recorded
in package.json and download-verification.json. This temporary link depends on
the local server and tunnel remaining available.

There is no decoder delay, ranking, dictionary, gesture-threshold or height change.
Actual completed candidate results can still change order or preferred intent as
spelling changes. This fixes the extra disappearance/reappearance and view churn;
it does not claim complete Google Zhuyin parity or a human eye-comfort measurement.
