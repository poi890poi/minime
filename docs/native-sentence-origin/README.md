# Where 家碾斷 comes from

Observed September 12, 2026 while capturing MinIME 0.8.4. The raw input was
`jianianduan`; the highlighted Space choice was 家碾斷, while the attested Rudy
entry 加年端社 was third. This is a release quality defect, not a successful store demo.

## Reproduction and causal chain

Pinned desktop Rime 1.16.1 with the exact packaged model and a separate user directory
returns 家碾斷 first for `jianianduan`, consuming all 11 letters. It returns
家年度安設 first for `jianianduanshe`, consuming all 14 letters. No MinIME Java
dictionary, Rudy add-on, personalization or Android app participates in that native
query. Both full output strings are absent from the scanned base/Rime lexical
inventory. 碾斷 is an individual entry in the base dictionary; the whole output is
not an attested phrase there.

The pinned librime `ScriptTranslation` generates a `Sentence` when it finds no
reliable whole-input phrase and has at least two syllables. `MakeSentence` builds
a graph of dictionary pieces; `Poet` chooses a weighted path. `PrepareCandidate`
returns that assembled sentence before dictionary prefix alternatives. These are
deterministic phonetic guesses, not random corruption and not Rudy place names.
The shipped schema's `custom_phrase/enable_sentence: false` applies to a different
translator section; the active `script_translator` still performs this assembly.

MinIME then loses evidence needed to judge the result:

- `app/src/main/cpp/rime_jni.cpp` sends only consumed length and text to Java.
  The native candidate type, composition and confidence are discarded.
- `RimeBackend.candidates()` constructs ordinary Candidates with score `100-i`.
  Their `composed` flag defaults to false, even for an assembled Rime sentence.
- `CandidateMerge` suppresses composed *Java fallback* candidates when Rime has
  a whole-input result, but preserves Rime hypotheses. This safeguard therefore
  cannot recognize the native equivalent of the same weak construction.
- `CompositionEngine` places the first incomplete, non-focused add-on preview
  no earlier than candidate index 3, and further previews no earlier than 9.
  `jianianduan` is an incomplete reading of the stored `jia'nian'duan'she`, so
  the known name is held behind the native guess. This is an insertion policy,
  not evidence that the native output is a more likely phrase.
- The first acceptable whole-input choice remains the Space default. Consuming
  the spelling is being confused with having a trustworthy phrase interpretation.

Local source inspected: librime 1.16.1 `src/rime/gear/script_translator.cc`
(sentence construction and PrepareCandidate) and `src/rime/gear/poet.cc`.
Pinned model identity: `third_party/rime/model.json`.

## Broader diagnostic

Run `python tools/audit_native_sentence_origin.py`. The tool freezes inputs before
querying: 256 SHA-256-selected Rudy names with 3–7 explicit syllables, each with
full and omit-last-syllable readings, plus 256 existing mixed Chinese audit rows (including UD-derived words and
MOE glyph probes, not conversational utterances).
Source hashes, inputs and raw top-eight results are retained beside this report.

| Input group | Inputs | Native first result absent from scanned lexical inventory |
| --- | ---: | ---: |
| Rudy full reading | 256 | 238 |
| Rudy omit-last-syllable reading | 256 | 141 |
| Existing mixed Chinese audit inputs | 256 | 1 |

This measures lexical attestation, not semantic validity or end-to-end accuracy.
Some valid names/sentences are assembled. The data are existing production/evaluated
sources, not a fresh holdout. The native-only probe deliberately excludes the add-on
merge, whose rank-three effect is established by the actual screenshot and core rule.

## Required fix boundary

Preserve native lexical/sentence provenance through JNI and the shared Candidate
model; distinguish sentence paths from attested full or incomplete readings in
the shared ranking/acceptance policy. Keep valid multiword sentence construction,
initial-letter input, partial consumption, English literal recovery and intentional
user choices. Do not blacklist strings or globally discard all constructed sentences.

Before landing, freeze broad conversation/essay/full/partial/initial baselines and
fresh holdouts, measure candidate-page coverage, Space-choice errors and latency,
and compare one shared rule at a time. Desktop and Android adapters must carry the
same metadata; the current public C-API desktop bridge lacks candidate type, so
its transport needs corresponding evidence rather than a text-membership heuristic
silently substituted for true provenance. No production ranking fix has landed in
this diagnostic commit. Current release and screenshots remain review-only.
