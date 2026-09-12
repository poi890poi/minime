# 0.8.4 phone capture and review

September 12, 2026; production source c0f6a60 with screenshot-harness-only changes.
The debug-only Notes editor hosted the real MinIME service. All five screenshots
are unmodified phone captures at 1080x1920. Input examples are authored marketing
demonstrations, never evaluation/training labels. No dictionary tuning took place.

The first attempt had no visible keyboard. The second detected Samsung's fallback
IME and failed the identity assertion; both were excluded. The final harness waits
for initial dictionary readiness and selects MinIME after attaching/interacting
with the replacement editor. All five captures passed the selected-IME assertions.
Preferences, prior IME and viewport were restored; display OFF verified after every
session. Device ownership was explicitly released to the other project.

Visual review found a real defect in 05-geography.png: 加年端社 appears third after
the meaningless full-input sequence 家碾斷. The user independently flagged it.
The screenshot is retained as evidence, not approved for Play publication. See
../native-sentence-origin/README.md for direct Rime reproduction and shared ranking
cause. Do not crop, retouch or select a different demonstration to conceal it.

Other observed candidates: 明天, English hello, Taiwanese lí hó with 你好,
Japanese ありがとう and アリガトウ. The last Japanese preview is clipped by the
scrollable strip; alt text should describe only clearly visible candidates.
Store kit is review-only until candidate quality is corrected and images recaptured.
