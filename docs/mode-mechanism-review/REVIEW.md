# Review of mechanisms inherited from the four-language board

Baseline: `bed0315` (0.7.7). Review and diagnostic tooling only; no production
code, dictionary, setting or phone state changed. The current shipped modes are
Chinese/English, English, Taiwanese/English and Japanese/English. The central
problem is that Taiwanese and Japanese gained focused presentation but still use
an optional-supplement data, learning and prediction architecture.

## Findings, in priority order

### P1 — no-learning fields remove the focused language's static dictionary

`EditorPolicy.java:15` maps `IME_FLAG_NO_PERSONALIZED_LEARNING` to `privateField`.
`CompositionEngine.java:313` then empties every optional pack, even when
`literalField` and `direct` are false. Unlike Chinese's base dictionary, Taiwanese
and Japanese lose their primary vocabulary. All 32 source-derived probes (16 per
language) have focused candidates normally and zero in this state.

This is inherited optional-pack privacy policy, not a required consequence of
Android's flag. [The Android contract](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING)
requests that personalized data not be updated. It does not request removal of
static language conversion. The probe establishes the core behavior, not that a
specific installed browser currently supplies the flag. Password/direct-input
policy is a separate boundary and must remain protected.

Recommendation: separate static vocabulary availability from personalization
read/write policy. Test ordinary versus no-learning fields with identical language
results and zero persistence; retain password and explicit literal-field controls.

### P1 — focused-language selections cannot teach the IME

`CompositionEngine.java:259` excludes `supplemental` candidates from explicit
choice recording; line 45 excludes them from phrase observations. All ordinary
Taiwanese/Japanese lexical candidates still carry that flag. In the 32 source
probes, three explicit selections per word produced **zero choice writes and zero
phrase observations across 96 selections** with phrase learning enabled.

The policy was introduced in `6e6319d` to keep supplemental choices out of the base
Chinese model. That original isolation goal is reasonable, but it now disables
learning for the selected primary language. Mode-namespaced context keys added in
`5237175` do not fix the recording exclusion. Simply removing it is insufficient:
`applyCandidates` extracts supplements before the learning-based sort, then inserts
them again in source order. `PhraseSession` and `PhraseLexicon` additionally accept
only Han outputs, with Chinese-oriented reading validation and exact-key retrieval.

Recommendation: record and rank explicit choices within each language namespace;
give phrase observations a language-aware representation. Preserve private-field,
edit and source-ownership boundaries. Verify persistent preference changes and
cross-mode isolation, rather than merely checking that a candidate can be selected.

### P1/P2 — sources and duplicate records influence ranking before useful evidence

`AddonDictionary.java:47` gives every general add-on candidate score zero. It then
uses two separate 24-choice indexes per focused language (`:38`, `:94`):

| Focus | Earlier source group | Later source group |
| --- | --- | --- |
| Taiwanese | iTaigi and beginner words/examples | Taihoa general dictionary |
| Japanese | Expressions and culture/name entries | General JMdict common vocabulary |

Full matches precede incomplete matches. Within each completeness class, the earlier
source group precedes the later one; partial ties use omitted-letter penalties and
text order. This is source-category precedence, not a learned/commonness ranking.
Japanese character lookup adds another explicit tier: up to eight source-ranked
characters before lexical words of the same completeness (`JapaneseBasics.java:62`).
The user requested characters, but character-first ordering still needs evaluation.

The broad dictionaries were separated to prevent them consuming an old small
supplement budget. The focused budget expansion in `2ffb058` retained these groups.
That protects source diversity, but it can put a specialist/name alternative before
a common word without evidence that it is more likely. No corpus-based accuracy or
per-word commonness claim follows from this static observation.

Duplicate records also consume intermediate budgets. The compiler retains source
provenance rows; the runtime shares candidate objects but keeps repeated references.
`ReadingUnitIndex.java:103` takes the first 24 references before deduplicating;
`ReadingIndex.java:65` counts accepted references rather than distinct outputs.
An inventory scan found 20 terminal reading lists where this hides 27 distinct
reading/output alternatives from that stage. Other aliases can recover some later.

One-variable control: keep the same distinct reading/output candidates and their
first-occurrence order, remove only repeated identities per source tier, then use
the unchanged public lookup. Across all 43 prefixes/full keys of the affected
terminal readings, eight queries change: nine query/output pairs become reachable
and ten disappear. For example, `chin` gains `chīⁿ`; `ko` gains `kôe` and `kōe`.
This demonstrates representation-dependent output, not a successful dedup patch.
The negative results are retained in `dedup.tsv`; no control was shipped.

Recommendation: separate provenance from unique runtime lexical identities; count
distinct candidates in budgets. Then evaluate a unified focused-language ranking
using attributed frequency/context evidence and personal choices. Do not pretend
source rank, Unicode order or repeated attribution rows establish usage frequency.
Do not simply remove all limits or increase them until complaint examples appear.

### P2 — language scope restricts queries, but not loading or retained memory

`MiniMeService.java:34` requests `AddonRepository.load` when any non-geography pack
is enabled. The singleton loader reads all 323,719 add-on rows: 244,131 POJ, 43,921
Japanese and 35,667 Taiwan entries, plus paired forms and Japanese characters.
Its static future keeps the combined indexes alive. Enabling Taiwan vocabulary
alone therefore also builds the POJ/Japanese indexes; switching to English does
not release them. The Chinese/English base model is loaded independently too.

This is a verified loading path, not a new memory or startup benchmark. Earlier
value/list sharing optimizations reduced duplication but did not split languages.

Recommendation: package and load languages independently, with bounded warm
caching for fast switches. Keep source provenance and optional specialist datasets
independent. Benchmark cold activation, retained heap and switching costs before
choosing an eviction policy; do not unload/reload on every language tap.

### P2 — suggestions and acceptance wait behind one combined job

`AsyncDecoder.java:23` cancels queued work with `cancel(false)`, so an already
running obsolete lookup keeps the sole worker. It schedules the newest request
with an 8 ms delay and publishes only after all active providers finish. The core
checks the revision after work returns. English completions in a focused mode are
also assembled only once its add-on result reaches `applyCandidates`.

The atomic result was introduced in `3ae868c` to fix flicker and mismatched Space
acceptance. Keep those correctness guarantees. However, the arrangement couples a
fast secondary result and Space to the slowest current lookup and to obsolete work
ahead of it. Current raw text is composed before lookup; the ordinary touch handler
does not deliberately wait for this job. The previous phone report measured raw
frame p95 around 20–31 ms and fully observed Chinese candidate p95 104 ms; this
review does not attribute that entire delay to the worker or claim a new speedup.

Recommendation: add queue/compute/apply measurements and cooperative obsolete-query
checks at provider boundaries. Use separate primary/secondary provider ownership
while retaining one coherent displayed/default choice and stable held gestures.
Evaluate staged presentation only with acceptance/flicker tests; removing the barrier
would reintroduce the defects it protects against.

### P2 — dedicated modes still lack a phrase/continuation model

`AddonDictionary.lookup` receives raw input and enabled packs, without preceding
context. It matches one stored entry; it does not perform Taiwanese/Japanese
grammatical conversion or productive inflection. On an empty buffer,
`CompositionEngine.java:287` produces predictions only for English-only mode or a
Chinese-enabled mode. The shipped Taiwanese/English and Japanese/English modes
take neither branch. All 32 source probes finish with zero idle candidates.

That omission is not another cap that can be removed. It requires language-specific
learning/prediction providers and evidence for their coverage. English as secondary
also does not get its English-only continuation/adaptation path in these modes.

## Dictionary processing: what is still restricted, and why

| Mechanism | Current finding | Decision |
| --- | --- | --- |
| POJ Mandarin-gloss length / six-syllable headword gate | Removed in `4d0ef51`; supported Taihoa, iTaigi and beginner headwords now enter | Do not reintroduce |
| Slash/Unicode POJ input rejection | Corrected in `acafb16`; output tones and CH spelling retained | Keep fix; not a complete vocabulary claim |
| Beginner example restriction | Still complete examples of at most six syllables / 64 characters | Deliberate sentence-extraction scope; review separately from headword completeness |
| General input/output limit | 96 characters; unsupported/unaligned records rejected | Keep bounded and attributed; report omissions |
| Japanese common-only corpus/readings/spellings | Still active in `everyday_addons.py:32`; common-only is not a complete language dictionary | Audit missing coverage independently; change source admission before expansion |
| Japanese name/culture selection | `compile_addons.py:95` still filters JMnedict by work/culture/Taiwan metadata | Remnant of the original culture add-on; assess separately from everyday lexical data |
| Generated Japanese romanized output | Removed in `e390388`; romanization remains an input alias | Keep current output policy |
| POJ Han examples | Still uses Mandarin written-character familiarity, one example per phonetic, restricted Han syntax, and whole primary source fields | Display/alternate-output policy, not POJ headword admission; cannot establish Taiwanese spelling consensus |
| Han-pair alternate readings | Pair compiler does not mirror the shared POJ variant importer | Coverage risk for Han annotations; do not guess alignment or delete valid phonetics when Han is absent |
| Chinese initials heuristic | Applied across packs; 12 Japanese single-character full aliases get an abbreviated flag | Metadata cleanup candidate; exact aliases remain available, no missing-word effect proved here |

## Restrictions already removed or correctly isolated

The current `InputMode.packs` excludes POJ/Japanese from Chinese and English,
and excludes Chinese/geography from the shipped Taiwanese/English and
Japanese/English modes. `AsyncDecoder` receives `phonetic=false` in those latter
modes, so it does not run Rime/Chinese conversion for their suggestions. Internal
Chinese-secondary enum values remain for comparison, but `ModePreferences.resolve`
maps shipped 台/日 choices to English secondary.

The old one-early-partial/rest-after-eight insertion rule and base Han protection
now require `!focused(c)`. They do not bury focused Taiwanese/Japanese results.
The old shared eight-choice retrieval limit was replaced by 24 per focused index;
Chinese specialist supplements still share eight. The former Chinese-intent veto
of English completions in English-secondary modes was fixed by `fd2cb60`.
Focused defaults also override English-spelling collisions; raw recovery remains.

Other limits remain: a 512-node prefix search, a 2,048-state reading-unit search,
24 candidates per terminal stage, and 32 input characters for unit matching versus
96 at the outer composition boundary. The Chinese-derived length-diversity rule
counts output code points; POJ letters and Japanese characters are not equivalent
linguistic units. These are bounded-search tradeoffs, not proof that every missing
common word is a cap problem. Exact, prefix, initials and mixed-unit paths must be
measured separately. The 0.7.7 half-input source audit reached the first eight for
263/898 targets and retained five displaced results; it was not language accuracy.

Manual custom entries remain shared explicit overrides. They are filtered for the
English-only board, but not language-tagged for 台/日. Treat that as an explicit
migration/design decision if strict two-language scope should include custom data.

Keep literal recovery, password handling, revision checks, Space/display coherence,
candidate identity during gestures, source attribution, and disabled-language query
scope. Those are correctness boundaries, not expendable four-language compromises.

## Recommended independent work order

1. Decouple static focused dictionaries from the no-personalized-learning flag.
2. Make choice learning and its ranking language-scoped; then add appropriate phrase
   observations instead of weakening the Chinese-only phrase validator.
3. Deduplicate runtime identities and report each search-stage loss. Evaluate a
   focused-language ranking policy separately, preserving all negative results.
4. Split language loading/cache ownership; measure memory and switch latency.
5. Instrument and reduce obsolete worker work without weakening acceptance rules.
6. Audit source coverage, Han pairing and continuation/grammar capabilities as
   separate data/provider changes, using conversations and essays independently.

Each stage should be an independent commit with current mode, private-field,
learning, full/partial and switching controls. Freeze broad evaluation inputs before
ranking changes. An on/off option is appropriate for new prediction/learning
capabilities; a static dictionary disappearing in a no-learning field is a defect
to fix directly, not a feature toggle to preserve indefinitely.

## Evidence and limits

`MechanismProbe.java` uses current source rows, sorted by Java String hash, selecting
16 unique 2–16-letter readings with focused results per language. This is a mechanism
diagnostic, not a fresh holdout or test of commonness. It runs the shared core with
the packaged base model and add-on source asset; no Rime/device claim is made.
The duplicate control selects all prefixes/full keys of the affected terminal lists
before inspecting outcomes and changes only duplicate representation. No dictionary
entries or experimental code were promoted into production.

Run with Java 17, compiling against `core/build/manual`, then execute
`dev.minime.core.MechanismProbe` with the probe output directory on the classpath.
It writes `artifacts/mode-mechanism-review/probes.tsv` and `dedup.tsv`; copies and
the aggregate `summary.json` are retained here. Production asset SHA-256 is recorded
in the summary. This review did not build/install an APK or touch the phone.
