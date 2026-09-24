# Conditional pronunciation frequencies integrated

The compiler now allocates a glyph's frequency among its existing toneless
readings using the unchanged Taiwan phrase source. The frozen experiment's
primary screen, tradeoffs and Google comparison are retained in
[PHRASE-READING-RESULTS.md](PHRASE-READING-RESULTS.md). This is a production data
change; no runtime decoder, completion rule or per-glyph override is added.

Recompilation exactly reproduces the frozen TSV and binary hashes in the
[admission decision](PHRASE-READING-ADMISSION.md). Only `zh_tw.tsv` changes among
the ten source TSV assets. The APK's 24 language assets differ only in `model.bin`
and `model-report.json`. Binary size remains 50,026,326 bytes; the TSV grows by
4,394 bytes. The estimator runs at build time. These size observations do not
certify phone latency.

The older committed `model-report.json` was stale (48,943,042 bytes); the actual
previous test APK and the controlled rebuild both already contain 50,026,326
bytes. This refresh records the actual generated model. Generated TSV files
explicitly retain LF on Windows checkout so their original-byte hashes do not
depend on `core.autocrlf`.

Integration evidence in `phrase-reading/integration/`:

- All 25,100 complete Pinyin glyph/reading pairs remain reachable; all 26,489
  tone-bearing Zhuyin pairs over 1,395 queries remain reachable.
- Both models retain identical candidate text, score, reading, flags and span
  after binary round-trip on 1,894 source spellings/frozen prefixes.
- Native/core broad (31,527 conditions) and chat (4,608) preserve first-one,
  first-five and first-eight reference and compatible-prefix metrics in every
  genre/condition. Broad Space changes only on three unlabelled prefix probes;
  chat Space is unchanged. Lower-ranked fallback changes are retained.
- The updated production core passes 1,770,105 assertions. The count differs
  from the old model because some loops depend on returned candidate inventory;
  no assertion was removed. This is contract coverage, not language accuracy.
- The pinned desktop evaluator completes 13,014 inputs / 11,272 native queries.
  English token outputs are identical. Chinese Space and first-page target
  coverage are preserved; 616 records have lower-rank/top-list changes. Output
  SHA-256 is `ff5607e5d4c580c9a6ddeab37443b21c33216f1beb6f5a46603fd93e6a57d8a2`.
- Dictionary, language assets, apostrophe metadata, model hash, pinned Rime and
  offline-manifest verification all pass. Six estimator and 19 source-framework
  structural tests pass.

The non-debuggable release-mode test APK is SHA-256
`c0ef5f26c891495cb36065c268d29c8e8d5ecf1efc01dfba7e54aa4a7ab4cd15`.
It uses the existing debug certificate for installation without erasing user
data; it is not a Play-upload artifact and version code is not advanced yet.

Phone session `b205ad19-4d34-404d-b783-6b398d481442` passes three tests against
the actual packaged model and IME: every one of the 28 frozen pronunciation
inputs commits the core-predicted Space output and finishes composition; all
four language modes expose selectable candidates; capitalized English completion
and raw Space recovery remain correct. Typing uses visible keys in the production
Settings editor. Expected values exist only in instrumentation assertions.

Original APK, preferences, learning and prior Samsung IME were restored. Display
OFF was verified by test cleanup, environment collection and the outer driver.
The shared mutex and coordinating reservation were explicitly released. Raw
device captures, preference backups, expected strings and corpus outputs remain
local. This verifies integration, not human touch hit rate, display latency,
Android 16/16 KB execution, the Play signing chain or store delivery.

The remaining completion-ordering issue and broader release gates remain open.
Keep the earlier core encyclopedic tradeoff visible: ten net first-eight targets
are lost while reference-supported Space, useful-prefix slots and Google
diagnostic agreement improve. No old labels were changed to hide those losses.
