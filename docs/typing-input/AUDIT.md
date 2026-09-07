# Pinyin input continuity and touch investigation

Baseline 94420d0 / 0.5.2. User reports unintended Chinese commitment before
finishing Pinyin and missed touches. Treat these as separate hypotheses until
reproduced. Owner boundaries: core composition/commit, service command routing,
Android pointer delivery and SlideKey gesture recognition. No ranking/data edits.

Code evidence: keyboard root and rows disable split motion events; SlideKey
cancels on ACTION_POINTER_DOWN. This can discard overlapping thumb taps. Release
must also land strictly inside the key, with no platform touch-slop allowance.
Letter slide alternatives use LITERAL, which commits the current Chinese default
before inserting a capital or symbol. Compare that last behavior with the installed
Google keyboard before selecting a correction. Ordinary alphabetic core typing
must retain composition and never commit a candidate just because results arrive.

Freeze controlled pointer traces before changing gesture rules. Exercise different
rows, same/different keys, both release orders, edge drift, real cancellation,
intentional slides and English trace. Verify through the actual ViewGroup dispatch
and real phone input, not accessibility clicks alone. Keep any shared rule in its
owner and preserve caps/slides/hold/delete/palette and stable IME height. Restore
phone keyboard/preferences, sleep and verify display after every session.

Use platform pointer splitting and ViewConfiguration touch slop, where supported
by the reproductions, instead of word-specific patches or arbitrary prediction tuning.
References: https://developer.android.com/reference/android/view/ViewGroup#setMotionEventSplittingEnabled(boolean)
and https://developer.android.com/reference/android/view/ViewConfiguration#getScaledTouchSlop()

Touch baseline: same-row overlapping w/o emits nothing; edge drift on n within
half Android touch slop also emits nothing. Cross-row n/i succeeds because the
intermediate vertical container still splits events. Enable standard splitting
at keyboard/row boundaries and track the initiating pointer; ignore secondary
same-key pointers without cancelling the owner. Allow release within platform slop.

The first slide study is withdrawn as an isolated comparison: snapshots recorded
an inactive editor (empty text) while screenshots showed accumulated input in the
visible one. Keep its evidence, but fix test ownership with a fresh synthetic
activity per case and attached/focused/active-input assertions. Freeze a travel
sweep before adjusting any slide thresholds; intentional slide behavior alone
cannot explain whether ordinary taps commit prematurely.

The isolated overlap study disproves release-order semantics: Google produces wo,
ni and xx under both release orders. The first fix produced ow/in on reversed
release and x for overlapping repeats. Complete an older plain letter tap when
the next letter touches down, then track the new owner. This matches Google and
preserves the user's phonetic sequence; it does not accept any Chinese candidate.
Slides/holds/cancellations are excluded from this rollover. Frozen holdouts now
include both release orders and repeated letters on the real phone.

The isolated upward-drift sweep found Google more sensitive than MinIME in this
configuration (30% versus 45% of key height), and both intentionally commit pending
Chinese on a recognized slide. Do not tune a larger slide threshold as an alleged
Google-parity fix. Ordinary letter typing and prediction callbacks are separately
checked for absence of commitment: 411 corpus inputs, immediate/out-of-order
results, 11675 total core assertions pass. This is composition integrity, not
language-model accuracy.
