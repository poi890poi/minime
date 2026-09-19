# Expanded suggestion coverage — 2026-09-19

Scope: Chinese glyphs/phrases, complete spellings, initials, mixed full/initial
units, multi-letter partial units and explicit separators. No production words,
frequency exceptions or source weights were changed. This is not a parity claim.

The frozen matrix contains 315,099 labeled retrieval cases, covering all 166,181
distinct stored reading/text pairs and 12,000 hash-selected phrase families with
partial forms. Development has 157,467 cases; reserved has 157,632. Split by text
identity keeps alternate readings together. These are seen-source retrieval
contracts, not conversation accuracy or an unseen-language holdout.

## Deeper partial retrieval

The single-entry matcher retained only 24 candidates (plus length-diversity
slots), hiding many correctly matched stored words. Its state budget is 2,048.
Independent exhaustive matching over 242 frozen development queries found all
1,721 high-ranked eligible matches; this did not support rewriting segmentation.
Capacities 64 and 128 were then compared with identical scores/traversal.

Reserved results, with the 128 choice fixed before inspection:

| Condition | Cases | Target available before | After | Target in first 8, before and after |
|---|---:|---:|---:|---:|
| Initials | 6,059 | 3,334 | 4,806 | 2,697 |
| Mixed full/initial | 6,059 | 5,410 | 6,010 | 4,200 |
| Partial letters in each unit | 6,059 | 5,147 | 5,890 | 4,333 |

"Available" means the exact stored target is returned with a whole-input span;
it can require scrolling. First 8 means decoder positions, not visible phone
cells. No previously available target was lost and no first-eight sequence
changed across either corpus. Increasing capacity improves access, not top-row
ranking. Ambiguous initials still do not retrieve every source homophone.

Development lookup p95: 24 candidates 0.255 ms, 64 candidates 0.262 ms, 128
candidates 0.297 ms. Reserved 128 lookup p95 is 0.270 ms. These are uncached
desktop dictionary calls; composition, rendering and Android touch latency are
excluded. Full distributions and candidate changes are in capacity-results.json;
compressed per-case evidence is under evidence/. No phone speed claim is made.

PartialDepthRegression fails at source alternative 25 on the baseline. It passes
with 128, exercising 96 alternatives under seven spellings without constructed
text. The full core suite passes 357,085 behavioral assertions; that number is
not an accuracy percentage. The prior pinned desktop baseline covers 13,014
rows / 11,272 native queries (p95 native IPC plus decoding 4.398 ms).

## Reference comparison

The 18 previously frozen Google/MinIME cases have now been observed on
RFCR91GWXLX. Google version: 2.4.5.164561151-arm64-v8a, versionCode 2451413.
Google exposes word prefixes as well as first glyphs. Default MinIME often only
offers first glyphs when the full sentence is absent. This motivates a separate
source-backed prefix-selection change, not resuming random sentence assembly.
An unavailable requested first glyph in the collapsed row is not proof that the
glyph is absent from Google's expanded list. The runner's completion status alone
does not establish parity; detailed step outcomes must be assessed separately.

Both baseline sessions restored the prior Samsung keyboard and MinIME preferences,
verified display OFF after collection, and explicitly released the shared phone.
