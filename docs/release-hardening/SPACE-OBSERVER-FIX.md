# Same-text Space observation

September 24, 2026. Test-observer bug fix; no app, dictionary, selection or timing
cadence change. The upcoming language-specific workload includes ASCII POJ whose
accepted form can equal the typed spelling. A whole-token commit ends composition
without necessarily changing characters. The old TouchLatencyTest predicate
required changed characters and therefore reported such acceptance as missing.

The regression drives actual CompositionEngine Space acceptance with invented
same-text and changed-text lexical candidates, both synchronous and deferred.
The old predicate fails at `completed Space must be observable even without
character changes`. The editor's commit callback occurs before engine.raw clears.
Consequently the test must retain its changed-text observation during that
callback, and additionally accept unchanged text after composition has ended and
the engine has released its owned spelling. No composing span alone is insufficient:
an unprocessed Space with unchanged text and nonempty engine spelling is rejected.

CommitObservation lives in testSupport, included only in host tests and androidTest.
54 added host assertions exercise these transitions and unfinished controls. The
full core run passes 1,768,350 assertions. This is a measurement contract, not an
improvement in dictionary accuracy or app latency. Android compilation and actual
same-text frame submission remain to be checked with the new workload. A missing
same-text TextWatcher callback is still missing; it is never assigned a timestamp
from the engine or another event. Submitted-frame timing remains distinct from
physical contact and panel presentation.

Raw reproduction: artifacts/release-hardening/commit-observer-before.txt and
commit-observer-after.txt. The previous eight-query timing results are preserved;
they are not recomputed or presented as corrected measurements.
