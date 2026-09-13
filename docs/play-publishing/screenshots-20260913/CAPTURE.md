# Capture and visual review

September 13, 2026. App 0.8.5-review1 (33), unchanged from the prior core,
desktop and 53-test Android validation. Its APK SHA-256 is
`9a8e5fccbfe2306b9209b79241c734bc6f2dda7b8efec5aa26824121e2c20d54`.
Only the screenshot harness and packaging tooling changed. No dictionary entries,
source weights, ranking rules or production UI were changed.

The Notes host now contains four-line authored example contexts and a visible
「多語日常 · 範例文字」label. The PNGs are unmodified 1080x1920 RGB phone captures.
The manifest separates prefilled context from keys typed into the real IME.
Scene 07 additionally types lí hó twice, taps once and holds once, and asserts
the actual editor contains `lí hó` and `你好` before typing `tosia`.

## Review

- Chinese: bilingual weekend conversation; 明天 and stored title completions.
- English: everyday messages and contractions; thank / thanks / thanked visible.
- Taiwanese: four everyday POJ/Han examples; lí hó / 你好 candidate annotation.
- Japanese: everyday greetings; ありがとう / アリガトウ choices.
- Geography: unchanged `jianianduan` input. All three visible choices are matching
  Rudy entries: 加年端社, 加年端山, 加年端溫泉. The previous generated garbage
  sequences are absent in this capture. This does not certify general coverage.
- Trails: expanded 八通關 list, including 駐在所, 古道東段/西段 and 山西峰.
- Taiwanese output: actual tap/hold results and to-siā / 多謝 annotation.
- Japanese choices: きもち / 気持ち / キモチ and everyday related phrases.

All eight views were visually inspected. Notes text and primary choices are
legible. Long trailing strip candidates are naturally clipped by scrolling;
neither pixels nor candidate order were changed to conceal this behavior.
The first `chiah` expanded view exposed a questionable Han-example choice and
is retained under `findings/`; it is excluded from the upload PNG set. This
finding remains open and is not repaired by choosing another presentation.

## Device and verification

SHINE explicitly acknowledged the exclusive RFCR91GWXLX screenshot window.
Both sessions used the shared mutex through all reads, installs, capture and
cleanup. Session `a882f56d-0fe0-4d1f-a26f-0adfb25768a8` captured the initial
eight scenes. Session `7cf10cb3-19c5-4d30-9707-9e3b81f8afcd` captured the final
set; the capture test passed in 60.991 seconds, including IME identity, the Rudy
source check and actual tap/hold editor outputs.

The original preferences and Samsung HoneyBoard IME were restored and read back.
The original viewport was restored and verified, then display OFF was verified
again. No always-on-display setting was changed. The phone reservation was
explicitly released. Logs and display evidence accompany this document.

Local packaging verifies eight image dimensions and RGB format, records SHA-256
for each payload, and checks ZIP integrity. The archive is committed to the
repository so access does not depend on an expiring Cloudflare tunnel. This is a
screenshot refresh, not a new signed release or a Google Play submission.
