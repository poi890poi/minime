# Symbol navigation results

The symbol panel now retains its category and content page through reconstruction.
Navigation alone saves the location; no symbol insertion is required. Category
chooser pages cannot replace the saved content page, and closing the chooser
returns to the same content. Invalid categories fall back to Common and invalid
page indexes clamp to the category's bounds. Private input ignores saved state
and never updates it. Emoji navigation/recents retain their prior behavior.

Two real Android SymbolPanel tests pass in 0.497 seconds: reconstruction and
chooser cancellation/open state; removed category, oversized/negative page, and
private navigation isolation. Phone RFCR91GWXLX was restored to Samsung IME with
its saved settings/learning files and verified Dozing after the run.

This is intentional UI behavior, not a decoder or language-model fix.
