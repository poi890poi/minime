# Systematic rare/common glyph investigation

Probed **424 distinct supported syllables and every proper prefix: 499 inputs**, at five layers (core, native, merge, composition without packs, composition with all packs). Fresh native sessions; no user history. The independent references are the MOE school-corpus table (4,343 common-standard glyphs) and held-out UD GSD counts (2,365 observed Han glyphs). All source/hash metadata and raw candidate lists are retained.

## Confirmed application defect

The released 0.6.2 add-on promotion rule creates **9,540 relative-order reversals** between existing whole-input glyph candidates across all 499 inputs. The corrected rule creates **0**. Native lists are byte-for-value unchanged. This is a direct mechanical invariant, not a claimed universal language-accuracy percentage.

An exact add-on duplicate previously jumped ahead solely because it appeared in another dictionary. Protecting only the leading base candidate left later common homophones exposed; English completion interleaving could hide even the leading Chinese candidate from that check. Existing exact base entries and intrinsic single-glyph whole-input results now retain their base rank. Novel dictionary entries and explicit personal choices remain separate. No character-frequency override or word exception was introduced.

Positions below exclude the separate raw-input buffer and include other-language suggestions. Examples are selected deterministically from measured reversals by MOE frequency ratio, not used to tune production.

| Input | Reversed pair | 0.6.2 positions (rare / common) | Final positions (rare / common) |
|---|---|---:|---:|
| `ta` | 獺 before 他 | 3 / 8 | 30 / 4 |
| `you` | 鈾 before 有 | 1 / 3 | 44 / 2 |
| `zhe` | 懾 before 著 | 3 / 5 | 21 / 2 |
| `bu` | 鈽 before 部 | 5 / 6 | 29 / 5 |
| `shu` | 曙 before 數 | 4 / 7 | 47 / 2 |

## Separate data issues, not silently patched

Rime’s own source frequencies differ from the independent references. For `an`, it ranks 俺 before 安; its essay counts are 17,692 versus 12,474, while MOE records 5 versus 1,121 and held-out UD records 0 versus 57. This is a source-distribution mismatch, not the duplicate-promotion bug. Other primary-reading inversions and source weights are listed in summary.json. No native dictionary weights changed.

The fallback assigns zero source frequency to **11** glyphs present in the MOE reference. 臺 is the sole one within MOE’s top 1,000 (rank 285); the fallback places it at position 55 for `tai`. The importer uses the same whole-glyph count for every pronunciation and defaults missing counts to zero. A future data change should distinguish missing counts from measured zero and retain pronunciation-specific weights; it must be evaluated on a fresh independent corpus rather than trained and scored on this diagnostic table.

A naive frequency-only reorder is rejected as an unsupported remedy. For example, 的 is globally frequent mainly under `de`, so its lower position under `di` is not evidence of a bug. The report additionally identifies whether both compared pronunciations have the highest source reading weight. MOE school usage is not universal adult conversation usage, and absence from either reference is not proof that a glyph is rare.

## Presentation and verification

The first shared-partial presentation experiment inserted up to eight incomplete matches together. Broad conversation testing showed that this displaced common choices; that design is rejected and its raw evidence is retained under docs/speculation. Final presentation previews one incomplete match early and keeps the rest after the first eight displayed alternatives. All lookup results remain reachable. This changes visibility, not dictionary coverage or source frequencies.

Generic regressions cover duplicate homophones under all four pack names, English-overlap interleaving, native glyphs absent from the fallback source, Space/visible-choice ownership, stale callbacks, and private fields. No probe example supplies production scores or eligibility. The immutable core/native data and independent reference hashes make the diagnosis reproducible.

Source: [MOE table 18](https://language.moe.gov.tw/001/Upload/files/SITE_CONTENT/M0001/PRIMARY/shrest2-18.htm), extracted by the pinned SHINE AAC generator; UD, McBopomofo and Rime source archives retain their repository licences. These references and evaluation files are not shipped in the APK.
