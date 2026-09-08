# MinIME engineering rules

- Verify suggestion, ranking, composition and acceptance logic in the shared core
  before building the Android app. Use `tools/test-core.ps1` and the pinned desktop
  Rime evaluator in `tools/test-desktop.ps1`. Reserve device tests for Android
  integration, input lifecycle and visible keyboard behavior.
- Fix language quality through attributed data and general rules. Never add
  individual word/glyph exceptions or tune model weights to complaint examples,
  demos or test labels. Freeze broad, diverse corpora before evaluation; keep
  train, development and test roles distinct. Record negative results and missing
  coverage. Do not call a synthetic assertion count language-model accuracy.
- Keep independently reviewable changes in independent commits.
- Prefer original Taiwan-authored or locally curated language sources. A locale
  tag or Traditional spelling does not establish Taiwanese usage. Do not convert
  a Simplified Chinese corpus into production Taiwan vocabulary. Register source
  decisions, evidence and pins through `sources/README.md` before refreshing data.
- Essays and conversations both belong in evaluation, with separate genre results
  and declared input/error conditions. Split by document or conversation, preserve
  source lineage, and keep production, development and holdout roles distinct.
  Previously inspected or evaluated data is not a fresh holdout.
- Never handpick production dictionary entries, phrase IDs, names, aliases or
  per-word promotions. Use established source datasets and reproducible general
  extraction/filtering rules. Track dataset versions, licences, hashes and counts;
  do not curate a per-name article-link list. Keep evaluation corpora independent.
- Test only the authorized phone RFCR91GWXLX. Restore its prior IME and MinIME
  preferences, sleep its display after every session (including failures), and
  verify its display state. Do not change always-on-display settings.
