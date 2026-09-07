# Gap closure implementation contract

Baseline: reviewed 0.2.1; see GOOGLE_MINIME_GAP_REVIEW.md for paired evidence.
Deliver independently reviewable commits for acceptance, English boundaries,
ranking/retrieval, language assistance, interaction/lifecycle and performance.
The baseline is committed separately before implementation.

Acceptance is a behavior correction: incomplete Pinyin already has candidates,
but the classifier defaults to raw Latin. Prefer Chinese for lowercase abbreviated
readings with candidates, while retaining recognized English words/prefixes,
technical tokens, explicit English mode and exact recovery. Private fields use
the same generic policy without learned votes.

English word boundaries are a behavior change: explicit completion arms a deferred
space before the next word; punctuation, Enter, Space, deletion, cursor movement
and language transitions resolve/cancel it deliberately. No editor-wide text log.

Prediction changes may use only attributed source dictionaries, installed input
context and opted-in bounded local learning. Paired expected outputs and holdouts
must not be runtime features or per-phrase overrides. Compare retrieval/ranking
separately from default acceptance. Preserve negative results.

New English correction is configurable and defaults off; recovery remains exact.
Capitals honor editor flags and manual Shift. Literal/password/numeric restrictions
take priority. Optional recents/adaptation remain local and respect private input.

Lifecycle recovery may reuse only the IME-owned composing range with a matching
editor/selection; external cursor changes must not rewrite another range. Heavy
lookup must not publish stale results after newer input or a field switch.

Validate each core slice before committing; add focused device tests for Android
boundaries and run the final native suite, paired probes and performance benchmark.
Restore the phone's prior IME/preferences and sleep its display on every exit.
Document unclosed quality/device gates instead of claiming Google parity.
