# Conversation data decisions, before evaluation

The user requested stronger Japanese and Taiwanese conversational tests during the
frozen proposal experiment. Add a separate corpus; do not overwrite the original
21,598 conditions or select new cases from proposal outputs.

## Japanese: admit for evaluation only

- **RealPersonaChat**, Nagoya University dialogue group; pin
  `28d0b6b3865b29cabc26c230a2db37cdf315e937`, CC BY-SA 4.0.
  Actual human casual text conversations, public release anonymized by authors.
  Preserve conversation, turn and speaker boundaries; omit persona/profile and
  timestamp fields. [Source](https://github.com/nu-dialogue/real-persona-chat).
- **ASDC**, Megagon Labs; pin `f37d6575c73602adcb8ae3687520726793baa672`,
  CC BY 4.0. Human customer/operator role-play about accommodation. Narrow domain,
  more formal than casual chat; report separately. Use main original dialogues,
  not supplemental rewritten SCUD utterances. [Source](https://github.com/megagonlabs/asdc).
- Freeze source conversations by salted SHA256 order. Schema-inspected conversation
  00001/001 is excluded from the new holdout. Use 32 development and 64 holdout
  conversations per source. Split by conversation, not utterance; speakers may
  recur, so this does not establish speaker-independent generalization.
- Original kana-only spans have directly checkable orthography. Kanji readings
  from SudachiPy 0.6.10 / SudachiDict_core 20250129 are **silver annotations**, not
  human-verified gold. jaconv 0.4.0 supplies independent kana-to-Roman input. Neither
  tool is a MinIME dependency or production dictionary. Record OOV/unreadable
  tokens, preserve original surface forms, and never replace them with lemmas.
- Keep entire ordered turns for later stateful evaluation. Evaluate word tokens,
  adjacent multi-token spans and entire Japanese clauses separately, with complete,
  prefix, incomplete-unit and deterministic mistyping conditions. No engine output
  may determine whether a reference is eligible.

## Taiwanese: improve usable everyday-utterance tests; retain the access gap

- **媠聲一千 / SuiSiann-TsitTshing** is separately licensed CC BY-SA 4.0,
  unlike the restricted packaged speech transcriptions discussed below. Pin
  `242cf1ef7ce4679e7301f08353bc054791b79840`; import all compatible aligned
  `_hanlo.txt` files, with file-level development/holdout splits frozen by SHA256.
  The authors supply modern everyday examples, questions, negation, illness,
  comparisons, service announcements and loanword contexts. This is authored
  situational text, not recordings of spontaneous multi-turn conversations.
  Convert supplied Tâi-lô syllables to POJ using Taiwanese Language Tools 1.1.1 /
  KeSi 1.6.0, validate syllables and round trips, preserve original readings in
  the reference export, and report conversion failures. No Han-to-reading inference.
  Its loanword list acknowledges iTaigi/MOE lineage; report lexical overlap rather
  than claiming every word is new. [Source](https://github.com/SuiSiann/SuiSiann-TsitTshing).
- **A Basic Vocabulary for a Beginner in Taiwanese** (Ko Chek-hoàn and Tân
  Pang-tìn, 1956), digitized by the ChhoeTaigi community, CC BY-SA 4.0; reuse the
  existing pinned CSV and its attribution. Extract authored complete POJ examples,
  including longer examples excluded from the production six-syllable limit, and
  their actual word sequences. Keep CH/chh, tones, nasalization and neutral-tone
  separators; derive keyboard forms mechanically. Do not write new example text.
- This is a dated teaching source, **not spontaneous multi-turn conversation**.
  Production shares its headword/short-example lineage. Mark every exact target
  overlap, separate unseen full examples from shared-source words, and call the
  entire source regression/development data. Splitting pages cannot make the book
  independent. Mandarin/English glosses must never supply Taiwanese readings.
- **Tsay/TAICORP** is genuine family conversation in Chiayi. Official access now
  returns an authentication page, not transcripts. Ground rules also require
  noncommercial use and prohibit unapproved external processing. No transcripts
  were obtained or uploaded. Retain as access/rights hold; no numerical coverage.
  [Corpus](https://talkbank.org/phon/access/Chinese/Taiwanese/Tsay.html),
  [rules](https://talkbank.org/0share/rules.html).
- **SuíSiann** has useful modern situations and parallel readings, but its
  transcription license restricts use to speech synthesis. Do not repurpose it
  for this IME benchmark without suitable permission.
  [Publisher terms](https://suisiann-dataset.ithuan.tw/).
- **咱來學臺灣台語**: 840 authored everyday sentences with source readings; accessible
  CSVs were acquired for source audit. Redistribution/adaptation rights are not
  established from the GitHub repository, so no benchmark import yet. Its sentence
  collection must not be labeled recorded conversations.
  [Repository](https://github.com/Taiwanese-Corpus/Lan-Lai-Oh-Taigi).
- **TaigiSpeech** supplies genuine speech intent data, but intent labels do not
  provide verified POJ typing references or multi-turn conversations. Retain for
  further annotation audit, not automatic conversion from Mandarin labels.
- Taiwan-branded Mandarin corpora, synthetic dialogue scripts, mainland MinSpeech
  labels and unlicensed subtitle collections do not fill the Taiwanese POJ gap.

## Boundary

No source in this document is added to production. Evaluation exports include
source IDs, version/hash, conversation/turn identity where present, genre,
reference status, production overlap and exclusion reasons. Derivatives retain
the relevant attribution and share-alike terms. These tests measure source text
retrieval and input robustness, not community spelling consensus, physical touch
accuracy or the quality of responses generated by a chatbot.
