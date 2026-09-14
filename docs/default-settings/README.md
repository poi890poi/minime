# Screenshot defaults (2026-09-14)

Type: intentional settings-default change requested by the user.

| Setting | Default |
| --- | --- |
| Zhuyin + English layout | Off |
| Rime Pinyin | Off |
| English spelling correction on Space | Off |
| Double-Space period | On |
| Recent emoji | Off |
| English word-pair learning | Off |
| Taiwanese/Japanese candidate-choice learning | On |
| Repeated Chinese phrase learning | On |
| Taiwan vocabulary dictionary | On |
| Rudy geography dictionary | On |
| Japanese mode | On |
| Taiwanese mode | On |
| Taiwanese Han alternatives | On |
| Prefer Taiwanese Han output | Off |

Change the missing-preference fallbacks in the settings UI, decoder selection,
dictionary enablement, and both phrase-learning gates. Preserve explicitly stored
true/false values; do not run a migration or overwrite users' choices. The default
selected mode remains Chinese/Pinyin. Only Rime, phrase learning, and the four
pack defaults differ from the previous build.

Risks and scope: the original Java decoder now handles default Chinese input;
bundled Taiwan vocabulary/geography participate by default, and accepted Chinese
phrases can be learned locally. Existing private-field protections and opt-out
controls remain in place. No dictionary data, ranking algorithm, network behavior,
backup format, or release signing changes are part of this task. Historical
experiment/release reports retain their original defaults; current UI guidance and
README usage instructions must describe the new ones.

Verification: run core contracts before the Android build and replay the pinned
desktop regression. Check fresh settings, stored opt-outs/opt-ins, local learning gates, mode
availability, and existing explicit-Rime behavior in Android integration tests.

## Results

* Core contracts: 306,634 assertions passed.
* Pinned desktop replay: 13,014 input rows completed. Reference output is byte-for-byte
  identical to the preceding core revision (SHA-256
  `81fb9ff2aeee5ea7791499b038b1090e5d1ee4ae5612f0163a835ff692095f02`).
  This verifies unchanged shared algorithms, not language quality of the newly
  selected Android defaults. Host load and overlapping build work make this run
  unsuitable for latency acceptance.
* Local debug APK and Android test APK build succeeded.
* Five phone integration tests passed: the actual Settings screen matches all 14
  screenshot switches, UI overrides persist and reach the decoder/dictionary/learning
  gates, default phrase learning survives a new reader, explicit opt-out hides saved
  phrases without deleting them, mode availability remains reversible, and Rime still
  works when explicitly enabled. Legacy keyboard test fixtures now specify their
  prior configuration rather than silently depending on fresh-install defaults.
* Only RFCR91GWXLX was used, after SHINE's explicit availability acknowledgement
  and under `tools/phone-lease.ps1`. Session
  `ffc87be1-52b0-46b4-afae-e0a3eb7325d3` restored settings/learning and Samsung IME,
  verified display OFF, and explicitly released the reservation. No AOD change.

Logs are in `artifacts/default-settings/`; no release version or signed Play bundle
was changed. Explicitly stored settings take precedence on both upgrades and fresh
installs with imported preferences.
