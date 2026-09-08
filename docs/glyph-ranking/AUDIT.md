# Glyph-order investigation

Probe every supported Pinyin syllable and proper prefix with no learned choices.
Use independent MOE school-corpus frequencies and held-out UD GSD counts only
after decoding. Trace core, native, merged, English-interleaved composition and
all-pack composition separately. Missing frequency records are unranked; character
frequency is not pronunciation frequency. Rime's source reading weights explicitly
downweight many alternate pronunciations, so those are not automatic defects.

Confirmed bug boundary: static add-on promotion elevates an existing exact base
candidate solely because another dictionary duplicates it. Only the leading base
choice was protected, and English completion interleaving could hide even that
choice from the check. This can move rare homophones ahead of common ones despite
both decoders already ranking them correctly. Fix duplicate ownership: a static
duplicate of an existing exact base entry retains its base rank. Keep manual/learned
choices, novel full dictionary entries, native scores, alternative pronunciations
and explicit raw acceptance intact. No glyph-specific frequencies or exceptions.

Verify generic fixtures with/without English interleaving, exhaustive layer probes,
the frozen conversation corpus and platform integration. Report unresolved native
and fallback frequency mismatches separately rather than claiming a universal
frequency fix. Keep this correction in an independent commit.
