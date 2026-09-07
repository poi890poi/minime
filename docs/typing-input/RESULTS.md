# MinIME 0.5.3: Pinyin touch and composition

Baseline: 94420d0 / 0.5.2. The stable-height fix is retained.

## Reproduced causes and correction

The keyboard disabled Android pointer splitting on each row and cancelled its
active key on a second pointer. Same-row overlapping w/o therefore emitted
nothing. A small release drift over a key edge also emitted nothing, even inside
half the platform touch slop. Standard pointer splitting, owner pointer tracking,
and the platform release tolerance correct those defects.

A paired Google study then exposed a second boundary: simply delivering each key
on release reversed letters when the second finger lifted first. Google keeps
finger-down order and both overlapping repeats. MinIME's intermediate fix produced
ow instead of wo, in instead of ni, and x instead of xx. A new letter contact now
completes the older plain letter tap before starting the next, matching the
observed rolling-thumb behavior. Deliberate slides, holds and cancellations retain
their lifecycle. Hidden QWERTY key references are cleared on layout changes.

## Composition and interaction verification

411 deterministically sampled full/initial/mixed Pinyin inputs from the frozen
corpus retain every letter as composition under immediate and out-of-order
candidate callbacks. No callback commits text; explicit Space still accepts.
The full core run passes 11675 assertions. Injected corpus labels exercise callback
ownership only and are not evidence of ranking accuracy or production training.

The phone test samples every seventh eligible input from the existing Rime probe
fixture: 12 full/initial/mixed inputs, alternating release orders, including
repeated letters. It injects real multitouch events and asserts exact composing
text and no committed Chinese prefix after each pair. All nine focused pointer,
typing, slide, trace, punctuation, held-delete and stable-height tests pass in
52.538 seconds. No dictionary weights or production core behavior changed.

## Negative evidence and limits

The first slide study used a stale editor reference: screenshots contained text
while telemetry reported empty input. It is retained as withdrawn evidence.
The harness now creates a fresh synthetic activity per case and verifies that its
editor is attached, focused and owns the input connection before observation.

The isolated upward-travel sweep showed Google also commits pending Chinese on an
intentional slide. In this device configuration it recognized upward movement at
30% of key height, versus MinIME at 45%; a larger threshold is therefore not
supported as a Google-parity correction. Thresholds were not tuned. Google inserts
a lowercase Latin letter for that Pinyin shortcut whereas MinIME retains its
existing capital shortcut; this remaining difference is reported, not concealed.
The broad tests found no premature conversion from ordinary letter typing or
candidate arrival. These checks cannot identify every accidental gesture in a
user session, and do not claim complete Google parity or improved model ranking.

## Packaging

Debug/test/release builds pass. Lint has zero errors and 15 warnings. Asset and
APK provenance checks pass, model bytes are unchanged, and both app APKs pass
16 KB ZIP alignment. The distributed ZIP contains only the debug-signed installer.
The temporary Cloudflare download is verified against its local SHA-256.


Final paired replay on 0.5.3: all 12 provider/case runs were observed in 34.096
seconds. MinIME preserves all six intended raw sequences and their composing
spans across both release orders. Four of six Space outputs match Google. The
remaining two differ because xx ranks 學習 ahead of 謝謝 in MinIME, while Google
prefers 謝謝. The initial all-output-equality check failed and is not counted as a
PASS; touch integrity and candidate ranking are reported separately in
overlap-results.json. No special-case ranking fix is included.
