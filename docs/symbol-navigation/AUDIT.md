# Symbol navigation and useful ordering

Intentional behavior change requested after 0.5.5: remember the last symbol
category/page and prioritize symbols not available on the active main board.
Currently each newly constructed SymbolPanel resets to Common page zero, and
Common starts with digits even though letter-board slides already enter them.

Keep symbol category and page in local settings, separate from optional emoji
recents. Restore on reopening/recreation; validate categories and clamp pages.
Private fields start from defaults and neither read nor write saved navigation.
Opening/dismissing a category chooser must not erase the content page. Emoji
navigation and optional recents retain their existing behavior.

Use the actual KeyboardView slide mappings and direct punctuation outputs to
derive the redundant symbol set for the current language/layout. Stable-partition
each symbol category with additional symbols first. Retain every existing entry,
category, insertion action and the three-row fixed-height layout. This is a
general availability rule, not phrase/glyph ranking or hand-tuned exceptions.
The same saved page is clamped if category sizes change; switching languages can
change ordering because their directly available symbols differ.

Verify reopen after navigation, category selection, chooser cancellation,
reconstruction, invalid saved state and private isolation. Verify whole-category
inventory and partition ordering across Pinyin, English, Zhuyin and numeric
boards, then attached keyboard switching, insertion and fixed height on the phone.
Only RFCR91GWXLX is authorized; restore prior IME/preferences and sleep its display.
No core model, composition acceptance, dictionary or gesture thresholds change.
