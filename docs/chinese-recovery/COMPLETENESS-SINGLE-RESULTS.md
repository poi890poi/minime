# Prefer one complete Pinyin match, retain the alternatives

September 25, 2026. **Admit the single-preference rule.** This follows the rejected
[all-complete grouping](COMPLETENESS-RESULTS.md), with scope frozen in the
[follow-up plan](COMPLETENESS-FOLLOWUP-PLAN.md). Baseline `21557f4`; no dictionary,
frequency, matching limit or source identity changes.

The built-in decoder now retains a fully typed path when the same output also
has an incomplete path, then prefers just its strongest complete whole-input
candidate. Other alternatives keep their frequency order. The composition
sort preserves that preference after explicit learned votes. Partial-input
choices retain their ordering and require a tap; no prefix is silently accepted
as the whole spelling. This adds no per-word rule, score adjustment or candidate.

Rime transport does not establish spelling completeness. A list with unknown
native reading metadata keeps its existing score order. The rule also leaves
Zhuyin, English and focused Taiwanese/Japanese policies alone. It is not a new
four-language ranking mechanism.

| Development evidence | Baseline | Single preferred full match |
|---|---:|---:|
| Reference-supported Space, 405 complete-syllable probes | 313 | 337 |
| Complete reference glyph/reading pairs available | 6,257 | 6,257 |
| First-choice whole targets, 24,244 labelled broad episodes | 8,789 | 8,844 |
| Whole target in first eight, same broad episodes | 11,523 | 11,525 |
| Whole target in first eight, 11,220 essay episodes | 1,249 | 1,249 |
| Useful first-eight slots, same essays | 12,268 | 12,268 |
| First-choice whole targets, 4,608 conversation episodes | 442 | 445 |
| Whole target in first eight, same conversations | 610 | 610 |
| Useful first-eight slots, same conversations | 4,852 | 4,852 |

Whole-target coverage means the supplied intended text appears at that rank,
not that Space necessarily commits it: raw recovery may remain the default.
Useful slots count occurrences that accept the whole target or an explicitly
selectable target prefix. These exposed corpora are development evidence, not
fresh holdouts. Complete-reading support does not establish intended homophone
choice. All 31,527 broad and 4,608 conversation candidate inventories/spans are
preserved; actual Space and suffix-editing checks pass for every episode.

Keep the adverse results: broad first-choice coverage has 65 gains and ten
losses, including eight essay episodes; essay first-five coverage loses two.
Conversation first-choice coverage has five gains and two losses. Preference
for fully spelled words can move an intended abbreviated phrase to second
place. The first-page gate alone did not settle that tradeoff.

The [frozen Google diagnostic](COMPLETENESS-REFERENCE-PLAN.md) includes all ten
distinct first-choice loss inputs, ten hash-selected gains and eight other
changed defaults. The phone records all 56 provider observations and 112
screenshots; all 28 Google typed screenshots were visually verified and all
28 Space actions end composition. Agreement with Google's actual committed
text is 1 -> 13 of 28, with 13 gains and one loss. By stratum: losses 0 -> 3,
gains 0 -> 7, other changes 1 -> 3. This biased diagnostic is not a population
accuracy estimate or proof that Google's constructed output is linguistically
valid. No Google strings enter production data or code. The original APK,
preferences, learning and Samsung IME were restored, actual display OFF verified
three times, and reservation explicitly released.

Native/core broad and conversation first-one/five/eight whole and compatible
metrics and Space outputs remain identical in every genre/condition. Only
28 broad records and one conversation record change lower order. The pinned
desktop evaluator completes 13,014 inputs / 11,272 native queries: all 130,233
English token outputs and Chinese Space/first-page target metrics are unchanged;
four of 21,044 records have lower-list changes. The separate 6,144 English
word/prefix probes preserve every English-mode output and target rank within that mode.
The [corrected matched-configuration replay](ENGLISH-CONFIGURATION-CORRECTION.md)
finds Chinese-mode English first-eight coverage unchanged at 2,218/3,072, with no
gains or losses. The earlier claimed 22 gains compared enabled optional dictionaries
against disabled dictionaries and is withdrawn. All 21 Chinese-mode Space changes
are retained in local telemetry; no English-mode Space output changes.

The full core suite passes 1,770,165 assertions, including generated duplicate
paths, learning, partial-input ordering and unknown native metadata. This count
is mechanical coverage, not accuracy. Dictionary source identities and all 24 APK
language assets are unchanged. Model SHA-256 remains
`d82ec850c067e9300246d487967a19f29ab473eac87503234531dcb3a09d4b13`.

An ABBA desktop lookup check uses 1,488 fixed inputs, one warm-up and three
measured rounds per process (4,464 measurements each). Baseline/trial p95 is
0.755/0.791 ms in the first pair and 0.782/0.756 ms in the reverse pair. Mean
paired differences are +0.0085/-0.0150 ms. No repeatable slowdown or speedup is
established; optional-dictionary signatures and candidate counts are identical.
This does not certify Android presented-frame latency or human touch hit rate.

Aggregate receipts are in `completeness/single/`; original per-query outputs,
screenshots and preference backups remain local. The actual trial release-mode
APK is test-signed, not a Play-upload artifact. Final packaged integration passes
three tests in session `d4de1693-eb68-400e-ad3c-a26b4aed7616`: all 55 unique
spellings from the two reference studies produce the exact core-predicted Space
output with composition finished; all four modes expose selectable candidates;
explicit capitalized English completion and raw Space recovery work. Tests type
through visible keys in the production Settings editor of a non-debuggable APK.

The APK SHA-256 is
`2037bfe2a3fe7888596f449aedd54b0f09853e6842f7a44dfd492f4c59942c16`.
Original APK/settings/learning/IME are restored and actual display OFF is verified
through cleanup and both outer checks. The mutex and coordinating reservation
are released. No source-rights, platform/upgrade, human-touch or large-sample
latency gate is cleared by this scoped admission.
