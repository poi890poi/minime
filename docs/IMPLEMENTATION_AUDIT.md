# Implementation audit

Type: new feature, with Android packaging and regression tooling. No existing app or data is being migrated.

The shared core owns raw composition, phonetic lookup, per-token intent, candidate ranking, learning and commit/space rules. The Android service owns editor lifecycle, selection, secure-field policy, rendering and editor actions. Network access and Accessibility services are absent.

Primary failure paths: a Latin token is mistaken for Pinyin (raw candidate is always first in the strip, highlighted default is separate); cursor movement invalidates composition (finish and reset without replacement); a secure field opens (direct input, no candidates/history); asynchronous dictionary startup finishes during typing (refresh the current composition only).

Verification: deterministic keystroke corpus, broad dictionary inspection, JVM core checks, Android compilation/lint, InputConnection integration tests, visible Samsung smoke tests where available. No claim of legacy behavioral equivalence or phone latency without corresponding observation.

Implementation evidence: the shared punctuation boundary needed to delay committing leading ASCII syntax; otherwise `.ming` became `。明` after Chinese. Explicit Pinyin apostrophes also need to constrain parsing rather than disappear during normalization. Both corrections are covered by observable regressions. Phone testing distinguished test-shell faults from production deletion behavior. Detailed results and remaining gates are in VERIFICATION.md.

User-requested reference guidance reviewed: shine_aac AGENTS.md, PROJECT_CONSTITUTION.md, AGENT_ONBOARDING.md, AI_AGENT_GUIDE.md, EN_US_DICTIONARY_REPORT.md and ZHTW_DICTIONARY_INVENTORY_REPORT.md (main, 2026-09-06). Apply core-first tests, source-based dictionaries, cheap repair, stable layout and visible verification. AAC scanning and WebView architecture are specific to that app and do not replace Android InputMethodService here.
