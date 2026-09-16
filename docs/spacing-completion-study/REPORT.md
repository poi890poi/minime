# Google Zhuyin auto-spacing and completion

Follow-up: [the mixed-board completion boundary is fixed](FIX.md).
[Additional punctuation probes](punctuation/REPORT.md) matched the installed
reference. [Automatic prefix-expansion experiments](completion/REPORT.md) failed
quality checks and were rejected. The original observations below remain unchanged.

Live study, 2026-09-16, authorized phone RFCR91GWXLX. Google Zhuyin
2.4.5.164561151-arm64-v8a / code 2451413; MinIME runtime revision
`8021a65ac52c926b22e87b2d633f06185cb5aed9` (debug version code 38).
No production behavior or dictionary changes were made for this study.

Two actionable gaps are confirmed: MinIME does not insert a word boundary after
an English completion selected on Pinyin, and enabling spelling correction does
not give it Google's completion-on-Space behavior. Ordinary English candidate-tap
spacing, explicit spaces, punctuation adjacency and double-Space already match.

## Observed rules

`␠` below means one actual U+0020 space. Google is observed in its installed
configuration; no factory reset or preference toggles were performed. MinIME uses
fresh study preferences (spelling correction off), with explicit correction-on
follow-ups. Reference history/personalization is not reset. These results do not
establish Google's factory defaults or behavior in every app.

| Action | Google Zhuyin | MinIME | Assessment |
|---|---|---|---|
| English: type `pronun`, tap `pronunciation` | Commits `pronunciation`, with no trailing space | Same | Match |
| Then type `test` | `pronunciation␠test` | Same | Match: deferred separator |
| English: tap `tomorrow`, explicitly press Space, type `morning` | `tomorrow␠morning` | Same | Match: no duplicate separator |
| English: tap `hello`, comma, type `world` | `hello,world` | Same | Match: punctuation cancels pending separator |
| English: tap `please`, period, type `help` | `please.help` | Same | No automatic post-punctuation space in this editor |
| English: Space after `pronun` | Highlighted `pronunciation␠` is accepted | `pronun␠`, even with spelling correction enabled | Completion acceptance gap |
| English: `thos` / `recieve` + Space | `this␠` / `receive␠` | Raw spelling by default; same corrections when enabled | Main difference here is configuration |
| English: `the` + Space | `the␠` | Same with correction on | Valid spelling retained in this control |
| English: `thank` + Space, tap predicted `you`, type `again` | `thank␠you␠again` | Same | Match: next-word taps use deferred spacing |
| English: two quick Spaces after `hello` | `hello.␠` | Same | Match |
| Pinyin: `meeting` + Space | `meeting` | `meeting␠` | Different acceptance contract; already documented in LEGACY_REFERENCE |
| Pinyin: accept another `test` after that | `meeting␠test` | `meeting␠test␠` | Google supplies the separator when accepting another English word |
| Pinyin: select completion `pronunciation`, then accept `test` | `pronunciation␠test` | `pronunciationtest␠` | Missing completion boundary in MinIME |
| Pinyin: accept English then Chinese | `meeting你好` | `meeting␠你好` | Consequence of trailing-space policy |
| Pinyin: Chinese then English | `你好meeting` | `你好meeting␠` | Google does not surround Chinese/English boundaries with automatic spaces |

Google keeps Pinyin composition in its own keyboard area, while MinIME displays
inline composition in the editor. Intermediate editor text therefore cannot be
compared as if both had already committed the same word. The Pinyin comparisons
above use the acceptance step. Both preserve an explicitly typed Space after a
Chinese phrase. The `ma` probe produces a different candidate in MinIME; that is
an observed ranking/intent difference, not an auto-spacing result.

## Backspace is stateful

The first Backspace after a committed/corrected word in Google cleared the
prediction strip without changing the editor text. The second Backspace after
`thos` → `this␠` or `recieve` → `receive␠` restored the original spelling. After
an explicitly tapped `because`, the next two Backspaces produced `becaus`, then
`becau`. During live `hello` composition, Backspace immediately produced `hell`
and `hel`, confirming that the delete key itself was delivered correctly.

MinIME edits immediately: its first Backspace undoes an automatic correction,
or removes a character from an explicitly selected completion. Google's first
prediction-dismissal step should not be copied automatically; it can feel like a
missed key. Double-Space deletion was also observed, not inferred: Google first
dismissed predictions, then deleted the trailing space, then the period; MinIME
started by deleting the trailing space.

## Editor boundaries and unavailable cases

- With `NO_SUGGESTIONS`, Google did not expose the requested completion; MinIME
  retained it. This is consistent with MinIME's deliberate manual-suggestion
  support for Keep-style fields. No spacing comparison is possible after the
  missing Google candidate tap.
- In the tested URI field Google offered `pronunciation` and subsequently inserted
  `pronunciation␠test`; MinIME offered no completion. Keep MinIME's URL/literal
  policy unless separately justified; ordinary-text behavior is not a safe URL rule.
- Both typed `name@example.com␠` through explicit email-field actions.
- The Google English → Chinese shortcut probe hit its bottom globe and changed
  the active IME to MinIME. The provider guard rejected the rest of that sequence;
  it is not evidence of Google's post-switch spacing.

## Proposed changes, not implemented

1. Share an English-word boundary policy across boards. After an explicit English
   completion is accepted, keep a pending separator. Resolve it once the next
   committed segment is known to be English, consume it with an explicit Space,
   and suppress it before Han text or punctuation. In Pinyin, do not insert based
   merely on the next Latin keystroke: those letters may become Chinese.
2. Evaluate a separate rule for accepting a highlighted prefix completion on Space.
   The reference promoted `pronun` to `pronunciation` and `hel` to `hello` in these
   probes. That is not proof that every prefix should be expanded. Freeze broader
   ambiguous-prefix/valid-word/typo data and benchmark the shared core before
   changing default acceptance. No per-example promotions or dictionary tuning.
3. Preserve the existing double-Space and punctuation behaviors that already match.
   Treat changing Pinyin's English trailing-space contract as an explicit product
   decision: the original requirement intentionally retained that trailing space.
   Keep immediate correction undo unless a separate UX decision changes it.

The current ownership boundary is `CompositionEngine.selectChoice`: it sets
`completionBoundary` only for English mode. `resolveCompletionBoundary` reacts to
the next letter/digit, suitable for pure English but insufficient for Pinyin's
unresolved Chinese/English composition. Simply removing the English-mode guard
would risk inserting spaces before Chinese phrases. The missing-boundary fix
needs to use the next accepted segment's language, not a broad keystroke heuristic.

## Evidence and limits

The frozen plan has 24 scenarios, followed by seven explicitly evidence-driven
follow-ups. Across both providers, there are 68 recorded attempts including three
Pinyin reruns. Of the final 31 paired scenarios, 28 completed on both providers;
three have an unavailable provider/action as described above. These counts are
coverage of a behavior study, not dictionary accuracy, prediction hit rate, or
human typing-performance measurements.

[Detailed traces](TRACES.md), [summary](summary.json), [frozen matrix](matrix.json)
and `runs/*/observations.json` preserve text, literal spaces, composing spans,
selection, visible candidates, provider identity and rejected attempts. The
matrix SHA-256 is `fea7b86b2bcadee97ad94bbc6a3e7d104f1133cf5c6b10ee763a434de20bf4ec`.
[Google before Space](evidence/google-completion-before-space.png) and
[MinIME before Space](evidence/minime-completion-before-space.png) show their
actual highlighted choices with correction enabled in the MinIME follow-up.

Three initial Pinyin cases failed at setup because prediction UI hid the top
language shortcut. A retry succeeded with the exact provider verified at every
action and snapshot. An exploratory globe fallback was removed after another
probe demonstrated that the globe can leave Google entirely; it is not a reliable
same-provider language selector. The unavailable records remain in batch-02.
The runner now invokes only `testPairedObservations`, excluding the unrelated
uncommitted typography study. Its correction-on plan option is test-only.

All five phone sessions used the acknowledged reservation and shared mutex.
Previous Samsung IME and MinIME settings/learning were restored with readback;
actual display OFF was verified after every session and evidence collection.
Final cleanup: `artifacts/spacing-completion-study/followups/display-after-collection.txt`.
Reservation explicitly released. No Google preference was toggled; observations
can still influence its existing personalization. No reference code/model is
copied into production. No hosted CI, production edits or new release were made.
