# Google Zhuyin / MinIME gap review — 2026-09-07

MinIME 0.2.1 has a working offline typing foundation, but it is not yet a close
replacement for everyday Google Zhuyin Pinyin and English use. The highest-impact
gaps are abbreviated-Pinyin acceptance and phrase ranking, English language
assistance, and composition/editing continuity. This review changes test tooling
and documentation only; production behavior and dictionaries are frozen.

## Evidence and scope

The installed Google Zhuyin 2.4.5.164561151-arm64-v8a (2451413) and MinIME 0.2.1 (3)
were exercised on the authorized Samsung SM-G781B, Android 13, using real native
touch events in a local synthetic EditText. Pinyin and English have equal priority.
The [fixed matrix](parity/matrix.json) contains 90 paired cases. Final coverage and
results are in [the comparison](parity/comparison.tsv), [structured comparison](parity/comparison.json)
and [coverage summary](parity/summary.json). Each comparison row links its raw
batch through the `evidence` field. Runner success means observation completed;
it does **not** mean parity passed.

Final paired coverage is **87/90 comparable cases**, after 12 targeted paired
rechecks (204 case executions across both keyboards, excluding rejected/diagnostic
trials). Thirty-seven comparable cases ended with identical editor text and 50
did not. Identical text is not parity: next-word suggestions, composing spans,
editor actions and panel presentation can still differ.

The three incomplete comparisons are explicit: Google language selection during
active Pinyin was not reached through the available toolbar route; its English
globe route changed to another IME; and MinIME has no English candidate-expansion
control. The last is a confirmed capability gap, while the first two limit the
reference comparison. MinIME's own single-tap switching completed in both directions.

Coverage includes full/initial/mixed Pinyin, English spelling and completions,
next-word suggestions, spaces, capitalization, vertical slides, word tracing,
punctuation holds, symbol panels, candidate expansion, cursor and selection edits,
hide/show and restart, active language switches, restricted editors, Search/Enter,
and landscape. A separate 24-case core probe measures predetermined phrase target
retrieval independently of whether Google chooses the same phrase.

No Google data was cleared, no proprietary model/assets were copied into MinIME,
and no synthetic text was sent to a contact or service. Google's existing settings
and learned state can affect ranking. MinIME preferences/learning were isolated
and restored after each batch. Each batch restores the prior keyboard and sleeps
the display. These are one-device samples, not a population accuracy estimate.

## Highest-priority findings

P1 means a substantial obstacle in the requested everyday typing flow. P2 means
a compatibility/ergonomic gap. Review priority is not a claim of data loss or a
security severity.

| Priority | Gap and concrete evidence | Likely implementation boundary / next work |
| --- | --- | --- |
| P1 | **Initials and mixed Pinyin can show the right phrase but Space still commits Latin.** `bkq` and `bukq` show 不客氣 first in both keyboards; Google Space commits 不客氣, MinIME commits `bkq ` / `bukq `. `xiex` similarly shows 謝謝 but commits `xiex `. | `IntentClassifier.classify` treats incomplete spellings as literal; `CompositionEngine.refresh` leaves the raw candidate preferred. This is distinct from dictionary recall. Define explicit Chinese-mode acceptance while preserving deliberate English entry and exact recovery. |
| P1 | **Phrase ranking and search recall are weak even with full spellings.** `womenmingtianjian` defaults to 我們明天間 in MinIME versus 我們明天見 in Google. MinIME's `zaoshh` candidates start 造時候 / 造社會; Google offers 早上好. | The current frequency scores and bounded syllable/phrase composition need a stronger contextual model and independent Taiwan usage evaluation. Do not add these individual test phrases as fixes. |
| P1 | **English correction is absent.** English mode only offers exact-prefix completions; typo recovery and contractions are not a correction pipeline. The installed Google configuration turns `teh` + Space into `tech `; MinIME preserves `teh `. Google's result is an observation, not an ideal spelling target. | `englishCompletions` has no edit-distance/spatial-error model. Exact spelling on Space was deliberately retained in the previous implementation; an automatic-correction policy still needs a product decision. |
| P1 | **Automatic capitalization is absent.** With CAP_SENTENCES, Google produced `Hello. World`; MinIME produced `hello. world`. Manual Shift/Caps Lock cannot substitute for honoring editor capitalization requests. | `EditorPolicy` does not consume capitalization flags; `MiniMeService.onStartInput` resets Shift. Add capitalization state derived from the editor and sentence boundaries without changing literal identifiers. |
| P1 | **Accepting an English completion joins the next word.** Tap pronunciation after `pronun`, then type `test `: Google gives `pronunciation test `, MinIME gives `pronunciationtest `. | `CompositionEngine.select` commits without a word-boundary state. Current settings instruct users to press Space manually. Reference parity needs deferred spacing that also handles punctuation and deletion correctly. |
| P1 | **English next-word prediction and adaptation are absent.** After `see you `, Google offers soon / tomorrow / then; after `going to `, be / have / the. MinIME shows no next word in all four probes. `CompositionEngine.refresh` explicitly excludes English when raw input is empty; candidate selection also bypasses learning in English. | English needs word/context modeling and a tested local adaptation policy. The Chinese custom dictionary currently does not supply English-mode suggestions. |
| P1 | **English completion ranking truncates before sorting.** The first 500 alphabetic dictionary matches are scanned. For `co`, the existing data would rank `could` first, but it is excluded; `in` excludes `into`, `re` excludes `released`. | This is a bounded-search bias independently proven using the same bundled data, not a claim that Google defines the correct vocabulary. Use a prefix index/top-k retrieval that preserves frequency ranking. |
| P1 | **Unfinished Pinyin cannot resume conversion after interruption.** After `nihao`, hide/show and Space produce `nihao ` in MinIME; after `srf`, restartInput and Space produce `srf `. Google discarded its private composition and inserted only a space in these probes. | `onFinishInputView` / restarting `onStartInput` call `abandon`, finishing the inline raw span and clearing conversion state. Define a deliberate resume/accept/recover policy. Do not copy Google's discard behavior by deleting editor text. |
| P1 | **Readiness and worst-case decoding remain costly.** The isolated phone benchmark constructed the dictionary in 11.35 s with about 124 MiB retained Java heap; 32 repeated initials took about 198 ms median to decode. | Dictionary loading is asynchronous, but lookup/rendering remain on the typing path. Measure key-to-visible-frame latency and improve memory/startup/search bounds without sacrificing the already weak phrase recall. Google latency was not benchmarked, so this is a MinIME performance gate rather than a measured speed ratio. |
| P2 | **Candidate presentation remains different.** MinIME reserves 90 dp for raw text, shows a horizontal row, and advances 24-candidate pages. Google has a compact candidate strip and an expanded grid. English MinIME returns at most three completions. | Separate accessible exact recovery from an efficient expandable candidate browser; measure taps/scrolls needed to reach target phrases. |
| P2 | **Word tracing and typing customization are missing.** One continuous h→e→l→l→o trace composed `hello` in Google and produced no text in MinIME. MinIME has key ripples and slide labels, but no explicit sound/vibration preference or space-bar cursor scrub. Horizontal key movement cancels a key. | `SlideKey` owns one starting key and supports vertical alternatives; general gesture typing is a different feature. Do not mistake word tracing for the required upward capital slide. |
| P2 | **Settings text is stale and English adaptation is unclear.** About says version 0.1.0 while the APK is 0.2.1. The local dictionary UI does not explain that English mode bypasses its suggestions/learning. | Fix product copy and expose accurate behavior after deciding English learning scope. |
| P2 | **Double-space sentence punctuation is absent.** `hello` then two Spaces gives `hello. ` in Google and `hello  ` in MinIME. | A spacing/punctuation state machine is needed if this shortcut is adopted. Preserve intentional multiple spaces in literal/technical fields. |

Source references: [classifier](../core/src/main/java/dev/minime/core/IntentClassifier.java),
[composition engine](../core/src/main/java/dev/minime/core/CompositionEngine.java),
[dictionary](../core/src/main/java/dev/minime/core/PhoneticDictionary.java),
[service](../app/src/main/java/dev/minime/ime/MiniMeService.java),
[editor policy](../app/src/main/java/dev/minime/ime/EditorPolicy.java),
[keyboard view](../app/src/main/java/dev/minime/ime/KeyboardView.java),
[touch handling](../app/src/main/java/dev/minime/ime/SlideKey.java).

## Punctuation, emoji and layout observations

Holding period is still a different interaction. Google opens a floating selection
popup over the letter keys; MinIME replaces the keyboard with a large punctuation
panel and needs another tap. In the recorded hold/release path without dragging,
Google committed `，` in Pinyin and `?` in English; MinIME committed nothing and
left its panel open. The exact release result is specific to that gesture path,
not a statement that every period hold inserts those characters.

Holding comma opens emoji in both. Google's page has category icons, a recents
icon and a continuous-looking multi-page arrangement. MinIME uses category and
subgroup buttons with explicit paging; the initial face-smiling subgroup leaves
much of the four-row grid unused. MinIME's extra toolbar and selectors consume
more editor space. MinIME deliberately stores no emoji recents and uses the
system font for its 3,010 pinned Unicode 12.0 sequences. This review does not
establish exhaustive glyph-by-glyph parity or the reference's complete inventory.

| Visually inspected evidence | Google | MinIME |
| --- | --- | --- |
| Pinyin period, finger held | [Floating popup](parity/runs/batch-06/parity-google-pinyin-period-hold-held-PERIOD.png) | [Full punctuation panel](parity/runs/batch-06/parity-minime-pinyin-period-hold-held-PERIOD.png) |
| English comma hold, after release | [Emoji page](parity/runs/batch-06/parity-google-english-comma-hold-step-0.png) | [Emoji subgroup page](parity/runs/batch-06/parity-minime-english-comma-hold-step-0.png) |

The requested ordinary English upward slide is working: upward `a` produced `A`
in both. Downward `q` produced `1` in both English layouts. Double Shift, type
`api usb `, unlock, type `x ` produced `API USB x ` in both; one-shot Shift
produced `Hello `. Shift followed by an upward `q` produced lowercase `q` in
Google and uppercase `Q` in MinIME, a narrower remaining gesture difference.
Ordinary `hello, world.` and explicit exact-input recovery for `teh` also matched.

## Editing, lifecycle and editor actions

The settled hide/show and restart rechecks confirm the composition difference
above. An earlier 250 ms hide/show replay did not reliably reopen the keyboard;
it is superseded by the 700 ms settling recheck and screenshots. Source inspection
also shows that external cursor movement clears owned composition/context and
that committed words are not reconstructed from surrounding text.

- Deleting selected `hello` from `hello world ` produced ` world ` in both.
  Moving the cursor to index 2 and typing `x ` produced `hex llo ` in both.
- Type `hello `, delete the space, then type `x `: Google produced `hello x `;
  MinIME produced `hellox `. Word-boundary/re-edit handling differs. This needs an
  explicit behavior decision, since literal editing can intentionally join words.
- English Enter committed `hello` plus a newline in one press in both; Chinese
  Enter committed 你好 and a second Enter inserted a newline in both.
- In a Search field with pending `nihao`, both committed 你好 on the first Enter.
  **MinIME dispatched Search (action 3); Google did not dispatch an editor action
  on that first press.** This is an intentional current policy with a reference
  compatibility difference, not a passing action-parity check.
- Password, URL, email and numeric final text matched in these short probes.
  Google still exposed completions for URL/email prefixes while MinIME suppressed
  them; both kept the tested spelling on Space. A no-personalized-learning flag
  is not a no-suggestions flag: both offered generic English completions there.
  These observations do not measure Google's learning/storage behavior.
- Both landscape rechecks typed and committed successfully after allowing the
  keyboard to settle. MinIME keeps an inline editor and wide key rows. This is
  a basic smoke check, not rotation-during-composition or long-session validation.

| Additional inspected evidence | Google | MinIME |
| --- | --- | --- |
| `bkq` before Space | [Phrase selected by default](parity/runs/recheck-01/parity-google-py-bkq-step-0.png) | [Raw input preferred](parity/runs/recheck-01/parity-minime-py-bkq-step-0.png) |
| Accept completion, continue `test ` | [Word boundary](parity/runs/recheck-01/parity-google-en-completion-space-step-2.png) | [Words joined](parity/runs/recheck-01/parity-minime-en-completion-space-step-2.png) |
| Hide/show `nihao`, then Space | [Reference editor](parity/runs/recheck-01/parity-google-py-hide-show-step-2.png) | [Raw spelling retained](parity/runs/recheck-01/parity-minime-py-hide-show-step-2.png) |
| Candidate expansion | [English expanded grid](parity/runs/batch-09/parity-google-en-expanded-candidates-step-1.png) | [Pinyin next horizontal page](parity/runs/batch-09/parity-minime-py-expanded-candidates-step-1.png) |

The candidate screenshots show different input cases and illustrate presentation
only; phrase-ranking comparisons use the matched JSON records.

## Independent phrase and ranking checks

The [24-case core results](parity/pinyin-core-results.tsv) use eight fresh phrases,
each with full, initial and mixed spellings. Target phrase retrieval was **8/24 at
rank 1 and 11/24 within five candidates**. These are small, deliberately diagnostic
probes, not a Chinese typing accuracy score. They were not used to tune this APK.

Examples that distinguish retrieval from acceptance:

| Input | MinIME target retrieval | Implication |
| --- | --- | --- |
| `bkq`, `bukq` | 不客氣 rank 1 | Space policy prevents convenient acceptance despite successful retrieval. |
| `xx` | 謝謝 rank 6 | Exists, but ranking puts it behind 學校 / 學習 / 訊息 / 相信 / 消息. |
| `zaoshh` | 早上好 absent from returned candidates | Search/model coverage failure beyond the raw-default issue. |
| `womenmtjian` | 我們明天見 rank 16 | More candidate navigation is necessary even if acceptance is fixed. |
| `qingbangwokanyixia` | 請幫我看一下 rank 9 | Full phonetics alone do not solve sentence ranking. |

Candidate visibility from Android accessibility is only the current viewport.
The paired review therefore reports visible suggestions, while the core probe
measures the returned decoder list. These counts must not be equated.

In the paired editor test, one Space accepted the predetermined phrase in **23/24
Google cases and 4/24 MinIME cases**. All 16 initial/mixed probes committed raw
Latin plus a space in MinIME. Google's exception was `zsh`, whose first choice
was 這是 and second visible choice was 早上好. This separates a naturally ambiguous
reading from a model that cannot retrieve the intended phrase. The 24-case sample
is deliberately narrow and must not be marketed as a general accuracy percentage.

The [English prefix audit](parity/english-prefix-cap.json) compares capped and
uncapped ranking with identical dictionary frequencies. Five tested prefixes
(`co`, `de`, `in`, `re`, `un`) lose higher-frequency options. Reproduce with
`python tools/audit_english_prefixes.py`; this audit changes no runtime data.

## Compatibility choices to preserve or decide explicitly

- Pinyin upward slides produce capitals in MinIME at the user's explicit request.
  Earlier live Google Pinyin observations produced lowercase literal letters.
  That difference is not a regression to remove.
- Exact English spelling on Space and English + Space inserting a real U+0020
  are deliberate current behavior. Typo correction should be an explicit policy,
  especially for names, code, URLs and identifiers.
- MinIME uses Android inline composing text and a stable exact-input slot. Google
  Pinyin keeps phonetics in its keyboard composition area. Lifecycle behavior and
  surrounding-text editing must be assessed with that architectural difference.
- Single-tap EN / 中 is a required direct language switch. Google’s globe can
  select another installed IME in the tested configuration, so it cannot be used
  blindly as a Chinese/English test setup shortcut. A diagnostic recorded the
  default IME changing to MinIME; earlier screenshots alone did not prove this.
- Voice, handwriting, cloud services and themes remain outside this independent
  implementation. Their existence in reference resources is not a live test.

## Platform and release review

Fresh validation during this review:

| Check | Result / boundary |
| --- | --- |
| Core regression | PASS, 908 assertions; 808 key events over 12 corpus cases. |
| Android acceptance | [PASS, 22 tests in 95.199 s](parity/acceptance-results.txt). These establish existing behavior, not language-quality parity. |
| Android benchmark | [PASS, 1 test in 16.784 s](parity/performance-results.txt); [raw timing and heap measurements](parity/android-performance.json). |
| Assets | Bundled asset validation passed, including 3,010 emoji sequences. |
| Build / lint / signing | Observation APK built successfully. Unchanged production lint report: 0 errors, 12 warnings. Debug signature verifies (v2); no requested permissions in APK; release manifest excludes the debug editor. |
| Cleanup and artifact identity | [Verified final state](parity/final-device-state.json): Samsung Honeyboard restored, original empty settings/learning maps restored, `mWakefulness=Dozing`, APK hash unchanged. No AOD preference was changed. |

The Android benchmark's six ordinary short probes took median 5.9–12.3 ms and
p95 7.3–16.2 ms; the sentence probe took median 17.4 ms / p95 19.3 ms. The long
repeated-initial probe took median 198.0 ms / p95 199.7 ms. Fresh-process dictionary
construction was 11,349 ms and approximate retained heap 129,706,688 bytes.
These exclude input dispatch, editor updates and rendering. The test follows a
long device session and does not control thermals or filesystem cache; its higher
times than the earlier benchmark do not prove a code regression in this unchanged
APK. They do reinforce the need for startup and worst-case latency work.

MinIME has no network permission; the service requires Android's BIND_INPUT_METHOD
permission, and application backup is disabled. Password/numeric input is direct;
no-personalized-learning fields bypass learned choices. The debug editor and
observation harness are separate from release behavior. These source/artifact
properties do not establish correctness in every host application.

MinIME targets Android 35 with minimum Android 29. Actual testing here is Samsung
Android 13. This is narrower compatibility than the supplied legacy APK's minimum
API metadata. Chrome, real messaging/terminal apps, stock Android, Android 10,
large-font settings, TalkBack, long sessions, and sustained rotation/restart stress
remain open gates. Existing local WebView tests are not Chrome-app validation.

The APK stays at 3,742,475 bytes with SHA-256
`181ade044ee202bd316c1fde4f86059d00224b645e8f7d5ebe4b584d56bcdf08`.
No production code, language data or package version was changed by this review.

## Reproduction and rejected evidence

Generate plans with `python tools/make_parity_plans.py`, build
`:app:assembleDebugAndroidTest`, then use `tools/study-parity.ps1` with the authorized
phone serial and a batch plan. Use `python tools/summarize_parity.py` after the runs.
The [review contract](PARITY_REVIEW_AUDIT.md) defines the measurement boundary.

Earlier harness trials are retained under `parity/rejected` and diagnostic
directories. Reusing/reconfiguring an editor produced invalid empty observations;
matching raw `x` instead of the lower letter key invalidated an `xx` replay;
reference language controls were unavailable while predictions occupied the
toolbar. A process-reset experiment also invalidated editor connections and was
rejected. Final runs use fresh editor instances and lower keyboard-key matching,
and record unavailable controls instead of guessing their coordinates. Only final
`parity/runs/batch-*` records and explicitly recorded successful `recheck-*`
observations enter the aggregate, with prior evidence paths retained. Rechecks
allow more time for hide/show and rotation settling and begin with Pinyin cases
to avoid using the missing toolbar as a language precondition. Diagnostic screenshots may be
stale unless backed by their JSON stage; final evidence links must match a completed
capture action or recorded failure.

## Recommended implementation order

1. Resolve abbreviated/mixed Pinyin acceptance separately from contextual ranking.
   Preserve raw recovery and intentional English entry. Add a fresh phrase holdout
   after implementing a general model/index improvement.
2. Complete English as a primary layout: editor capitalization, completion spacing,
   reliable prefix ranking, optional typo correction, next-word prediction and local
   adaptation. Keep literal-field policies and all-caps/identifier preservation.
3. Define composition continuity on hide, restart, cursor edits and language change;
   verify the chosen semantics in real apps before claiming daily-use reliability.
4. Align candidate expansion, punctuation/emoji selection and layout ergonomics,
   then close accessibility, device coverage and measured key-to-frame performance
   gates. Do not treat a passing functional suite as language-quality parity.
