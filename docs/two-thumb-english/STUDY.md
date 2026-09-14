# Two-thumb English typing on a small phone

Research date: 2026-09-14. MinIME baseline: `8a4a690`.

## Recommendation

Keep familiar, full-width QWERTY as the default. First investigate overlapping
thumb contacts, correction effort, and accidental gestures. Compare an existing
alternative keyboard before developing a new letter arrangement. The evidence
reviewed here does not establish that an unfamiliar layout beats practiced QWERTY
on a small portrait phone after accounting for learning and correcting mistakes.

Efficiency means completing the intended message accurately and comfortably. Fewer
taps, shorter calculated thumb travel, and more prediction hits are intermediate
measurements; none alone establishes faster writing.

This is a documentation/design study. It changes no runtime behavior, preferences,
dictionary, dependencies, or release binary. No phone session or new performance
benchmark was conducted. Source descriptions below are research evidence, not
measured MinIME gains. Existing explicit settings remain authoritative, including
English autocorrection off and English word-pair learning off.

## What the research supports

| Evidence | Finding relevant to this decision | Limits |
| --- | --- | --- |
| [Palin et al., MobileHCI 2019](https://userinterfaces.aalto.fi/typing37k/) | In a browser transcription study of 37,370 volunteers, mean speed was 36.2 WPM. Over 74% used two thumbs. Autocorrection use correlated with higher speed; manually choosing suggestions correlated with lower speed. | Observational, self-selected users on their own keyboards. This does not prove that enabling correction or hiding suggestions causes an improvement. Writing a conversation also involves planning. |
| [KALQ, CHI 2013](https://www.pokristensson.com/pubs/OulasvirtaEtAlCHI2013.pdf) | Optimized thumb travel and alternation, including preparation by the idle thumb. Six trained users reached 37 WPM with approximately 5% error. | Designed on a 7-inch tablet in landscape, with substantial training. Its result is not evidence for copying the split layout onto a small portrait phone, nor directly comparable with another study's WPM. |
| [VelociTap, CHI 2015](https://www.keithv.com/pub/velocitap/velocitap.pdf) | Shows the potential of combining touch-location uncertainty and linguistic evidence. A 40 mm keyboard supported about 41 WPM with approximately 3% character error. | Participants used a single dominant-hand finger while holding the phone in the other hand. Recognition used a server. This is neither a two-thumb result nor an on-device latency result. Sentence decoding does not justify silently rewriting MinIME input. |

These findings favor investigating input reliability before undertaking a layout
replacement. They do not establish a universal best keyboard. Particularly,
prediction can save keystrokes while requiring a pause to inspect and select a
word. Compare elapsed time including that pause and subsequent correction.

## Existing approaches to compare

| Approach | Potential advantage | Cost or uncertainty | Decision |
| --- | --- | --- | --- |
| Familiar full-width QWERTY | Existing motor memory; direct access to every letter; little onboarding | Narrow keys and thumb overlap remain | First development path |
| Slightly adjusted QWERTY geometry | Can redistribute space toward difficult targets without moving letter order | Enlarging one target reduces another; changes can disrupt learned aim | Test one geometry change at a time |
| Split or curved QWERTY | May improve reach for some grips | A central gap consumes scarce portrait width; curvature changes reach and target shapes | Optional experiment only after measuring grip and errors |
| KALQ | Explicit two-thumb optimization | Large relearning cost; tablet evidence | Longitudinal research option, not a default replacement |
| Thumb-Key's English two-handed layout | Large targets and directional gestures offer another space tradeoff | Direction errors, gesture duration, and learning may offset fewer target misses | Try the existing implementation before recreating it |
| Typewise hexagonal layout | Alternative target packing intended for thumb use | Vendor performance claims are not a controlled MinIME comparison; unfamiliar arrangement | Secondary comparator if available |
| Continuous word swiping | Useful alternative entry method for some users | A single continuous stroke does not exploit alternating thumb taps; gesture recognition can conflict with tap drift | Preserve as a separately measured method |

[HeliBoard](https://github.com/HeliBorg/HeliBoard) provides configurable layouts,
split mode, and offline typing, making it a practical QWERTY comparator. Its glide
library is external and closed source, so a glide comparison must record whether
that component is present. The repository describes GPL-3.0 and inherited Apache
licensing; this study proposes behavioral comparison, not code import.

[Thumb-Key](https://github.com/dessalines/thumb-key) uses taps and directional
swipes with large keys; its [release history](https://github.com/dessalines/thumb-key/blob/main/RELEASES.md)
includes an English two-handed arrangement. Use that actual arrangement when
evaluating two-thumb use. Its AGPL-3.0 implementation is an existing experiment
candidate, not evidence of a speed advantage.

[Typewise's own explanation](https://www.typewise.app/blog/qwerty-killer-typewise-hexagon-keyboard-app)
describes its hexagonal arrangement and advertises speed benefits. Treat these as
vendor claims until reproduced with matched tasks, users, training, and error
measurement. This study does not propose copying its design or implementation.

For scale, an illustrative 70 mm usable width provides approximately 7 mm pitch
for ten equal columns. An 8 mm central gap leaves 6.2 mm pitch, about 11% less.
This is geometry, not a measured phone or a prediction of human accuracy. Record
physical dimensions as well as dp; shrinking an emulator does not reproduce grip.

## What MinIME already does, and what needs investigation

The following are static code findings at the named baseline. They identify
experiments; they do not establish user-visible defects.

1. **Letter overlap has special handling.** In
   [KeyboardView.java](../../app/src/main/java/dev/minime/ime/KeyboardView.java),
   `ACTION_POINTER_DOWN` calls `finishTapForOverlap` when the new contact lands on
   a letter. In [SlideKey.java](../../app/src/main/java/dev/minime/ime/SlideKey.java),
   this completes an older plain tap in contact-down order. Normal taps otherwise
   finish on release. Audit letter/Space, letter/Backspace, punctuation, Shift,
   and candidate transitions in both orders, including crossed release order.
   Measure the actual event path before changing this rule.

2. **A/L targets are already enlarged.** The home-row outer keys absorb the former
   half-key gutters with weight 1.5 while keeping labels aligned. Space's painted
   inset also does not reduce its full View hit area. A redesign justified by
   presumed empty target space would repeat an already completed change.

3. **Tap drift can interact with gestures.** Letter slides use an 18 dp directional
   threshold. English tracing also recognizes horizontal movement relative to
   key pitch. Record false trace activation and accidental caps/symbols during
   real two-thumb input. Preserve intentional slide-up capitals, slide-down
   symbols, and caps lock; do not globally suppress gestures to improve a tap-only
   metric.

4. **Ordinary spelling correction lacks tap coordinates.**
   [PhoneticDictionary.java](../../core/src/main/java/dev/minime/core/PhoneticDictionary.java)
   receives strings for `englishCorrections`, using bounded edit candidates and
   lexical evidence. A spatial correction experiment would require an explicit
   geometry/input contract, rather than per-word exceptions. Start with offline
   replay and candidate ranking; any automatic replacement experiment is opt-in.

5. **Layout and swipe geometry are coupled.**
   [EnglishTrace.java](../../core/src/main/java/dev/minime/core/EnglishTrace.java)
   contains normalized QWERTY positions. A different arrangement must supply
   matching geometry to this decoder. Changing only the visible keys would make
   trace recognition inconsistent.

Keep keyboard height fixed during typing, stable candidate identity during a
selection, literal input recovery, apostrophe restoration, and English-only
suggestions in English mode. Layout experiments must also retain accessible
actions and test screen-reader focus rather than assuming gesture-only access.

## Evaluation plan

### Establish the baseline

Use MinIME `8a4a690` with the requested defaults and recorded saved overrides.
Google Zhuyin is the user's reference for English behavior. Add HeliBoard as a
current configurable comparison. Record exact APK hashes, settings, keyboard
dimensions, refresh rate, and enabled assistance; report unmatched features.
Compare default experiences separately from matched-assistance conditions.

Phone work remains restricted to RFCR91GWXLX, with explicit shared-task handoff,
the repository phone lease, preference/IME restoration, and verified display
sleep. No access was taken for this study. Results on that one device cannot
establish performance across small phones or hand sizes.

### Freeze independent tasks before tuning

Use separately reported conversational messages and essay passages. Include
source-derived strata for contractions, punctuation, numbers, names, URLs, and
unfamiliar words. Register corpus provenance, license, hashes, roles, and splits
through [source management](../../sources/README.md) before importing data.
Split by conversation/document, not individual sentence. Existing regression
examples remain regressions, not fresh holdouts.

Include both transcription and short message-composition tasks. Transcription
provides an independent expected string; composition reveals editing and planning
cost but cannot label every different wording an error. Obtain intended-key
reference independently of the hit tester. Do not infer the intended key from
the same nearest-key rule being evaluated.

For touch models, runtime inputs may include observed coordinates, event times,
current geometry and allowed lexical/context data. Prompt text, target words,
and evaluation thumb labels must not leak into runtime scoring. A pointer ID
alone does not identify left versus right thumb.

### Change one variable

| Experiment | First question | Evidence required before landing |
| --- | --- | --- |
| Contact ordering | Are intended taps lost, duplicated, or reordered around non-letter keys? | A reproducible event trace, targeted integration regression, and corrected behavior |
| Gesture arbitration | Does natural drift activate unintended gestures? | Human traces with independently recorded intent; preserved intended gesture success |
| Geometry | Does one size or spacing change reduce correction-inclusive task time? | Paired human sessions at fixed physical dimensions; target-group accuracy and comfort |
| Candidate presentation | Do suggestions save time after inspection and selection costs? | Timed tasks with correction, selection frequency, and errors reported |
| Spatial English ranking | Can coordinates improve candidate quality without harmful substitutions? | Shared-core replay on disjoint users/sessions and fresh holdout; bounded latency/memory |
| Alternative layout | Does trained performance justify relearning? | Existing-app trial followed by repeated training sessions and retention measurement |

Counterbalance keyboard order and task sets. Use a small repeated-measures pilot
to estimate variance before choosing a larger sample; do not present a single
developer's practice curve as population evidence. Test natural overlapping taps,
release-order reversals, edge aiming, drift, rapid deletion, and interruption.
Synthetic traces establish deterministic behavior, not human hit rate.

### Report outcomes, not just taps saved

- Primary: task completion time including inspection, selection, and corrections;
  paired per-person changes with confidence intervals.
- WPM using five characters including spaces per word, alongside final character
  error rate, corrected errors, and harmful replacements per 1,000 tokens.
- Before-correction intended-key accuracy; loss, duplication, reordering, and
  accidental gesture rates, broken down by key group and posture.
- Candidate availability and selection cost; time to recover literal input.
- Comfort/fatigue, training time, and performance after a break.
- p50/p95/p99/max visible latency, stalls, and device memory. Distinguish touch
  contact-to-display, OS event-to-display, and decoder execution time.

Retain the existing [product acceptance requirements](../PRODUCT_REQUIREMENTS.md):
event-to-visible raw input p95 <= 33 ms and p99 <= 50 ms; stable suggestions
p95 <= 50 ms and p99 <= 80 ms; human intended-key accuracy at least 97% overall
and 95% per declared group. These are product targets, not achieved results or
universal perceptual thresholds. Held old suggestions are not fresh responses.

A speed proposal must meet those requirements and show a reproducible improvement
without increased final errors or harmful correction. Report failed and
inconclusive variants. Run shared-core and pinned desktop checks before building
changes to suggestion/acceptance logic; use device tests for actual event handling
and visible behavior. No GitHub CI or Actions.

## Next action

First reproduce two-thumb letter/non-letter overlap behavior against Google
Zhuyin and MinIME, extending the existing
[human-input checks](../human-input/RESULTS.md). Then run a matched QWERTY baseline
before choosing geometry or correction changes. Retain Thumb-Key as the first
existing alternative-layout trial if the user accepts relearning. No production
change or claimed speedup follows from this research alone.
