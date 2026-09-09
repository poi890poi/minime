# Dedicated dictionary source decision

Taihoa: retain and extend the existing reviewed use from Han metadata to original
POJ headword lookup. The unchanged ChhoeTaigi b33c6a1 pin contains 91,339 records.
The bundled upstream README attributes the foundation to 鄭良偉, additions and
editing to 楊允言 and transcription/proofreading volunteers, under CC BY-SA 4.0.
This Taiwan-authored general dictionary supplies original numbered and Unicode POJ
and aligned variants. Import all supported source headwords up to the core's
96-character limit. Do not extract fragments from explanations or translate glosses.
Reject unaligned variants and unsupported input syntax; record counts. The broad
inventory includes historical/specialist vocabulary and has no typing frequencies.
Use an independent bounded lookup tier so it cannot evict the beginner dictionary
before the final merge. Source membership does not imply everyday popularity.

Remove iTaigi's 2–6 Han Mandarin-gloss and six-syllable eligibility restrictions.
Mandarin translation structure cannot decide Taiwanese vocabulary availability.
Keep beginner complete-example extraction; lift headword-only length restrictions.
Han examples retain the separately reviewed familiarity policy; adding vocabulary
does not establish community agreement on rare spellings.

Japanese: retain the complete eligible source-common reading inventory from the
pinned JMdict snapshot, all compatible common spellings, existing expressions and
names, both single-kana scripts and source-ranked kanji. No POS restriction remains.
The source marks 22,972 common readings; 19 unsupported aliases remain declared.
This is extensive lexical lookup, not complete Japanese grammar or sentence decoding.
Chinese and English retain their existing full core dictionaries and Rime assets.

All inventory queries are source-derived retrieval audits. Freeze them before the
new importer runs; they cannot establish independent conversational accuracy.
