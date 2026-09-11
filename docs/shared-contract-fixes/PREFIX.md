# Prefix acceptance fix

Cause: CompositionEngine classified consumed prefixes using lowercase Pinyin
syntax, while JapaneseKana correctly supplied raw offsets for uppercase,
title-case and hyphenated input. Acceptance therefore committed a prefix as a
whole token and dropped the remaining spelling. Partial learning additionally
bypassed the focused-language namespace and its on/off setting. Base and add-on
candidate paths did not share malformed-span validation.

Fix: Candidate documents and validates raw UTF-16 consumption boundaries. Every
provider passes the same validation before presentation. Only explicit selection
can accept a proper prefix; automatic boundaries retain a whole-input default or
raw recovery. Partial/whole choices share the learning policy. Preserve exact
supplemental suffixes, including separators. Legacy non-supplemental lowercase
Pinyin apostrophe trimming remains scoped to its existing syntax. Current decoder
alignment and stale-composition gesture behavior remain unchanged.

Verification before this commit:

- New regression fails against baseline e73dcec on focused partial learning.
- Shared core: 288,162 assertions, including 14,056 source-derived kana
  casing/privacy controls, malformed offsets, supplementary-code-point boundaries,
  focused-learning disabled, and existing Pinyin/gesture/lifecycle contracts.
- Frozen proposal replay: 6,144/6,144 acceptance controls pass; no unavailable
  choices or private learning writes. Candidate order and defaults match baseline
  on all 27,136 mode-condition evaluations.
- Pinned desktop Rime evaluator: all 13,014 inputs complete, 11,272 native queries.

See `prefix/verification.json` and raw replay files. Counts verify mechanics,
not language accuracy. No dictionary/ranking changes, Android build, phone tests,
or touch-latency claim are part of this commit. English-context work follows
independently. The new test's header parsing error was corrected before recording
the genuine baseline failure; no production behavior was tuned to a test word.
