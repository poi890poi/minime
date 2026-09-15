# Candidate text and font audit

This document preserves the pre-fix failure evidence. See
[the device-readable candidate fix](candidate-text-audit/fix.md) for the subsequent
implementation and verification results.

## Scope and diagnosis

Test/tooling change prompted by the expanded Chinese candidate screenshot for
`rime`. Boxed text is an observable rendering defect; the screenshot alone does
not distinguish malformed Unicode from unsupported font glyphs. CandidateWord
assigns complete strings to TextView. The JNI bridge decodes standard UTF-8.
Neither observation establishes that every candidate renders correctly.

The audit separates three boundaries:

1. Strict UTF-8 decoding and valid display text in every TSV/text asset and the
   Play release notes/listings. Replacement characters, unpaired surrogates, noncharacters
   and unexpected control characters fail with file/line/code-point details.
2. Exact standard UTF-8, native-record and binary-model transport. Supplementary
   Han, kana, decomposed POJ, emoji joiners and variation selectors are positive
   controls. Malformed byte sequences and surrogate halves are negative controls.
3. Android's actual candidate TextView paint and fallback fonts, through
   TextRunShaper and missing-glyph ID 0 (API 31+). Grapheme clusters
   keep tone marks, emoji modifiers and selectors with their bases. Both collapsed
   and expanded candidate views are checked; a missing displayed glyph fails the
   test, even when its Unicode is valid. The expanded panel must retain each
   nonliteral candidate's exact text.

The font census covers every unique output code point in Chinese, English,
Taiwanese/Japanese add-ons, geography, paired Han forms and Japanese basics.
Source-only font gaps are reported rather than treated as proof that users see
them. Live panel probes cover all source Pinyin syllables with Java and native
Rime+Java providers, plus every prefix of the screenshot input. This is a
reproduction and systematic inventory audit, not a fresh language-quality holdout
or a coverage/accuracy benchmark. There are no production word exceptions.

The first phone run exposed a test-harness false positive: Paint.hasGlyph on a
multi-code-point POJ cluster asks whether there is one ligature, not whether all
parts can render. The positive control correctly failed. The final gate shapes
clusters with the complete text context and inspects the resulting glyph IDs.
Paint.hasGlyph remains appropriate for the census's individual code points.
The shaped audit explicitly fails with an unsupported-platform message on API
29/30 rather than claiming those devices were verified.

API references: [Paint.hasGlyph](https://developer.android.com/reference/android/graphics/Paint#hasGlyph(java.lang.String)),
[TextRunShaper](https://developer.android.com/reference/android/graphics/text/TextRunShaper),
[PositionedGlyphs](https://developer.android.com/reference/android/graphics/text/PositionedGlyphs).

No ranking, dictionaries, layout, preferences or runtime filtering change.
The shared checking helper is included only in desktop/Android test sources.
Well-formed mojibake can still be valid Unicode: these checks cannot establish
semantic correctness without independent expected text. Font coverage is specific
to the tested Android build and font configuration, not every supported phone.

## Run locally

Run `tools/test-core.ps1`, then `tools/test-desktop.ps1`. Desktop evaluation now
strictly decodes the native stream and checks every native/merged candidate and
alternate, including candidates beyond the reported first eight.

After the core checks, build debug and Android test APKs locally. Obtain the
explicit shared-phone handoff, then run:

```powershell
tools/test-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk `
  -TestClass dev.minime.ime.CandidateTextAuditTest -TimeoutSeconds 900 `
  -Reports candidate-text-audit.json,candidate-text-rime-java.png,candidate-text-rime-native.png
```

The report is written before the final assertion and pulled during cleanup even
on test failure. It separates source-only font gaps from displayed failures and
records input, provider, panel, text and Unicode code points. The standard phone
lease restores preferences/IME and verifies display OFF. A failed rendering gate
must remain visible as a quality defect; do not allowlist the screenshot glyphs
or call a successful source-encoding scan a rendering pass.

## Results, 2026-09-15

- Core: 28 files / 735,177 lines pass strict UTF-8 and text checks; transport and
  malformed-input controls pass. Existing core suite: 306,781 assertions. These
  assertion counts are regression checks, not language accuracy.
- Desktop: all 13,014 existing corpus inputs complete with candidate checks;
  11,272 uncached native queries. No malformed candidate text was found.
- Android SM-G781B / API 33: the corrected positive/negative sentinel test passes.
  The rendering gate **fails**, identifying the existing product defect. All
  427 inputs run with both providers (854 query/provider combinations). The
  133,330 checked view occurrences include duplicates and offscreen strip/grid
  entries; they are not distinct words or first-page coverage.
- Java candidates: 674 distinct unsupported glyphs, affecting 288 probed inputs.
  Rime+Java: 938 distinct unsupported glyphs, affecting 332 inputs. The union is
  938, not their sum. There are 3,716 failing view occurrences across both panels
  and providers; zero of these contain malformed Unicode.
- The `rime` reproduction contains U+207C5 and U+211B8 with shaped missing-glyph
  IDs. The pinned desktop Rime executable emits the same scalar values in strict
  UTF-8, so these characters were not introduced by JNI conversion. The captured
  keyboard image shows the same two tofu boxes. The test uses clean engine state
  without optional packs/learning; it does not claim identical full ranking to
  the user's screenshot.
- Census: 647,768 source rows / 22,162 distinct output code points; 674 unsupported
  code points in those source assets. Native Rime contributes a wider repertoire.
- APK inspection confirms the shared text-checking class is in the test APK and
  absent from the production app APK. Both phone sessions restored preferences,
  previous IME and verified actual display OFF; the reservation was released.

See [machine-readable summary](candidate-text-audit/phone-summary.json),
[test failure output](candidate-text-audit/phone-tests.txt) and
[reproduced keyboard](candidate-text-audit/rime-native.png).
The summary is generated with `tools/summarize_candidate_text_audit.py` from the
complete phone report; its SHA-256 is retained for provenance.

This change establishes detection. It does not fix font coverage or silently
remove source vocabulary. The failing Android rendering gate remains open until
a separately reviewed rendering/candidate policy resolves unsupported output.
