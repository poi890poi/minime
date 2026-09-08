# Dictionary ordering, cost and apostrophe restoration

Baseline 27f3f37 / 0.6.1. User reports exact phrases behind meaningless sequences,
less smooth input with custom dictionaries, and Cant/can't ordering.

Reproduction: optional exact matches were deliberately inserted at slot 3 after
raw and two guesses. Manual entries were sorted alongside decoder candidates with
unrelated score scales. English apostrophe matches existed but were appended after
prefix completions; mixed Pinyin did not request English corrections at all.

Behavior change: exact custom/learned/enabled-pack matches should precede guesses;
raw remains independently recoverable and Space selection is preserved except for
the explicitly requested apostrophe restoration. Do not add word-specific rules.
The user clarified: always restore apostrophes unless the raw spelling is a valid
word. Cant is a real word in AOSP and Merriam-Webster; keep its valid default while
making can't a leading alternative. General source-backed restorations operate on
Space in normal fields, independent of optional broader spelling correction.
Preserve valid English, technical/literal fields, explicit raw recovery and casing.

Performance hypotheses: full map copying on configuration, repeated custom TSV
parsing, repeated PhraseLexicon parsing; measure separately from indexed lookup,
loading, Rime and rendering. Use identical phone microbenchmarks before/after,
record approximate heap and limits. Cache only immutable dictionary composition and
saved-value-derived parsed data, with immediate invalidation on edits/deletion.

Keep ordering, apostrophe behavior and performance in independent commits. Verify
core/source-wide paired cases, frozen conversational and fresh Wikipedia samples
before Android integration. Restore phone IME/preferences and sleep/verify display
after each session, including failures. No telemetry or real editor corpus collection.
