# MinIME requirements review — 2026-09-06

Scope: all 31 sections of PRODUCT_REQUIREMENTS.md, current source, bundled assets,
test harness, and the supplied 2.4.5 APK resources. This is an implementation
review, not a declaration of Google compatibility. The initial legacy APK install
was rejected; the user subsequently installed and enabled it successfully. Live
reference observations are being recorded separately in LEGACY_REFERENCE.md.

## Change impact before implementation

Primary change: layout redesign and a new vertical-slide input feature. Replace
the stacked Zhuyin/Latin keyboard with four rows of ten Zhuyin keys and bottom-row
ㄦ; expose Latin letters/numbers through slides. Use three staggered QWERTY rows
with alternate labels, one-letter capitalization and symbols. Keep raw candidates,
per-token intent, literal spaces, dictionaries and saved layout preference.

Related defects: non-ASCII punctuation appended to an active Pinyin token defeats
conversion; predictions survive moving the cursor after commitment; candidate
paging can display an empty extra page. Boundaries: shared composition engine,
service selection callback and candidate renderer respectively. Add observable
regressions before claiming these fixed.

Risks: taps firing as well as slides, long-press firing after cancellation,
accidental caps state, hidden alphabet access, editor replacement at a moved
cursor, and device tests altering user preferences. Verify real touch input,
accessibility alternatives, secure fields, candidate recovery, and preference
restoration. No stored-data migration or proprietary code/assets are needed.

## Requirement coverage after implementation

| PRS | Result and remaining boundary |
| --- | --- |
| 1–4, 29 | Functional offline foundation retained; live reference study now available. |
| 5–7, 16, 22 | Mixed input, exact-input recovery, per-token classification and literal spaces covered by the corpus and phone tests. Pinyin upward slides produce capitals at the user's request; direct commitment follows the reference's event boundary. |
| 8, 9, 23 | Ordinary Enter confirms then inserts a newline on the next press; explicit editor actions remain direct. Held deletion, cancellation, cursor movement and recovery tested. Reconversion is not implemented. |
| 10 | Raw/Chinese/English/phrase/prediction candidates supported. Fixed page count, scrolling, stale cursor context and misleading idle prediction highlighting. |
| 11–13 | Preserved physical-keyboard technical ASCII tokens. Onscreen punctuation accepts composition immediately, with Chinese/English defaults and width alternatives. Categorized symbol and Unicode emoji panels now included. |
| 14, 15 | Taiwan source data retained. Added source-derived first-tone metadata and enforced syllable boundaries. Pinyin initials, mixed partial/full syllables, word combination and trailing phrase completion implemented. Ranking/search recall remain below the reference model. |
| 17, 18 | No network permission, telemetry, input logs or backup. Secure fields use direct input; no-personalized-learning policy tested. Tests restore MinIME preferences, previous keyboard and put the phone to sleep. |
| 19 | Native editors, actions, URL/password/numeric fields and local WebView textarea tested on Samsung Android 13. Chrome, messaging, terminals, stock Android and Android 10 remain separate gates. |
| 20, 21 | Compact 4×10 Zhuyin plus bottom ㄦ, staggered QWERTY, literal slides, double-tap Caps Lock, symbols, hints and accessible alternatives. Explicit EN / 中 switches language with one tap and remembers the Chinese layout. |
| 24 | Bounded local context-dependent choices retained; added reading/output entry form so users need not type tabs on a phone. |
| 25, 27 | Twelve keystroke corpus cases, independent boundary probes, prediction holdouts and twenty-two phone interaction/editor tests. Current run status is in VERIFICATION.md. These do not establish arbitrary-text ranking quality. |
| 26 | Twenty-five earlier live reference sequences plus 39 Pinyin abbreviation/holdout sequences. Scoped limitations and negative results documented. Invalid coordinate-replay trials excluded. |
| 28 | Dictionary loads in the background; candidate lookup/render remain synchronous. Desktop timing measured. Phone key-to-frame p95/p99 and long-session memory remain unmeasured. |
| 30 | User explicitly expanded scope to emoji: 3,010 licensed Unicode Emoji 12.0 sequences, including families, flags and skin tones. Voice, handwriting, cloud and themes remain outside this implementation. |
| 31 | A legacy user's ordinary mixed-typing evaluation remains required; this review does not declare production completion. |

## Prioritized remaining work

The user clarified that **Pinyin and English are both primary layouts**. Version
0.2.1 enables explicit English completions, preserves caps and uses one-press
English Enter. English typo correction, next-word prediction and automatic
sentence capitalization remain gaps alongside Chinese ranking quality.

1. **P1 language quality:** broader independent Taiwan ranking corpus, typo correction
   and a stronger contextual model. Initials/mixed syllables now produce candidates,
   but short readings and sentences still expose frequency and bounded-search gaps.
   See PINYIN_PREDICTION_RESULTS.md. Do not tune individual acceptance phrases to hide this.
2. **P0 integration validation:** Chrome, messaging/terminal editors, interruption,
   rotation and restart stress on more Android versions. Current native/WebView
   success does not prove those app boundaries.
3. **P2 compatibility/ergonomics:** reference-style composition panel versus current
   Android inline composing text, landscape and large-text/TalkBack validation,
   candidate expansion ergonomics and full symbol-page arrangement.
4. **Performance:** phone key-to-visible-frame timing, cold dictionary startup and
   long-session memory. Desktop timings are not a phone latency guarantee.

The PRS's English-plus-Space rule intentionally takes precedence over the tested
legacy Pinyin configuration, which accepted tapped English without adding a space.
MinIME's editor-action policy also remains explicit: Search/Go/etc. act on the first
press, while ordinary Enter follows the observed two-step composition behavior.

Exact validation counts and artifact identity are in VERIFICATION.md.

## Review follow-through

Implemented: compact layouts and slide alternatives; cancellation and held delete;
explicit Taiwan punctuation boundary; cursor-context invalidation; bounded page
count; normal horizontal candidate scrolling; accessible slide actions and
long-press alternatives; reading/output dictionary form; app-preference restoration
and phone display-off cleanup. Prediction suggestions no longer claim to be the
Space default when the composition is empty.

Selection callbacks must only abandon state owned by MinIME. The integration
check exposed why an empty engine must not finish an editor-owned composing span;
the service now guards that boundary. Gesture testing also required waiting for
keyboard resize animation, and candidate bounds must account for viewport clipping.
These are distinguished from the production scrolling behavior, which was visually
confirmed to work.

The native field suite was extended to actual touch gestures, cursor changes,
URL/password/numeric fields and a local WebView textarea. Final run results belong
in VERIFICATION.md; earlier failed runs are retained as diagnostic evidence.

The live study expanded the change impact: first-tone Space requires preserving
source syllable boundaries, so the generated Chinese TSV now carries a fifth
column containing explicit first tones from McBopomofo readings. The loader and
regressions were updated together; no user data format changed. Direct slide
commands bypass intent inference after accepting prior composition. Ordinary Enter
confirmation is separated from editor-action dispatch.
