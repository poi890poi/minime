# Production contract fixes

Baseline e73dcec / app code d353be4. Implement two independent changes supported
by the frozen language-contract benchmark. No dictionary, ranking policy, layout,
cache or provider algorithm changes are included.

1. Bug fix: lowercase-Pinyin syntax incorrectly determines whether a consumed
   prefix is partial. Valid Japanese uppercase/hyphen prefixes consequently lose
   unfinished input, may become defaults, and partial learning bypasses focused
   namespaces/settings. Make consumed offsets raw UTF-16 boundaries independent
   of language. Validate every provider's spans before display/acceptance, reject
   malformed offsets and literal prefixes, and use the same learning policy for
   partial and whole choices. Preserve legacy Pinyin separator behavior only on
   its existing non-supplemental syntax path. Keep current-alignment gesture
   semantics and stale-composition rejection.
2. Behavior change: dispatch English continuation and accepted-word learning on
   every English-enabled board. Preserve focused-language identity and English
   isolation, private/literal field restrictions, existing Chinese choice votes,
   correction/spacing policy, and lifecycle/context reset boundaries. English
   context must not silently become Chinese vote or phrase context.

Evidence: source-derived kana oracle controls, malformed-provider/UTF-16 controls,
natural English context references, baseline shared-core regressions and pinned
desktop Rime. Existing inspected corpora are regression evidence, not fresh
holdouts. No lexical quality changes are made or inferred from assertion counts.
Run each production slice through the core gate and final native integration
through tools/test-desktop.ps1; compare actual production results with the frozen
proposal outputs. Build Android only after core verification. Phone integration
and touch timing remain unrun while SHINE holds the explicit reservation.
