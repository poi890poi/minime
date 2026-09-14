# Joined KALQ option

2026-09-14. New optional layout; QWERTY remains the default. The preference applies
to all Latin-letter boards: Chinese Pinyin, Taiwanese, Japanese and English,
including text/password fields. Dedicated Zhuyin and numeric boards retain their
own layouts. Mode, candidates, corpus, correction defaults and learning are unchanged.

Rows use the joined KALQ positions from the completed compact-layout experiment:

```text
M B W H G T O J
P _ X C I E _ U
R Y S Z K A L Q
D N F V Shift  Delete
```

Each underscore is Space. Shift and Delete each occupy two cells in the spare
lower-right area. The normal bottom row remains available (symbols, comma, mode,
Space, period and Enter), including long-press emoji/punctuation and raw acceptance.
Four letter rows share the existing three-letter-row vertical budget. The complete
keyboard height stays fixed; portrait rows are consequently shorter than QWERTY.
This is a full UI adaptation, not a claim that the earlier nearest-center simulation
measured these Android hitboxes or human speed.

Map slide-down symbols in visual row order, skipping Space/control cells, using
the complete existing 26-symbol inventory. Top row 1–8; second-row letters 9, 0,
@, *, +, -; third-row letters =, /, #, (, ), apostrophe, colon, quote; final-row
letters ?, !, ~, ellipsis. The existing non-English inventory substitutes its
full-width punctuation in the same slots. Width follows the editor/mode punctuation
policy. Capitals use slide-up, one-shot Shift and Caps Lock as before. Hint text
must remain readable on shorter rows. Symbol pages exclude the actual main-board
inventory and remember their page as before.

The persisted boolean `joined_kalq` defaults false and is included in preference
backup validation. View layout identity includes it so returning from Settings
rebuilds keys without stale geometry. Two-thumb contact ordering must include both
internal Space keys and the new Delete position. Cancellation/holds/slides retain
their existing ownership. The QWERTY-specific word trace decoder is disabled for
this layout; Settings explicitly explains that limitation. No trace coordinates
from KALQ are sent to QWERTY templates. No experimental correction model is shipped.

Verify the complete alphabet, symbol inventory and mapping in core first, plus the
existing core/desktop suites before Android builds. Android tests cover every key,
slide directions, overlapping thumbs, all language modes, mode/setting switches,
private/numeric/Zhuyin fallback, height, panels and enlarged fonts. Capture the real
phone view after obtaining the explicit SHINE acknowledgement and shared mutex;
restore prior IME/preferences and verify display OFF even after failure.

Release separately with a new version code/name. Build locally, sign with the
existing upload key, verify AAB/APK identity, certificate, alignment and bundle
structure. Package APK, AAB, UTF-8 release notes and public notices/checksums; never
include signing material. Keep source archives available under existing licences.
Commit implementation and release preparation independently, push, then create
release binaries from that committed state. No hosted CI or Play upload.
