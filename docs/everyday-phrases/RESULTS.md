# MinIME 0.6.1 everyday Japanese and Taiwanese phrases

The 0.6.0 scope was too narrow: Japanese used a proper-name dictionary, while
Taiwanese entries were filtered by the frequency of a Mandarin translation.
Both packs now include everyday vocabulary and expressions without a geographic
restriction. This is a source-data change; core lookup, Space defaults, base
language model, phrase learning, privacy policy and pack switches are unchanged.

## Source rules

- Japanese: add the pinned JMdict common-only snapshot. Select every entry with
  an expression (`exp`) or interjection (`int`) sense; retain source-common kana
  readings and compatible common spellings, respecting sense restrictions.
  WanaKana supplies Romanization. The source has 590 eligible entries and yields
  1,606 indexed outputs including kana, written variants and raw Romanization.
- Taiwanese: remove the Mandarin-frequency eligibility gate from iTaigi. Retain
  all source entries with 2–6 Han translation characters and <=6 POJ syllables.
  Add all compatible headwords/variants and complete short examples from the
  licensed 5,429-record beginner vocabulary, yielding 5,457 distinct outputs.
  Examples retain source wording and POJ; terminal sentence punctuation is removed.
  Examples containing unsupported syntax or more than six syllables are skipped.
- No handpicked production phrases, phrase IDs, names, aliases or per-word ranking.
  Source manifests retain versions, hashes, licences, rules and unsupported cases.

The full POJ pack now has 20,274 distinct outputs; Japanese has 5,757. These are
inventory counts, not conversational accuracy or popularity measurements. The
beginner source dates to 1956 and includes dated vocabulary. Japanese source
commonness is a dictionary annotation, not measured current spoken frequency.
Taiwan culture and Rudy geography assets retain their existing coverage.

## Verification

The new example alias conversion initially lost the second `o` in tone-marked
POJ o-dot combinations. NFD places a tone between the vowel and dot. Preserving
the combining dot before discarding tone marks fixes this generally. An independent
paired-field check against the source's original POJ input keys passes all 5,882
comparable headword/variant pairs with zero mismatches. This checks transcription,
not the naturalness of the underlying source phrases.

- Eight source snapshot hashes verified; offline asset rebuild byte-identical.
- Core: 14,184 assertions, including 411 frozen continuity inputs, 96 source probes
  and 48 everyday keys each selected through both Pinyin and English modes.
- Desktop conversational evaluation: 13,014 frozen inputs / 21,044 output records,
  all four packs enabled: zero Space/English output changes versus baseline.
  Candidate lists may gain explicit choices; this is not a language accuracy claim.
- Android build/device: debug/test/release (four ABIs) and lint pass;
  zero lint errors and 17 existing warnings. All 15 phone tests pass in 58.819 seconds,
  including real-service everyday phrase selection in both modes and buffer/height
  regressions. Samsung IME and prior preferences restored; display slept and
  `mWakefulness=Dozing` verified.

No full Japanese/Taiwanese sentence decoder or arbitrary incomplete-phrase matching
is added. Type the complete Romanized key in an ordinary text field and explicitly
select the candidate. Password/private/literal fields exclude the optional packs.
Existing switches remain off by default. Everyday phrases coexist with the previous
name/culture vocabulary; this update does not make automatic Space choices.

## Try the updated packs

Enable Japanese and Taiwanese POJ in MinIME Settings → Optional dictionaries.
The following examples were read from the final packaged asset after import;
they are demonstrations, not production selection lists or independent tests.

| Type | Candidate |
|---|---|
| `arigatou` | ありがとう |
| `ohayou` | おはよう |
| `sumimasen` | すみません |
| `liho` | lí hó |
| `gaucha` | gâu-chá |
| `boeiaukin` | bōe-iàu-kín |

Version 0.6.1 / code 14. The development-signed APK is 66,051,922 bytes;
the ZIP is 36,491,854 bytes and contains that APK only. Packaged dictionaries and
notices match source bytes exactly. Base model SHA-256 remains unchanged.
See [package hashes](package.json) and [download verification](download-verification.json).
