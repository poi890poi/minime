# MinIME 0.5.2: stable height

The IME reserves 308 dp in portrait and 208 dp in landscape, plus system
navigation insets. Typing, clearing/committing composition, language/layout changes,
expanded candidates, emoji/symbol panels, and private/loading messages use the
same budget within each orientation. The 24 dp header remains allocated at idle.

Cause: conditional phonetic/status rows and independently sized panels previously
changed the WRAP_CONTENT height. The baseline Android measurement reproduced a
jump from 852 to 924 pixels when ni acquired Chinese candidates (density 3).

The fix reserves a header and a fixed keyboard area. Four-row layouts share the
same body area; the bottom row keeps its vertical position. Emoji and symbols
use two rows per page, with all catalog entries retained and existing paging.
No core prediction, dictionary, candidate acceptance, learning, gesture thresholds,
or preference schema changes are included.

Verification uses the actual Android view hierarchy, both orientation resource
configurations, font scales 1.0 and 1.3, navigation padding, content states and
child containment. Real-window assertions cover typing, expansion, commit,
language switches, palettes/choosers, Zhuyin, password and numeric fields.
The portrait screenshot and raw-recovery screenshot are retained. Orientation
coverage is view/configuration measurement, not a claim of a physical rotation test.

Debug/test/release builds pass; lint has zero errors and 15 warnings. Asset and
APK provenance checks pass and both app APKs pass 16 KB ZIP alignment. Language
model bytes and Rime bundle are unchanged from 0.5.1. Corpus ranking evaluations
were not rerun for this Android-only layout change.

The audit retains the failed baseline, initial containment failure and corrected
test assumptions. In particular, raw-row placement still must be above the phrase
strip; the check now waits for the asynchronous phrase before reading both bounds.
Artifact hashes are in package.json.

Final phone gate: all 11 focused layout and interaction tests pass in 65.656 seconds
on RFCR91GWXLX / Android 13 / ARM64. The original Samsung IME and saved settings/
learning files were restored and their hashes read back. Display verified Dozing;
AOD was not changed. The test run includes real-window height stability, emoji and
symbol selection, both-language expansion, partial Rime selection, scrolling,
slides/caps, English trace, and hide/restart behavior.
