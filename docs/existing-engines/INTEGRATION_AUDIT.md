# Existing Pinyin backend integration

Type: feature and behavior change. User asked to try established solutions before
more custom decoding. No word-join experiment was implemented.

Native comparisons with learning disabled favor librime 1.16.1 + Luna Pinyin and
Essay over the 0.3.0 custom decoder and AOSP PinyinIME. Fresh authored phrases were
frozen before running either engine; all full/initial/mixed variants are generated
by the same rule, without runtime access to targets. The 36-probe results are
15/36 Rime, 13/36 AOSP and 6/36 MinIME. These small sets do not establish broad
accuracy. Some individual choices regress (e.g. bkq); retain backend selection.

Owner: Android Pinyin decoding adapter. Keep the core commit/intent/privacy rules,
English and Zhuyin decoders, gestures, editor lifecycle and stale-query guards.
Only candidates covering the entire raw input may enter the whole-token commit
path: a Rime prefix candidate must never silently delete the remaining syllables.
Rime sessions are transient; disable user dictionaries and native logging. Only
pinned public models and synthetic/raw active composition enter the engine; no
surrounding editor text or personalized history is passed. Build from source with
attributed dependencies; no Trime JNI or Google APK binary reuse.

Risks: native ABI/build size and startup cost, candidate ordering changes, incomplete
phonetic fallback, and composition loss when changing backend. Provide the current
decoder as a selectable fallback. Validate packaged phone decoding, complete-input
consumption, privacy (no learned Rime data), language switching and async commits.
Native dependency/model hashes and reproduction instructions accompany the change.
