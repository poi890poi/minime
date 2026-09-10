# Focused-mode mechanism changes

Baseline: `bed0315` (0.7.7). The adjacent review and probe outputs are frozen
diagnostic evidence, not a holdout corpus or language-accuracy estimate.

## Contracts and independent changes

1. Static lookup in no-learning editors: preserve dictionary/mode availability;
   forbid personal reads/writes; retain password, direct and literal boundaries.
2. Explicit focused choices: persist and rank within a language namespace, using
   canonical paired identity; retain completeness ordering, raw recovery and
   existing static order when there is no personal evidence. Chinese phrase
   observations must not leak into English-secondary focused modes.
3. Runtime representation: evaluate duplicate removal independently. The frozen
   control has gains AND losses; it is not an accepted quality improvement.
4. Loading: scope optional indexes and cache ownership to requested languages;
   measure cold cost, retained heap and warmed query parity before shipping.
5. Worker: cancel superseded stages and delivery; keep serialized native work,
   revision checks, Space barriers and stable visible-candidate acceptance.

New grammatical prediction, inferred phrase joining and corpus expansion require
separate source/provider evidence. They must not be smuggled into defect fixes or
implemented by weakening the Chinese phrase validator. The existing optional
phrase-learning setting continues to govern that feature.

## Validation

Use shared-core contract regressions and the pinned desktop Rime evaluator before
Android compilation. Freeze existing source diagnostics for output parity, and
report source retrieval separately from conversation/essay benchmark results.
Measure latency distributions with denominators; do not substitute assertion
counts for quality or desktop timings for physical touch latency. Android tests
cover lifecycle and integration only, with authorized phone restoration and
verified display sleep if used.

Record each change, test result, remaining limit and rejected experiment below.

### Static lookup privacy boundary

The regression failed before the fix (normal static candidate present, private
candidate absent). Afterward `tools/test-core.ps1` passed 34,784 assertions.
The new controls cover all six core modes, full/partial input, synchronous and
decoder paths, forbidden personal-data access, literal fields and direct input.
Static candidate order/defaults match a no-history normal editor. The number is
a correctness check count, not vocabulary accuracy. No source assets changed.

### Focused explicit-choice learning

The original diagnostic recorded zero writes after 96 explicit selections. The
same diagnostic now records 96 writes, and all 32 private-editor queries retain
their focused static candidates (`probes-after.tsv`). Core regressions pass
34,936 assertions, including tap/hold, paired identity, stable static order,
language isolation, full-before-incomplete ranking, Space without self-training,
and the off switch. Existing generic local preference storage supplies bounded
persistence and clearing; focused keys use `FOCUS:<pack>`, not Chinese contexts.
Learned Chinese phrase lookup now requires Chinese to be in the active mode.

This learns preferences among retrieved candidates. It does not create vocabulary,
infer omitted readings, join Taiwanese/Japanese phrases or provide grammatical
continuation. The diagnostic still reports zero focused phrase observations and
zero idle candidates. Those need an independently designed provider.

### Duplicate-removal control: not shipped

The frozen representation-only experiment remains reproducible: 43 affected
queries, eight changed, nine gained and ten lost candidates. Removing repeated
references changes competition at bounded terminal and outer search stages;
equal source scores then use reading length/text order. It is not evidence that
new winners are more useful. Keep the runtime representation for now instead of
shipping a ranking change with no justified quality gate. No weights or entries
were tuned to these losses. A source-frequency policy needs separate evidence.

### Optional loading and cache ownership

See `loading/README.md` and its raw evidence. Two trial orders retain every
candidate in 23,532 paired lookups; no ranking change is attributed to loading.
Core tests pass 34,990 assertions, including queued Space, failed loading,
lifecycle cancellation, and independent Chinese specialist availability.
Language assets are a packaging transformation of the same registered source,
not a source refresh. Warm cache reuse and packaged-row equality have explicit
Android/APK integration checks to run after compilation.

### Obsolete decoder work

`DecodePipeline` checks request generations before work, after base conversion,
after native conversion, after add-on lookup, and again at Android delivery.
Core refresh/lifecycle cancellation also invalidates the worker when the new mode
needs no background query. The worker remains serialized. An in-flight provider
is not interrupted; cancellation saves only subsequent stages and stale delivery.
Existing revision checks and Space barriers remain.

Deterministic controls supersede requests at every provider boundary and verify
exactly which later stages do not execute, plus unchanged live merge order.
In-memory aggregate counts/timings contain no input text. Android tests exercise
the actual worker/main-thread delivery race, burst coalescing and close behavior.
Core suite: 35,011 assertions. Pinned desktop Rime evaluation completed 628 native
queries (p50 4.79 ms, p95 11.01 ms, max 54.26 ms including IPC). These timings
are not Android touch latency and do not establish a user-visible speedup.
