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

The symbol catalog now uses a stable partition: symbols absent from the current
main board come first; directly available digits/punctuation follow. Availability
comes from KeyboardView's actual Pinyin/English/Zhuyin slide maps, numeric keys,
and comma/period tap and slide outputs. No entries are removed. All ten category
inventories and relative order within each tier are verified across four board
types, including traversing every page. Pinyin and English fullwidth/ASCII bracket
availability differ, and the tests exercise that boundary explicitly.

The combined 11-test run passes in 37.924 seconds, covering navigation, all-category
ordering/inventory, the previous top-space regressions, portrait/landscape/font
height containment, attached keyboard reopening through letters and emoji,
insertion from the restored page, existing emoji/ZWJ/symbol selection, optional
recents/private isolation, and fixed height while typing and switching panels.
Logs are retained in memory-tests.txt.gz and combined-tests.txt.gz.

Debug, test and unsigned release builds pass. Asset provenance, model hashes,
four-ABI packaging and 16 KB alignment checks pass; lint remains at zero errors
and 17 warnings. No language model or decoder behavior changed, so the desktop
ranking corpus and unrelated letter-touch matrix were not rerun for this UI change.

The final 0.5.6 package passes five navigation/order/attached insertion tests in
9.314 seconds (final-tests.txt.gz). The screenshot records insertion from the
remembered second arrow page after visiting letters and emoji. Final preferences
match their pre-test hashes, Samsung IME is restored, and the phone is Dozing;
phone-restoration.json records the read-back.
