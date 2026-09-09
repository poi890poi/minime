# Compact exact and alias storage

Performance/memory change, independent of vocabulary and search budgets. Full
focused exact readings already live in the sorted prefix index. Reuse that index
for exact lookup and drop redundant hash keys/lists, keeping abbreviated collisions
in the original map to preserve their order. Intern equal immutable candidate
sequences across indexes and source aliases. Keep duplicate references and source
metadata; nothing is filtered from vocabulary or lookup.

Same-source desktop retained all-pack heap: 153,965,848 -> 90,471,600 bytes;
repeat: 90,352,592 bytes. This is after GC, not Android RSS. All 23,532 frozen mode
outputs are identical. Core 32,328 assertions pass. No speedup is claimed for this
representation alone. The final phone build also includes the independently tested
branch index; it passes loading, mode UI and timing tests without the earlier OOM.
