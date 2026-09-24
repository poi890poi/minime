# Explicit Latin casing now reaches the complete English vocabulary

September 24, 2026. **Accept the scoped bug fix.** This does not admit the
separate lowercase or blanket-priority trials and does not clear public release.

## Problem, cause and change

A fresh title-case or all-caps prefix in Pinyin had Latin intent but could not
complete capitalized source vocabulary. IntentClassifier already treats uppercase
letters as explicit Latin input. CompositionEngine failed to pass that signal to
the existing case-folded completion index, using only prior Latin context or
English mode. A tiny real-engine regression fails on the accepted runtime before
the fix; its failure log is retained.

Pass the same explicit-casing evidence to that lookup. Existing casing validation,
source scores, limits, ordering and Space policy remain in control. No words,
dictionary entries or model weights are added. Lowercase proper-name recall and
unusual mixed-case input are outside this fix; it does not promise restoration of
internal name capitalization beyond the existing title/all-caps behavior.

## Verification

| Check | Result |
|---|---|
| Focused engine regression | Title/all-caps completion in all English-enabled modes; raw Space, private/literal fields and lowercase boundary pass |
| Source-derived dispatch audit | 8,816 eligible AOSP entries → 9,334 distinct cased half-prefixes → 37,336 mode checks |
| Completion inventory/score mismatch against English mode | 26,616 before → **0 after** |
| Space identity across the source audit | Exact before/after SHA-256 match |
| Frozen lowercase English probes | All 6,144 complete inventories/orders/Space outputs identical, excluding timing |
| Frozen Chinese chat conditions | All 4,608 inventories/spans/order/Space outputs identical, excluding timing |
| Standard shared core | PASS, 1,770,117 contract assertions; not language accuracy |
| Pinned desktop Rime evaluator | 13,014 inputs / 11,272 native queries; output bytes unchanged |
| Local non-debuggable Android build | PASS |
| Real-service phone checks | Explicit title completion plus four-mode release smoke: **2 tests pass** |
| Packaged assets | All 24 asset files byte-identical to accepted-runtime APK |

The source audit uses every eligible ASCII title-case/all-caps source word, one
fixed half-prefix in title and uppercase forms, with duplicate queries removed.
An English-only dictionary fixture isolates real engine mode dispatch. This is
source coverage of the stated contract, not all prefix lengths, semantic precision
or natural conversation accuracy. Lowercase Chinese comparisons exercise the
full real dictionaries and enabled Taiwan/geography packs separately.

The initial source-audit harness wrongly assumed Space always emits the literal
prefix. Existing apostrophe restoration produces 30 different outputs across the
mode episodes even before this change. The corrected harness compares actual
baseline/trial acceptance hashes instead; those 30 outputs and all others are
unchanged. Preserve the initial harness failure, not a false production defect.

Desktop output SHA-256 remains
`72ed6f69cf0a838ec9c03a4d8de2a3e94e5991531b344ae1f173d95bff2d6bfe`.
The source audit's input digest is
`098c1d304d0c4db2c73c4a963e0ad3918c42d26730b001e5c3e3f599f6c16989`;
both acceptance digests are
`dc491e4357f58279f58c217c2b09e16dd5b07032afbf892588443b008f1a9171`.

## Android evidence and limits

The non-debuggable test APK is SHA-256
`663b20b9256ace1c43bf8e274218274256904c1866b13a31359b1cd2552cd7c8`,
signed with the existing debug certificate for device testing, **not a Play upload
artifact**. The verified instrumentation APK is
`a9f9ec4d6e5ff18f6bbd1be08fefefdedd4cd91971e1d993526ab142f37a5e91`.

Session `855f657d-8136-460c-ae25-e794672e573b` uses the production Settings editor,
visible keyboard keys, the real IME service and packaged model. Fresh Pinyin `Lon`
exposes London; tapping commits London, while Space commits `Lon ` and finishes
composition. The existing four-mode smoke also passes. The screenshot was visually
checked; an unrelated system alert makes it unsuitable as a store-listing image.

Earlier session `98d11936-8974-4db2-942b-77c44a863505` crashed in test construction:
the first fixture referenced EditorTestActivity, which is intentionally absent
from release APKs. The test was moved to SettingsActivity; the production APK was
unchanged between attempts. No application-behavior pass is claimed for that attempt.
Both sessions restored original APK/preferences/learning/prior IME and verified
display OFF. The shared mutex and coordinating reservation were explicitly released.

These tests establish integration, not physical touch hit rate, panel latency,
Android 16/16 KB execution or Play delivery. Aggregate/log evidence is in
`explicit-latin/`; raw corpora and device captures remain in ignored artifacts.
Broader ambiguous-language ranking, source-rights migration and release gates
remain open.
