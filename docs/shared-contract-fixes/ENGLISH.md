# English continuation across boards

The English-only branch in commit/refresh previously discarded English context
on mixed boards. Accepting the same English word therefore supplied continuation
and optional learning in English mode but neither on Chinese/English,
Taiwanese/English or Japanese/English boards.

This change keeps the last two accepted English words in separate transient
context and dispatches the existing English providers wherever English is enabled.
Eligibility uses literal, non-supplemental, untagged word identity, including
apostrophes. An identically spelled focused Taiwanese/Japanese choice remains a
choice of that language. Existing English-learning settings still control actual
persistence through LocalLearning; private fields access no personal history.
Static continuation remains usable privately, as in the existing English board.

The production implementation deliberately differs from the prototype by keeping
English continuation context separate from Chinese conversion and choice context.
Existing AFTER_LATIN votes, phrase context, corrections, spacing, dictionaries and
ranking are preserved. Restart, cursor/edit interruption, mode change, punctuation,
committed-text deletion and non-English acceptance clear the appropriate context.
MiniMeService now observes both contexts before handling cursor movement. No new
settings, database keys, frequency adjustments or source entries are introduced.

Verification:

- New transition regression fails against the prefix-only build because Chinese
  mode cannot expose English continuation. It passes with this change across all
  modes, casing, completion/contraction acceptance, foreign identity, private,
  literal/direct fields and editor boundaries.
- Final shared core: **289,460 mechanical assertions** pass. The original Chinese
  learning, English isolation, candidate identity and privacy regressions pass.
- **89,077 paired lookup conditions** retain exactly the baseline candidate order
  and defaults: 27,136 original mode conditions plus 61,941 conversation conditions.
- All **6,144** prefix acceptance controls still pass, with zero private writes.
- On **532 natural English context/target pairs per board**, static predictions
  are nonempty in 458 and contain the target within eight positions in **118**.
  Every English-enabled mixed board improves from 0/532 to 118/532, matching
  English mode. This restores access; it does not improve the language model.
- Pinned desktop Rime completes **13,014 inputs / 11,272 native queries**. Output
  JSONL is byte-identical between the prefix-only and combined fixes.
- `gradlew.bat :app:assembleDebug --offline` passes. This is a validation build
  retaining 0.8.2 version metadata, not a new versioned release or installation.

The side-effect-free `hasContext()` lifecycle accessor was added after the broad
replay. An exact source comparison verifies it is the only intervening core delta;
the final core tests exercise it and the Android build compiles its service use.
See `verification.json`, `english/verification.json` and the retained raw results.
Previously inspected corpora remain regression data, not fresh holdouts. Phone
lifecycle, visible latency and touch responsiveness are untested in this session;
SHINE's explicit phone reservation was respected throughout.
