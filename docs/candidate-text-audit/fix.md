# Device-readable candidates

## Cause and scope

The audit reproduced 938 distinct missing Han glyphs on SM-G781B/API 33. Source
strings and JNI transport contain valid Unicode; the installed fonts cannot
render those scalars. Filtering only TextViews would create invisible Space or
long-press choices. This fix therefore supplies Android font capability to the
shared composition engine, where display and acceptance share one candidate list.

This is a runtime bug fix, not an editorial dictionary refresh. No entries,
source weights, matching rules, normal relative order, permissions or stored
preferences change. No font is downloaded or bundled. The availability decision
is device-specific; the same vocabulary remains available on supporting devices.

## Behavior

- Source, native, learned, custom, completion and idle candidates pass the same
  final availability check. English tracing is checked before generating text.
- Readable choices retain their order and consumption spans. If the old automatic
  choice is unavailable, Space falls back to the user's exact raw input rather
  than silently choosing a different phrase. A surviving default stays selected.
- Exact raw input always remains recoverable, even if its font is unavailable.
  Explicit literal typing/paste is not rewritten by the suggestion policy.
- Taiwanese pairs retain both source forms and their original learning identity.
  If one form cannot render, show only the readable form and disable its alternate
  action. If neither form can render, omit the candidate. Han-first preference
  can fall back to readable phonetics, with the displayed output also used by Space.
- Font capability changes invalidate old touch snapshots. The cache is recreated
  with the service/input view and is bounded to 8,192 Unicode scalar decisions.

Android uses the candidate's default TextView font configuration and Paint.hasGlyph
for individual Unicode scalars (available on all supported API levels). Combining
marks are checked individually; format controls and variation selectors are not
standalone glyph requirements. This avoids treating a valid base-plus-tone-mark
rendering as a required single ligature. The independent test oracle still checks
shaped glyph IDs, not the production predicate's return values.

## Verification contract

Core tests use generated, simulated capability gaps instead of production word
exceptions. They cover preserved/default removal, all-rejected raw recovery,
partial consumption, private fields, paired forms in both directions, canonical
learning, disabled long presses, queued Space, stale snapshots, English custom and
idle predictions, and tracing. Existing core and desktop Rime corpora must pass
before the Android build.

Rerun the same 427-input, two-provider phone audit, including both candidate panels
and `rime` captures. Positive controls require readable Han, kana, decomposed POJ
and emoji to survive the runtime policy. Record time spent in the actual capability
predicate separately from dictionary lookup, rendering and touch latency. The
audit creates a fresh font cache per input and sums checks across that input's
prefixes; these are conservative test conditions, not production end-to-end typing
latencies. Phone cleanup and explicit release remain mandatory.

## Results (2026-09-15)

- Core: 308,652 regression assertions pass, including the generated capability
  tests and paired-copy availability checks. These are mechanics checks, not
  language accuracy.
- Desktop Rime: all 13,014 corpus inputs pass. The complete JSONL output is
  byte-identical to the pre-fix audit with no platform font restriction. Native
  timing output is diagnostic; these runs are not a controlled speed comparison.
- Local debug/test APK builds and lint pass: zero lint errors, 17 warnings.
- Phone: the glyph audit plus all 44 existing real-keyboard interaction tests
  pass (46 tests). After the final paired-copy preservation change, the two glyph
  audit tests pass again on the rebuilt app.
- The same 854 query/provider combinations previously produced 3,716 unreadable
  view occurrences (938 unique unsupported scalars). They now produce **zero**.
  The fixed audit checks 129,614 view occurrences, including repeats and offscreen
  entries. The source census remains 647,768 rows / 22,162 code points / 674 font
  gaps: dictionary vocabulary has not been deleted.
- Phone IME/preferences/learning restoration and actual display OFF were verified
  after both sessions. The shared reservation was explicitly released.

Final phone capability-check cost, summed across each input's typed prefixes,
with a fresh capability cache for each input (427 probes per provider):

| Provider | Median | 95th percentile | Maximum |
|---|---:|---:|---:|
| Java | 4.123 ms | 10.970 ms | 22.294 ms |
| Rime + Java | 1.310 ms | 2.758 ms | 5.834 ms |

These measure only the predicate, not a keypress or end-to-end suggestion latency.
Production retains the cache across inputs; no production latency claim is inferred
from these deliberately colder conditions. No new font payload is added.

Evidence: [fixed summary](fixed-phone-summary.json),
[final audit output](fixed-phone-tests.txt),
[interaction suite](fixed-interaction-tests.txt),
[fixed keyboard capture](rime-native-fixed.png).
