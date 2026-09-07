# First-use layout and candidate alignment

User: layout/appearance and phrase candidates/selection remain visibly different.
Type: intentional UI behavior/design changes, followed by decoder default change.
Baseline c762657 / MinIME 0.4.0, compared live with installed Google Zhuyin on
Samsung SM-G781B Android 13, 1080x2400 at 480 dpi. Six paired scenarios all observed.
The phone was restored to Samsung Keyboard and Dozing after the comparison.

Observed: Google uses a flat pale keyboard, centered secondary symbols, 59 dp row
pitch, a compact language toolbar, six bottom controls, and unboxed candidates.
MinIME uses rounded key cards, 52 dp row pitch, corner upper/lower hints, an extra
emoji bottom key and a 90 dp raw slot plus settings/page controls beside candidates.
Google places raw phonetics above its candidate row. Its active editor remains empty
until commit; MinIME currently owns an inline raw composing span. This slice changes
candidate presentation, not editor composition ownership or partial-consumption rules.

Plan: (1) align QWERTY row geometry, key/background treatment, icons, secondary hints,
idle language toolbar and bottom controls. Keep upward capitals, double Shift and
single-tap language switching, and expose emoji through comma hold.
(2) put Chinese choices first with raw recovery in a separate compact row; replace
24-choice pages with continuous horizontal scrolling and a width-aware expanded grid.
(3) enable the already evaluated Rime backend when no preference exists, retain explicit
backend choices, and refresh active predictions when asynchronous model loading finishes.
No language data, target-specific scores or proprietary resources are introduced.

Risks: changed touch geometry and navigation can disrupt slides, tracing, emoji access,
accessibility and candidate choice. Keep semantic descriptions and verify actual touch
paths, full regression suite, exact/raw recovery, short/full/mixed Pinyin and real phone
screenshots. Default ranking changes can regress individual phrases, as recorded in the
Rime comparison. All phone work must restore prior keyboard/preferences and sleep display.

## Additional observed selection boundary

An isolated replay confirms Google `nihao` → tap 你 leaves 好/號/豪 candidates;
Space finishes 你好. MinIME 0.4.0 has no 你 choice because its adapter discards
prefix candidates. The first replay after an English emoji case could not reach
Google's Chinese toolbar; the isolated replay is the valid reference.

Add explicit prefix consumption metadata from Rime, retain full phrase defaults,
and continue converting the unconsumed spelling after a prefix tap. MinIME commits
the selected prefix into its inline editor immediately; Google's selected prefix
remains internal until completion. This deliberately retains MinIME's editor
ownership contract, while closing missing selection/remaining-phonetics behavior.
Test exact recovery, backspace, subsequent typing, language switch and privacy.
