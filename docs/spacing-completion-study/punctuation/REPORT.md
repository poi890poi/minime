# Punctuation follow-up

2026-09-16, authorized phone RFCR91GWXLX, installed Google Zhuyin
2.4.5.164561151-arm64-v8a / 2451413. Frozen `plan.json`, complete raw native
observations in `observations.json`. Both providers used the same local ordinary
EditText; the URL control used a URI field. Google's preferences/history were left
as installed. These are observed behaviors, not factory-default claims.

All nine paired scenarios completed (18 observed provider runs, none unavailable).
Before production changes, Google and MinIME matched at every recorded step.
The eight ordinary-text scenarios cross comma/period with explicit Space before
and after punctuation. `␠` denotes an actual U+0020 space.

| Keys | Google and MinIME output before final Space |
|---|---|
| hello , world | `hello,world` |
| hello , Space world | `hello,␠world` |
| hello Space , world | `hello␠,world` |
| hello Space , Space world | `hello␠,␠world` |
| hello . world | `hello.world` |
| hello . Space world | `hello.␠world` |
| hello Space . world | `hello␠.world` |
| hello Space . Space world | `hello␠.␠world` |
| URI: example . com | `example.com` |

Every final Space appended one literal space. No automatic post-punctuation space
and no relocation of a preceding space were observed. This agrees with the earlier
candidate-completion-then-punctuation probes. It does not establish rules for every
punctuation character, every app or different Google preferences.

Decision: preserve punctuation behavior. Adding automatic post-punctuation spacing
would be a new optional feature, not a correction to reference parity. No URL,
decimal, Chinese punctuation or explicit-space heuristic is added here.

The coordinated reservation and `tools/study-parity.ps1` mutex covered all ADB
operations. Session `eb19a156-fe7b-48ef-a4cb-ac05f87affa8` restored previous IME
and MinIME settings/learning. Both session and post-collection cleanup verified
display OFF; always-on-display settings were not changed.
