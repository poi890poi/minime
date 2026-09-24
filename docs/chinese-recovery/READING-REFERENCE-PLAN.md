# Reference study for source-reading priorities

September 24, 2026; frozen before Google observation. This is a pronunciation
diagnostic, not a representative language-quality score or production admission.

Use the fixed 499-input reading-prior audit. Select eight complete syllables whose
top dictionary candidate changes, eight proper syllable prefixes whose first-eight
candidate order changes, and eight unchanged complete-syllable controls. Within
each stratum select by SHA-256 of `reading-reference-20260924|` plus the query;
do not choose displayed words or use Google output to select inputs. Preserve
shortages rather than filling them with examples. Record every selected query,
stratum, input hashes and final plan hash before running the phone.

Compare the already-tested accepted MinIME runtime (1ea175a) and legacy Google
Zhuyin using the existing paired real-key harness. The newer explicit-uppercase
completion fix cannot affect these lowercase Chinese queries. No experimental
weights are installed. Capture the typed candidates and actual Space output;
verify raw spelling visually because Google's composition buffer is outside the
editor. Unavailable actions or wrong spellings are failed observations, not
missing-language suggestions. Compare source-prior trial output offline only
after the reference observation; agreement is not proof of semantic correctness.

Use only the explicitly reserved RFCR91GWXLX phone, the shared mutex and existing
restoration runner. Restore original APK/preferences/learning/prior IME, verify
display OFF and release explicitly after all clients have stopped. Keep all
screenshots and per-query observations local. This study changes no source
decision, runtime policy or release gate.
