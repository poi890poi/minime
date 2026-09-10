# A/L outer-margin contact recovery — 0.8.1

Type: bug fix, with regression instrumentation and a debug version increment.
The user's icon and screenshot requests are deferred at their direction; this
change keeps the existing artwork and store kit. Design drafts remain under
`artifacts/store-refresh/design-draft/` for the later design discussion.

## Cause and correction

The QWERTY second row previously put a non-interactive half-key spacer on each
side. A DOWN in either spacer could not reach a SlideKey, so the input vanished
before composition or candidate acceptance. This is a general row-boundary
problem, not an A-specific character or language rule.

On d23f9c0, the diagnostic delivered center and near-edge contacts to all 26
letters: 78/78 passed. All six contacts across the two blank outer margins were
lost. This reproduces one plausible mechanism for the reported A misses; it
does not establish the cause of every human thumb miss.

Each outer letter now owns its former half-key gutter. Relative padding keeps
its glyph and alternate-symbol center in place. Android's ordinary pointer
splitting, SlideKey ownership, overlap ordering and cancellation still handle
the expanded Views. Trace eligibility uses their touch bounds; the trace grid
uses the unchanged visual centers. At a 360 dp keyboard width, A and L now have
54 dp touch widths rather than 36 dp, with the same visual centers.

Risk: changing hit regions can affect adjacent keys, overlapping fingers,
accessibility bounds and tracing. The change expands accessibility bounds too;
labels and actions are unchanged. Only previously blank outer space is claimed.
There are no preference, dictionary, ranking, timing or permission changes.
Other rows, panels and Zhuyin retain their existing layout. The code runs when
keys are laid out, without a new per-tap nearest-key search.

## Evidence and limits

Before the Android build, `tools/test-core.ps1` passed 35,984 assertions and
`tools/test-desktop.ps1` completed the pinned native evaluation for the frozen
suggestion-latency inputs (628 uncached queries). These are baseline checks,
not a language-quality claim or a measurement of this layout fix's speed.

The final attached-view matrix runs portrait/landscape, Pinyin/English and
font scales 1.0/1.3. Across 14,960 cases, all 14,752 required cases pass, including
1,856 new margin tap, cancellation, up/down slide and overlap cases. Another
208 contacts beyond key boundaries remain separately reported ambiguous stress;
they are not counted as required hits. Existing all-letter drift, every ordered
letter pair, both release orders and reordered pointer IDs remain covered.
Original A/L visual centers are checked within two physical pixels.

Final device instrumentation: 7/7 tests pass, including margin-started English
traces on attached portrait and landscape keyboards. Four live IME checks also
passed: English word tracing/uppercase slide, Pinyin thumb overlap, case/number
slides and cancellation, and stable height across typing/panels.

Negative result: the first added trace-only probe used a detached View. Its
screen positions were unavailable, so trace eligibility could not be evaluated
and the probe failed. It was moved to the existing attached-view harness. That
harness now preserves screen-space raw coordinates while dispatching local
coordinates, matching the runtime recognizer. The full matrix was rerun after
this test correction; production code did not change between those runs.

This is deterministic dispatch evidence using virtual event times. Physical
sensor latency, OS edge-gesture interception and human hit-rate acceptance are
not certified by it. The four language modes share this QWERTY layout, but the
exhaustive matrix measures the Pinyin and English render configurations.

All sessions used the acknowledged SHINE handoff and shared phone mutex. Prior
IME and settings/learning were restored, display OFF verified, and the phone
window explicitly released. No viewport or AOD changes were made.

Machine-readable counts and full-report hashes: `touch-results.json`.
Local raw evidence: `artifacts/store-refresh/core-baseline.txt`, `desktop.txt`,
`edge-baseline.txt`, `al-integration.txt`, `al-final.txt`, `touch-portrait.json`
and `touch-landscape.json`. The final APK-only download ZIP has SHA-256
`e421c3140132e1543cb6ff5e4ca95545f03875f620d8a6a19fda27847817f589`.
