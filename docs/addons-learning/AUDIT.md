# Composition continuity, traceable add-ons and phrase learning

Baseline 6ddbfe9 / 0.5.6. Separate work into independently reviewed slices.

1. Buffer defect: KeyboardView uses `preferred != 0` both to separate raw recovery
   from the candidate row and to decide whether the floating buffer exists.
   A completed decoder reply that changes the default to literal hides nonempty
   composition. Test Chinese/literal/empty replies and asynchronous pending states
   before changing visibility. Buffer existence must depend on composing state;
   candidate selection/default behavior and fixed keyboard height must not change.
2. Optional sourced Taiwan and Japanese dictionaries: prefer existing openly
   licensed data, retain upstream identity/license and entry provenance, and keep
   each add-on separate from the base model with default-off switches. No scraped
   definitions, lyrics or book content. Missing/uncertain readings must be reported,
   not invented. Japanese should use bounded exact reading lookup and explicit
   candidate selection; it must not turn Pinyin/English into a Japanese decoder.
3. Optional repeated-phrase learning: observe only MinIME-owned accepted reading/
   output segments, never arbitrary editor text. Require repeated input, bound
   storage, exclude private/secure/literal fields, clear pending chains on edits,
   field/language transitions and punctuation boundaries. Keep existing explicit
   candidate learning independent. No automatic cloud transfer. Propose portable,
   versioned, encrypted sync and conflict/deletion semantics separately.

Core tests and desktop Rime evaluations precede Android integration for candidate
or acceptance changes. Add-on-off and learning-off output must match baseline.
Use broad independent conversational regressions and source-derived coverage
separately; data membership checks are not language-quality accuracy. Keep
negative results and coverage gaps. Device verification only on RFCR91GWXLX;
restore IME/preferences and sleep/verify display after every session.
