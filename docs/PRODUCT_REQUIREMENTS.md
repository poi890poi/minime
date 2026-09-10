# Legacy Google Zhuyin–Style Android IME
## Product Requirement Specification

## 1. Goal

Develop an Android input method editor that reproduces the practical typing behavior and interaction model of the legacy Google Zhuyin Input as closely as possible.

The most important requirement is not visual similarity. It is the typing model:

- Chinese and English can be entered naturally without explicitly switching language modes.
- Literal English words can be typed while the IME remains in Chinese mode.
- Spaces can be entered naturally in English text.
- Chinese phonetic composition and English typing coexist in the same text flow.
- The IME must not force the user into separate “Chinese mode” and “English mode” workflows for ordinary mixed-language typing.

The primary target is Traditional Chinese for Taiwan (`zh-TW`).

The first implementation should support both:

1. Zhuyin input.
2. Hanyu Pinyin input producing `zh-TW` Traditional Chinese.

The Pinyin implementation should preserve the same seamless mixed Chinese/English UX even though Latin letters are also used for Pinyin.

---

## 2. Core UX Principle

The IME must behave like a text-entry system that understands the user's likely intent, not like two keyboards joined by a language toggle.

Example desired input:

`我今天 meeting 可能會 late 一點`

The user should be able to type the Chinese phonetic input and the English words continuously without pressing:

- 中/英
- Globe
- Shift-to-English mode
- Language key
- Special “direct input” key

The system may internally classify segments as Chinese phonetic input or literal Latin text, but this must normally be automatic.

Explicit language-switch controls may exist as fallback controls but must not be required for normal mixed-language text.

---

## 3. Reference Behavior

The behavioral reference is the legacy standalone Google Zhuyin Input for Android, not current Gboard.

Do not treat the following as equivalent reference implementations:

- Gboard multilingual typing
- Samsung Keyboard bilingual mode
- SwiftKey multilingual prediction
- Standard Rime `ascii_mode`
- Any IME requiring explicit Chinese/English mode switching

The agent should obtain and study the legacy Google Zhuyin application if legally and practically possible, including:

- keyboard layout
- composing behavior
- candidate handling
- English-word entry
- space behavior
- punctuation
- backspace behavior
- cursor behavior
- committing text
- switching between character classes

The goal is behavioral compatibility rather than copying copyrighted visual assets or proprietary code.

---

## 4. Priority Order

Implementation priorities:

### P0
Typing behavior and composition semantics.

### P1
Candidate ranking and Traditional Chinese language quality.

### P2
Keyboard layout and interaction details.

### P3
Visual resemblance to legacy Google Zhuyin.

Do not sacrifice P0 behavior to simplify implementation.

---

# 5. Mandatory Input Behavior

## 5.1 Seamless Chinese and English

The IME shall permit transitions such as:

`中文 English 中文`

without an explicit mode switch.

Examples:

`這個 pronunciation 不對`

`明天 meeting 改到 3pm`

`幫我 check 一下 GitHub`

`USB debugging 沒開`

The user must be able to enter the English terms as ordinary literal Latin text.

---

## 5.2 Zhuyin Mode

Zhuyin symbols are unambiguously Chinese phonetic input.

Latin alphabet input while using the Zhuyin layout should therefore normally be interpreted as literal English/Latin text.

The IME shall support mixed sequences such as:

`ㄓㄜˋㄍㄜ˙ pronunciation ㄅㄨˋㄉㄨㄟˋ`

resulting in:

`這個 pronunciation 不對`

without toggling input mode.

---

## 5.3 Pinyin Mode

Pinyin mode is harder because Chinese phonetic input and English both use Latin characters.

The IME shall not simply treat every Latin sequence as Pinyin.

It must classify the current segment dynamically among at least:

- Pinyin intended for Chinese conversion
- English word
- acronym
- identifier
- number
- URL
- email
- filename/path
- programming token
- mixed alphanumeric literal

Classification may use:

- legal Pinyin syllable segmentation
- Chinese language-model probability
- English word probability
- context before the token
- capitalization
- punctuation
- digits
- token length
- previously committed language
- user history
- candidate-selection history

It must remain possible to type an English word that happens to be decomposable into valid Pinyin syllables.

Example:

`meeting`

must be typeable as literal `meeting` without requiring a language toggle.

Likewise:

`input`
`python`
`typing`
`pin`
`ming`
`shanghai`

must not be irreversibly forced into Chinese conversion.

---

# 6. Space-Key Semantics

This is a critical requirement.

The Space key must not have one globally fixed meaning such as “select first Chinese candidate.”

Its behavior must depend on composition state.

## 6.1 English/literal state

If the current composition is interpreted as literal English/Latin text:

Pressing Space shall:

1. commit the literal token;
2. insert an actual U+0020 space.

Example:

User types:

`meeting` + Space

Result:

`meeting `

not:

- a Chinese candidate
- `meeting` without a space
- mode switching

---

## 6.2 Chinese phonetic state

When there is an active Chinese phonetic composition, Space may:

- accept the best candidate;
- advance segmentation;
- perform the behavior matching legacy Google Zhuyin.

Exact behavior should be determined from the reference implementation.

---

## 6.3 Ambiguous state

If a Latin sequence is ambiguous between Pinyin and English, Space behavior must follow the currently inferred interpretation but allow easy correction.

A mistaken classification must never trap the user in an expensive workflow.

The user should be able to recover using ordinary operations such as:

- candidate choice
- Backspace
- Enter
- tapping the literal candidate

No language-mode detour should be necessary.

---

# 7. Literal Candidate

Whenever the user has entered a Latin sequence, the exact literal sequence must remain available until committed or edited.

Example composition:

`meeting`

Candidate list may contain Chinese interpretations if applicable, but:

`meeting`

must also be available as a literal candidate.

The IME must never destroy the raw input just because it can interpret it as Pinyin.

The literal candidate should be easily accessible, preferably in a stable predictable position when ambiguity exists.

---

# 8. Enter-Key Behavior

Enter shall follow Android editor context.

For ordinary text fields:

- commit the current composition appropriately;
- then perform the editor's expected Enter action when appropriate.

Where useful, Enter may provide a reliable way to commit raw literal input.

The exact behavior should be tested against legacy Google Zhuyin.

Do not globally overload Enter in a way that breaks:

- multiline editors
- search
- send
- done
- next
- go

Use `EditorInfo.imeOptions` correctly.

---

# 9. Backspace Behavior

Backspace must operate on the smallest intuitive editable unit.

During composition it should generally:

1. remove the latest phonetic symbol/letter;
2. update candidates;
3. preserve remaining composition.

After a candidate has been committed, Backspace should behave like normal Android text deletion unless the reference IME supports immediate reconversion.

Mixed English/Chinese text must not cause unexpected multi-character deletion.

---

# 10. Candidate Bar

The candidate bar must support:

- Chinese conversion candidates
- exact literal Latin candidate
- English word candidates where useful
- phrase candidates
- predictions based on committed context

Candidate selection must not implicitly switch language mode.

Candidate order should prioritize user intent rather than rigidly prioritizing Chinese.

Example:

If the input is strongly English:

`pronunciation`

the literal word should be first or near-first.

If the input is clearly Pinyin:

`wo xiang chi fan`

Chinese candidates should dominate.

---

## 10.1 Explicit English-board isolation

The dedicated English board permits English suggestions only. Chinese, Taiwanese
and Japanese conversion/add-on dictionaries must not run or contribute candidates
in that board, even when globally enabled or used immediately beforehand.
This restriction covers the collapsed row, expanded list, default Space result
and idle next-word predictions, not just the top candidate.

Custom entries, imported/saved predictions and choice counts cannot bypass the
scope. Known foreign dictionary provenance is excluded even for romanized output.
For untagged custom/predicted text, exclude non-Latin scripts; retain Latin
letters, accents, numbers and ordinary punctuation/symbols. A candidate's
`literal` flag describes acceptance, not its language. Do not guess the language
of untagged Latin names or user-authored shortcuts from their spelling.

Keep stored entries intact for other modes. Mixed/focused boards retain their
specified languages and explicit custom entries. Exact raw input and deliberate
literal/symbol insertion remain available even if their text is non-Latin.
Switching to English must invalidate earlier foreign results and candidate taps
without implicitly committing them, losing raw spelling or retaining the old
candidate row. An acceptance explicitly requested before the switch still follows
its original event order.

---

## 10.2 Japanese output forms

The Japanese board accepts romaji input and suggests source kana/kanji forms.
Generated Japanese romanizations must not appear as extra conversion candidates,
in either the first row or expanded list, for complete or incomplete input.
Retain romanized input aliases, single hiragana/katakana and common kanji, English
secondary suggestions and exact raw recovery. Remove generated romanization
outputs at dictionary extraction time so they do not consume lookup budgets.
Preserve original source spellings and explicit user-authored custom entries.

---

# 11. Punctuation

Punctuation handling should match natural Taiwan mixed-language typing.

The IME must handle context-sensitive punctuation without requiring a language mode switch.

Examples include:

- `,`
- `.`
- `?`
- `!`
- `:`
- `;`
- `'`
- `"`
- parentheses
- hyphen
- slash
- `@`
- `#`

Do not blindly convert ASCII punctuation to full-width Chinese punctuation.

For example:

`API v2.0`

must remain natural.

URLs and email addresses must remain intact.

---

# 12. Numbers and Alphanumeric Text

Literal sequences containing digits should normally remain literal unless there is strong evidence otherwise.

Examples:

`3pm`
`USB3`
`Wi-Fi 7`
`RTX5070`
`S23U`
`v2.1`
`1080p`
`30fps`

The IME must make technical mixed-language text easy to enter.

This is a first-class use case, not an edge case.

---

# 13. URLs, Email, Paths, and Code-Like Text

Detect contexts where literal ASCII input should dominate.

Examples:

`https://github.com/...`

`name@example.com`

`adb shell`

`C:\Users\Lee`

`foo_bar`

`MainActivity.kt`

`git checkout main`

In these contexts:

- do not perform Chinese conversion aggressively;
- Space should behave as expected for literal text;
- punctuation must remain ASCII;
- case must be preserved.

---

# 14. Traditional Chinese / Taiwan Output

Chinese conversion must produce Taiwan-appropriate Traditional Chinese.

Target locale:

`zh-TW`

Avoid naive post-conversion from Simplified Chinese.

The language model and dictionaries should preferably use Taiwan Traditional forms and vocabulary directly.

Examples of locale-sensitive distinctions should follow Taiwan usage where possible.

Candidate ranking should be based on Taiwan text data where legally usable.

---

# 15. Chinese Conversion Engine

The implementation may use an existing open-source engine where practical, such as:

- Rime/librime
- OpenCC for carefully scoped conversion
- another permissively licensed phonetic conversion engine

However, the engine must be adapted to the UX requirements above.

Using Rime with a standard Chinese/ASCII toggle is not sufficient.

The architecture should separate:

1. raw keystroke stream
2. composition buffer
3. language/intent classifier
4. Chinese phonetic parser
5. English/literal recognizer
6. candidate generator
7. candidate ranker
8. commit controller
9. Android IME integration

---

# 16. Intent Classification

For Pinyin mode, implement an explicit classification layer rather than relying only on the Chinese conversion engine.

Suggested classification output:

- `CHINESE_PHONETIC`
- `LATIN_LITERAL`
- `AMBIGUOUS`
- `URL_EMAIL`
- `CODE_IDENTIFIER`
- `NUMBER_ALNUM`

The classifier must run incrementally after each keypress.

It should be deterministic and low latency.

A statistical model may be added later, but the first implementation may combine:

- dictionaries
- Pinyin legality
- English word frequency
- contextual rules
- token-shape rules
- language-model scores

Do not require a cloud service.

---

# 17. Offline Operation

Core input must work fully offline.

No typed content may need to be uploaded to a server.

Network-based enhancements may be optional and disabled by default, but the basic IME must remain fully functional without them.

---

# 18. Privacy

The IME handles highly sensitive user input.

Requirements:

- no keystroke telemetry by default;
- no transmission of typed content;
- no collection from password fields;
- respect Android's secure-input flags;
- clearly isolate optional diagnostics from production input logging.

Debug builds may log state-machine transitions but should redact actual user-entered text by default.

---

# 19. Android Integration

Implement as a standard Android `InputMethodService`.

Minimum target:

- modern Samsung Galaxy phones
- stock Android devices
- Android 13+
- preferably Android 10+ where practical

Must correctly handle:

- normal EditText
- WebView fields
- Chrome
- messaging apps
- search boxes
- multiline editors
- password fields
- numeric fields
- email fields
- URL fields
- terminal/code-like text boxes where Android exposes ordinary IME APIs

Use the appropriate Android APIs:

- `InputMethodService`
- `InputConnection`
- `EditorInfo`
- composing text APIs
- selection updates
- extracted text only where required

Do not use Accessibility APIs to implement basic text input.

---

# 20. Keyboard Layout

## Zhuyin

Provide a Taiwan-standard Zhuyin layout comparable to legacy Google Zhuyin.

## Pinyin

Provide ordinary QWERTY.

The keyboard should not visually transform into a radically different keyboard merely because the classifier decides the user is entering English.

The whole point is that language interpretation happens within one continuous keyboard state.

---

# 21. English Shift and Capitalization

Shift must work naturally while Chinese input capability remains active.

Examples:

`Google`
`Android`
`USB`
`OpenAI`
`GitHub`

Typing a capital letter must not require switching to an “English keyboard.”

Caps Lock should function normally.

Capitalization can also be used as evidence that the current token is literal Latin text.

---

# 22. No Forced Mode Switching

The following interaction is prohibited as the normal workflow:

Chinese → tap EN → type English → tap 中 → continue Chinese

Likewise, automatically switching into a persistent English mode after detecting one English word is undesirable.

Detection should normally apply to the current composition/token, not globally change keyboard language state.

---

# 23. Recovery From Wrong Classification

Automatic inference will sometimes be wrong.

Recovery must be cheap.

Maximum desired recovery cost:

- one candidate tap, or
- one Backspace and continued typing.

Never require:

- opening settings;
- changing keyboard;
- changing language;
- deleting and retyping an entire phrase.

If an ambiguous input was classified as Chinese, the exact raw Latin sequence must remain selectable.

If an ambiguous input was classified as English, Chinese conversion must remain accessible.

---

# 24. Learning

The IME should learn user choices locally.

Examples:

If the user repeatedly chooses:

`meeting`

as English rather than a Chinese interpretation, its English probability should rise.

If the user repeatedly converts:

`ming`

to `明`, that preference can be strengthened in Chinese contexts.

Learning should account for context rather than globally forcing one interpretation forever.

User dictionaries should support:

- English words
- names
- acronyms
- technical terms
- Chinese phrases

---

# 25. Candidate Ranking Evaluation

Create an automated corpus containing mixed-language phrases.

Examples:

`這個 pronunciation 不對`

`今天 meeting 在三點`

`check 一下 camera setup`

`USB debugging 沒有開`

`我 push 到 GitHub 了`

`RTX5070 可以跑嗎`

`明天去 Taipei 101`

For each test case store:

- simulated key sequence
- expected commits
- expected literal spaces
- acceptable candidate sets
- expected top candidate where deterministic

Regression tests must verify behavior at the keystroke level, not merely final text.

---

# 26. Legacy Google Zhuyin Behavioral Study

Before finalizing interaction semantics, create a behavioral reference document from the legacy IME.

Record at minimum:

- key sequence
- composition shown
- candidates shown
- Space result
- Enter result
- punctuation result
- Backspace result
- final committed text

Test these categories:

1. pure Zhuyin
2. pure English
3. Chinese → English
4. English → Chinese
5. repeated Chinese/English switching
6. English spaces
7. capitalized English
8. acronym
9. numbers
10. URL
11. email
12. punctuation
13. ambiguous Latin sequence
14. editing previously committed text

Do not guess these details if the reference app can be tested.

---

# 27. Acceptance Tests

The IME is not considered complete merely because it can output Chinese and English.

The following must work without pressing a language-mode switch.

### Test A

Desired text:

`這個 pronunciation 不對`

The user can produce the entire string continuously, including both spaces around `pronunciation`.

### Test B

Desired text:

`明天 meeting 改到 3pm`

`meeting` and `3pm` must remain literal.

### Test C

Desired text:

`請 check GitHub issue`

English words and spaces must work normally while Chinese input remains available immediately afterward.

### Test D

Desired text:

`USB debugging 沒有開`

Uppercase acronym and lowercase English word must not disrupt Chinese typing.

### Test E

After entering an English word and pressing Space, the next keystroke can immediately begin Chinese phonetic input without pressing a language switch.

### Test F

After entering Chinese and committing it, the next alphabetic token may be classified as English without pressing a language switch.

### Test G

An ambiguous Pinyin/English token can be committed either as literal Latin or converted Chinese from the candidate interface.

---

# 28. Performance

## 28.1 Latency acceptance

The following are release acceptance targets for steady typing in every shipped
language mode, with its complete enabled dictionaries. They are engineering
budgets informed by UX research, not universal perceptual thresholds. A faster
median or a faster pairing alone does not satisfy them.

| Measurement | p95 maximum | p99 maximum |
| --- | ---: | ---: |
| Key event received by IME to visible raw spelling | 33 ms | 50 ms |
| Key event received by IME to stable suggestions for that spelling | 50 ms | 80 ms |
| Space or candidate-selection event to visible committed text in the editor | 33 ms | 50 ms |
| Suggestion request to current results applied by the core/main callback, including queues | 20 ms | 30 ms |

- Measure the originating event, not the later start of dictionary computation.
  For a tap committed on release, use the release event; measure DOWN-to-key
  feedback separately. For rollover, use the event that accepts the preceding
  key. Report gesture dwell separately, never as compute latency.
- Physical contact/release-to-display includes digitizer and input dispatch
  overhead and must be reported separately using calibrated external observation
  when available. Software timestamps or a callback are not proof of physical
  touch-to-display latency. Record measurement resolution and uncertainty.
- Visible means the frame presenting the changed spelling, candidates or editor
  text, not a call to `setComposingText`, `invalidate`, or a result callback.
  A held old row is not a fresh suggestion. Late, missing, superseded and timed-out
  updates must be counted separately, with maximum age of held suggestions.
- All observed steady-state spelling, suggestion and acceptance stalls exceeding
  100 ms require investigation and resolution before a performance PASS. Preserve
  such samples; do not hide them behind percentile cutoffs or successful-only data.
- Rendering must meet the active refresh-rate deadline (approximately 16.7 ms at
  60 Hz, 11.1 ms at 90 Hz, 8.3 ms at 120 Hz). Missed frame deadlines must be under
  1% of typing frames, with no consecutive misses attributable to the IME. This
  frame budget is separate from total interaction latency.
- Candidate generation may be asynchronous, but raw key feedback must not wait
  for it. Pending acceptance must retain the correct event order without losing,
  duplicating or committing stale text. No blank-row flashes or changing the
  identity of a candidate under an active selection gesture.
- Core input remains offline. Speedups must not reduce dictionary coverage or
  change ranking/acceptance without separate quality evidence and review.

## 28.2 Touch hit-rate acceptance

Hit rate is measured before autocorrection or manual repair: intended key actions
that produce exactly the intended action, in order, divided by all intended key
actions. Report missed actions, substitutions, duplicates, reordering and
unintended gesture activation separately. Language-model correction cannot turn
a touch miss into a touch hit. Candidate selection uses the candidate identity at
touch-down, not its later position.

| Evidence / input condition | Required outcome |
| --- | --- |
| Deterministic in-envelope taps, drift and thumb rollover | 100% correct actions; zero loss, duplicates or reordering |
| Cancelled gestures, intentional slides, long-press and double-tap controls | 100% of their declared outcomes; no unintended extra tap |
| Real human intended-key accuracy, before correction | At least 97% overall and 95% in every declared mode/posture/orientation group |
| Valid OS-delivered taps assigned unambiguously to a target | At least 99.9% delivered exactly once to that target |

These numerical touch targets are initial product requirements, not measured
MinIME results or published human-performance constants.

The deterministic envelope must include the existing [human-input matrix](human-input/RESULTS.md):
all letters, off-center starts, 2%/98% edge starts with up to 2 dp release drift,
all ordered letter pairs, both pointer release orders and reordered pointer IDs.
Extend equivalent action checks to Space, Backspace, punctuation, language switch
and candidate cells. Cover small/large layouts and font scale, portrait/landscape,
and all shipped modes. Outside-envelope synthetic probes stay visible as a
separate stress result; they must not be represented as passing accuracy tests.

Human trials require intended text established independently of the keyboard's
hit tester, with one-thumb and two-thumb entry, normal imprecision, corrections,
and conversations and essays reported separately. Include real outside-key
misses in human accuracy. Freeze assignment and scoring before evaluation; split
by participant/session or source document/conversation as appropriate. Report
counts and 95% confidence intervals (clustered by participant/session for human
data); the lower confidence bound must meet the corresponding hit-rate target.
Synthetic injection cannot certify human hit rate or digitizer performance.

## 28.3 Verification and reporting

- Record code/APK, dictionary and corpus hashes, device/OS, refresh rate, editor,
  learning/settings state, thermal/power state, cache/load condition and input
  timestamps. Test only authorized devices and restore their state afterward.
- Freeze broad conversation and essay inputs before measurement. Include full,
  initial and mixed phonetics, English words/identifiers, paced typing, rapid
  bursts, backspaces, immediate Space/selection and language switching. Separate
  warm steady typing from cold activation, dictionary loading and mode switching;
  report those lifecycle delays without hiding their effect on concurrent input.
- Collect at least 10,000 accepted key actions per mode across at least three
  sessions for latency-tail assessment; balance test order. Report count, mean,
  median, p95, p99, maximum, deadline misses and uncertainty per mode, genre,
  editor and input condition. A favorable pooled number cannot hide a failing
  group. Smaller or missing strata remain provisional.
- Separate queue/debounce time, obsolete work, dictionary lookup, merge/ranking,
  main-thread application and presentation. Test stale-result rejection and
  pending acceptance, not only isolated queries with an idle worker.
- Compare legacy Google Zhuyin on the same phone/editor with matched input
  conditions when available. Relative superiority does not waive absolute
  budgets. Include typing/correction effort and candidate stability in UX review.
- Use shared-core and pinned desktop Rime checks for language logic before Android
  builds; use device tests for input dispatch, lifecycle and visible frames.
  Every gate must be marked PASS, FAIL or NOT MEASURED. Existing callback timing
  and synthetic touch assertions do not establish visible latency or human hit
  rate. This requirements update makes no claim that current builds pass.

Research basis: [Google RAIL](https://web.dev/articles/rail) distinguishes processing
from visible response; [Deber et al., CHI 2015](https://www.tactuallabs.com/papers/howMuchFasterIsFastEnoughCHI15.pdf)
shows task-dependent touch sensitivity; [Schmid et al., 2023](https://epub.uni-regensburg.de/55007/1/text-input-latency.pdf)
finds typing effort/correction costs at higher latency without establishing an
IME threshold; [Alharbi et al., 2020](https://vvise.iat.sfu.ca/pubs/alharbi2020frustration)
shows suggestion use has an attention cost; [Android rendering guidance](https://developer.android.com/topic/performance/vitals/render)
defines refresh-rate frame deadlines. The exact budgets above are MinIME's
engineering decisions, to be validated by measured UX.

---

## 28.4 Focused-language ownership and availability

- No-personalized-learning editors must retain the active static dictionaries
  while making no personal-history reads or writes. Secure/direct and literal
  fields retain their independent conversion restrictions.
- Explicit Taiwanese/Japanese choices must use separate language preferences.
  Paired Han/POJ outputs share one candidate identity. Automatic acceptance must
  not reinforce its own ranking. Preferences may reorder candidates within
  full/incomplete groups; incomplete suggestions must not displace full matches.
  The feature must have an off switch and use the existing clear-history action.
- Learned Chinese phrases must not appear in English-secondary focused modes.
  New language phrase/grammar providers require separate validation; admitting
  arbitrary text through the Chinese phrase validator is not an implementation.
- Load only requested optional languages. Preserve admitted source rows, their
  order and source attribution through packaging. Warm switching may reuse
  previously used enabled languages; disabling a pack releases cache ownership.
  Measure cold loading and warm switching separately from per-key latency.
- During a focused language's cold load, preserve spelling and queue acceptance
  until that dictionary is available. Failure must release the queue safely.
  Loading an optional Chinese specialist pack must not block base conversion.
- Superseded prediction requests must skip remaining provider stages and stale
  delivery, including transitions to modes that issue no background request.
  Preserve serialized native work, revision checks and acceptance ordering.

---

# 29. First Development Milestone

Do not start by building a complete polished keyboard.

Build a minimal functional prototype implementing:

1. Android `InputMethodService`
2. QWERTY Pinyin keyboard
3. `zh-TW` Chinese conversion
4. composition state machine
5. literal-English candidate
6. automatic Pinyin-vs-English classification
7. context-sensitive Space
8. mixed Chinese/English acceptance tests

Use a simple candidate bar.

The milestone succeeds only when this can be typed naturally:

`這個 pronunciation 不對`

and:

`明天 meeting 改到 3pm`

without explicit Chinese/English switching.

After this behavior is verified, add:

- Zhuyin layout
- better ranking
- learning
- polished UI
- legacy-layout compatibility

---

# 30. Non-Goals for Initial Version

Do not prioritize:

- emoji search
- GIFs
- stickers
- voice input
- handwriting
- cloud prediction
- themes
- fancy animations
- toolbar features

These are irrelevant until the core mixed-language behavior works.

---

# 31. Definition of Done

The project is done only when a user familiar with legacy Google Zhuyin can type ordinary Taiwan Chinese mixed with arbitrary English words, acronyms, technical terminology, numbers, and spaces without consciously managing an input-language state.

The defining property is:

**The user thinks about the text they want to type, not which language mode the keyboard is currently in.**
