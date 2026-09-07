# Candidate strip stability

Bug fix, baseline b9c426e (0.5.3). The user reports painful candidate flicker
after the typing fixes. CompositionEngine.refresh publishes raw-only candidates
and preferred=0 on every letter before the asynchronous decoder finishes.
KeyboardView immediately renders that intermediate state, removing the Chinese
words and hiding their phonetic header, then restores them on completion. It also
recreates the entire candidate strip on duplicate renders (including drain).

Falsifiable checks: a controlled delayed decoder must leave the previous Chinese
row visible while raw spelling advances; an unchanged render must preserve the
candidate scroller and word views. These checks exercise Android presentation,
without making claims about dictionary accuracy or changing decoder timing.

Keep a completed presentation snapshot during a pending query within the same
composition. Replace it atomically when the matching result arrives. Cache
identical strip presentations. Never reuse a snapshot across composition/editor
boundaries. A visible choice must be bound to its identity, not a mutable list
index: wait for the current query and accept only an equivalent current candidate.
An obsolete choice cannot consume spelling or train learning. Exact spelling and
Space retain their existing core contracts. Test this acceptance rule in core
before Android builds, including partial candidates, pending actions and resets.

Risks: stale selections, retained private-field content, expanded-grid mismatch,
scroll resets, delayed raw spelling, and candidate touch targets changing during
a gesture. Cover these alongside height and rapid letter touch regressions on
the authorized phone. Preserve ranking/data, candidate order/style, keyboard
height, punctuation and gestures. Restore IME/preferences and sleep the phone.

The baseline phone checks fail for both hypotheses (android-baseline.txt).
The first attempt retains content but still replaces views when the inline raw
label changes. Reuse the scrollers and word slots, including the expanded grid.
Keep a touched word's action through Android's posted click, and defer row updates
during a candidate gesture. A test of posted clicks must attach the keyboard to
the synthetic activity and drain the main queue; detached views do not execute
their posted click until attachment.

Requiring the old consumed-phonetic count also rejected a valid selection when
typing expanded a whole-token word into a prefix choice. Match text and literal
intent, then use the current decoder alignment and current learning reading.
The existing real Rime partial-selection test detects this error. Retain the
first failed run (android-first-attempt.txt); it is not a passing gate.
