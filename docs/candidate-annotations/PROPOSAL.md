# 台 / 日 candidate annotations — research and proposal

Date: 2026-09-09. Research only; no production dictionary or Android behavior changed.

## Recommendation

Use one candidate cell with **phonetics as the main line and one familiar
Han-character example as the secondary line**. Keep POJ, including `ch/chh`, in
台 mode and kana in 日 mode. Tapping anywhere in the cell, or accepting the
highlight with Space, inserts the main line. The secondary line is an aid to
recognition. It does not silently change the output to characters.

One example means one attested written word/phrase per complete reading, not
one isolated character per syllable. Group identical complete phonetic outputs
so spelling variants do not consume multiple candidate slots. Preserve tone,
nasalization, vowel distinctions, dialect variants and word boundaries. A
tone-free lookup key is not a sufficient grouping identity.

Prefer familiar, attested character spellings. Do not manufacture a
"controversial Taiwanese character" blacklist. No sufficiently systematic
community-disagreement dataset was established in this survey. The requested
disagreement exclusion therefore remains unimplemented unless suitable evidence
is acquired. Frequency, official recommendation, an alternative spelling and
community consensus are four different facts.

## What existing IMEs do

This survey inspected published documentation, source code and a developer
screenshot. It did not install or test these IMEs on the phone.

| IME | Verified design | Useful for MinIME |
|---|---|---|
| PhahTaigi Android | Candidate renderer draws main and hint lines. Romanization-first mode uses POJ/TL above Hanji; Hanji-first mode reverses them. The selected primary form is committed. | Closest fit: two representations, one candidate, clear output priority. Its width is the maximum of both lines, so copying that width rule could reduce MinIME's first-row coverage. |
| 拍台文 / Phah Tai-bun for Rime | Documents full-reading candidate comments, optional tones, POJ/TL input, and remembered Han-lo/full-roman output settings. | Preserve reading annotations and separate orthography from output choice. Audit source licensing and editorial rules separately before considering its large dictionary. |
| 信望愛 / FHL | Documents separate Han-lo and full-roman modes, POJ/TL selection and ruby output support. | Output format should be explicit; ruby/combined output is a separate optional feature, not what a normal annotated candidate tap inserts. |
| Taigi AI Labs | Developer screenshot shows Han forms and readings side by side, including separate 台北/臺北 candidates with the same reading. | Confirms the recognition benefit. MinIME should collapse those into one reading candidate when the output is phonetic. |
| Apple Japanese input | Kana input offers conversion candidates; some candidates expose additional dictionary information through a book indicator. | Show a small useful hint immediately; keep longer explanations in expanded details. Ordinary Japanese conversion and phonetic output should not be confused. |

Sources:

- [PhahTaigi renderer, pinned revision 3bd9d33](https://github.com/PhahTaigi/PhahTaigi_Android/blob/3bd9d33e1465d03f037c74183a8491be4f2a4534/app/src/main/java/com/taccotap/phahtaigi/ime/candidate/TaigiCandidateView.java#L329).
  Its [developer Play listing](https://play.google.com/store/apps/details?id=com.taccotap.phahtaigi)
  also documents tone-optional input and initials for multisyllable words.
- [拍台文 project documentation](https://github.com/soanseng/rime-phah-taibun).
- [FHL product documentation](https://taigi.fhl.net/TaigiIME/).
- [Taigi AI Labs developer page and screenshot](https://ailabs.jp/zh-TW/keyboard_lp_macos).
- [Apple Japanese candidate-window documentation](https://support.apple.com/guide/japanese-input-method/use-the-candidate-window-jpim10262/mac).

The [MOE IME](https://language.moe.gov.tw/material/info?m=a1c64194-23d9-433e-8782-8550080788d2)
also separates spelling/composition from a highlighted conversion choice and
documents continuous input and completion. This is a useful interaction reference,
not evidence that its selected orthography reflects agreement across communities.

## Layout alternatives

**A. Two lines within the existing 48dp candidate row — recommended.**
Keep the phonetic output prominent, with a smaller `例 …` hint underneath.
Use the existing whole-cell touch target. Reserve the secondary baseline even
when a hint is unavailable; never toggle row height. Try a 20sp main line and
13sp hint, with font padding measured on Android so POJ tone marks are not cut
off. Keep the cell width determined by the phonetic output; truncate only the
hint in the collapsed row. An expanded view can show the complete example.
The same hint is attached atomically to its candidate; it cannot arrive later
and resize/reorder the row. Large accessibility font scales need explicit tests
and a phonetics-only collapsed fallback if both lines cannot fit.

**B. Inline `phonetics · characters`.**
Easy to scan on a desktop or in an expanded list, but consumes more horizontal
space. The previous mode benchmark already showed the cost of reducing the
usable candidate width. Do not make this the default phone row without measured
coverage evidence.

**C. Phonetics in the row, characters only after expansion.**
Preserves the current typography and density, but requires an extra action to
recognize every unfamiliar reading. Suitable as an annotation-off fallback,
not the default for this request.

Illustrative, source-checked pairs include `chhia / 車`, `chhiáⁿ / 請`,
`chhiáⁿ-mn̄g / 請問`, `kám-siā / 感謝`, `おねがいします / お願いします`
and `だいじょうぶ / 大丈夫`. These are design examples, not a production allowlist
or evidence of candidate ranking. The Taiwanese examples are in the inspected
MOE-derived snapshot; its distribution terms need a source review before any
new production import.

## Dictionary audit

The source inspection is an inventory, not annotation correctness or usage
accuracy. Counts use current pinned MinIME assets; exact Taiwanese joins normalize
Unicode NFC but do not erase tones or guess segmentation.

| Existing material | Measured availability | Implication |
|---|---:|---|
| All current POJ outputs | 14,951 / 20,274 have an exact reading in iTaigi with a Han-containing Han-lo field | About 73.7% have potential examples before familiarity/editorial checks. |
| Beginner vocabulary | 464 / 5,163 match that pool | Only 9.0%; existing iTaigi alone does not cover everyday needs. |
| Beginner short examples | 0 / 295 match that pool | Do not borrow a headword label or Mandarin sentence translation as a supposed Han spelling of the whole phrase. |
| Current Japanese expression/interjection source | 366 / 616 common readings have a compatible common Han-containing spelling | Useful metadata already exists, but many readings should remain kana-only. Counts precede MinIME's ASCII alias filters. |
| Full pinned JMdict common snapshot | 22,637 records, of which the current expression/interjection rule selects 590 | General everyday nouns, verbs and adjectives are mostly outside that rule. Expanding by source metadata is a better approach than adding examples individually. |

The group counts overlap; they must not be summed. The Japanese source has 167
common expression readings with a `uk` (usually kana) sense. That flag is
sense-specific and does not mean every sense of the reading lacks a usable
Kanji example.

**Taiwanese:** the existing iTaigi source preserves both `HanLoTaibunPoj`
(Taiwanese written form) and `HoaBun` (Mandarin gloss). Current runtime candidates
retain only the output string, so those distinctions disappear before display.
The beginner source provides Mandarin meanings and POJ examples, not an aligned
Han-lo field for every example. Retain typed source relationships in a separate
annotation asset; do not infer Han spelling from `HoaBun`.

**Japanese:** retain JMdict/JMnedict reading-to-spelling restrictions, commonness
and unusual-spelling flags. Prefer an attested common form such as `食べる` over
a source-marked uncommon spelling such as `喰べる`; this follows metadata, not an
exception list. Do not force Kanji onto readings normally written in kana merely
to fill a subtitle. Keep Japanese forms unchanged; no Chinese-script conversion.
The [JMdict schema](https://www.edrdg.org/jmdict/jmdict_dtd_h.html) explicitly
distinguishes reading restrictions, spelling information and priority.

## Familiarity and disputed forms

For each exact complete reading, collect attested examples and apply a general
selection policy at build time:

1. Require a valid whole-reading relationship. Preserve word/sense restrictions;
   do not assemble an example from unrelated per-glyph readings.
2. Prefer ordinary source spellings over explicitly archaic, uncommon, irregular
   or erroneous forms where those flags exist.
3. Rank eligible examples by measured usage of the whole spelling and character
   familiarity from an attributed, language-appropriate corpus. A corpus-derived
   familiarity signal affects the **example**, not the phonetic candidate rank.
   Use document dispersion so one repetitive article cannot dominate. Do not
   treat the number of dictionary entries as usage frequency.
4. Keep one stable example per complete reading. Frequency ties can retain
   source/editorial order or an opaque stable ID; Unicode character order does
   not stand for familiarity. The chosen example must not cycle while typing.
5. If no credible familiar example is available, leave the hint blank and retain
   the phonetic candidate. Do not promote a rare spelling to fill the space.

Candidate spelling and glyph frequency need separate evidence. A familiar set
of individual characters does not make an unusual spelling common. Conversely,
Mandarin frequency alone would unfairly penalize normal Taiwanese written forms.

Systematically obtainable metadata was found, but it does not establish consensus:

- The inspected ChhoeTaigi MOE-derived snapshot has **24,608 rows**, **2,230** with
  an alternative-Han-spelling field, and **993** with `(替)` in its POJ field.
  These describe variants and substitute-character analysis. They are not
  disagreement labels. The [MOE editorial guide](https://sutian.moe.edu.tw/und-hani/piantsip/piantsip-thele/)
  describes 異用字 as other spellings in literature and 替 as borrowing a character's
  shape or sound. Neither should trigger automatic exclusion.
- Source recommendations can be recorded as `recommended_by_source`, not
  `community_approved`. Agreement between dictionaries copied from one ancestor
  is not independent community agreement. Absence from a dictionary is not dissent.
- The [NAER Taiwanese corpus](https://tggl.naer.edu.tw/) is a relevant usage-audit
  lead; bulk access, redistribution and a suitable sampling plan were not
  established here. [iCorpus Han/TL](https://github.com/Taiwanese-Corpus/icorpus_ka1_han3-ji7)
  is a news corpus with supplemented characters; it needs alignment and licence
  review and cannot stand in for conversations or community consensus.
- [NINJAL BCCWJ frequency lists](https://clrd.ninjal.ac.jp/bccwj/freq-list.html)
  include genre-specific Kanji frequencies. Their stated research/education use
  terms need review before production redistribution; they are a research lead,
  not an approved new bundled asset.

Accordingly, do not ship a switch promising to remove "disputed Taiwanese
characters" at this stage. It would imply evidence we do not have. An annotation
visibility setting per language is useful and accurately describes what it does.

## Source and implementation boundaries

Reuse the source-management framework. Before new data enters production,
register the exact snapshot, licence, field meanings, editorial policy, lineage,
coverage and role. iTaigi's existing snapshot is CC0; the beginner data and
JMdict derivations have their own attribution/share-alike obligations. ChhoeTaigi
is an aggregator with differing per-dataset terms, not one universally reusable
licence. In particular its inspected MOE-derived dataset is labelled CC BY-ND
3.0 TW in the [upstream attribution](https://github.com/ChhoeTaigi/ChhoeTaigiDatabase).
It is a schema/reference source here, not a proposed unreviewed derivative import.

Recommended independent implementation slices:

1. **Annotation plumbing and layout:** immutable `commitText`, `readingId`,
   optional `exampleText` and source reference; preserve existing ranking and
   Space behavior. Annotation-on/off settings for 台 and 日. Store a reference
   to shared metadata instead of copying annotations into every lookup alias.
2. **Reading grouping:** merge alternative displays of the same phonetic output
   before candidate limits are applied. Preserve existing selectable outputs in
   explicit expanded alternatives if needed; do not silently turn a Kanji-output
   choice into a kana-output choice. A future output preference can choose POJ,
   kana, or normal written forms, but annotation and output format remain separate.
3. **Attributed dictionary improvements:** recover paired metadata from approved
   sources; audit new Taiwanese everyday sources and broaden Japanese common
   vocabulary through general metadata rules. Merely adding Japanese verbs does
   not provide conjugation or a full Japanese sentence-conversion engine.
4. **Common-example selection:** land only after its frequency source and broad
   sample audit support the stated familiarity claim. Keep the disagreement
   filter absent unless explicit, systematic evidence becomes available.

Precompute examples during dictionary compilation. Runtime should query only the
active mode and attach the example in the same result batch as its phonetic text.
No network calls, corpus scans, metadata joins or secondary asynchronous display
updates while typing. More source rows should not multiply visible candidates
for the same reading.

## Acceptance evidence

Run the shared core before building Android. Test missing hints, exact and partial
readings, tones, combining marks, homophones, source conflicts, kana-only entries,
stale callbacks, mode switching and acceptance. Verify that toggling annotations
does not change candidate order, output, consumed prefix or learned preference.

Freeze a broad source-stratified annotation audit and independent conversation /
essay inputs before reviewing results. Report matched-reading validity, annotation
coverage, rare-character exposure, duplicate-reading occupancy and missing data.
No automatic count establishes community consensus. Keep seen-source retrieval
separate from natural text and from human recognition testing.

Repeat the existing first-row / first-page coverage benchmark using actual Android
two-line cell measurements; matching the outer dimensions alone is insufficient.
Measure annotation-off/on under the same candidate outputs before changing the
dictionary. Then evaluate dictionary enrichment independently. Include callback
latency, frame/render cost, allocations, load time and memory, plus large-font
layout and touch tests. A recognition benefit remains a hypothesis until tested;
the mockup does not prove it.
