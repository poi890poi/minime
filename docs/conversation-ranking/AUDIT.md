# Everyday conversion audit

Baseline: 01c88ff, MinIME 0.5.0. User deliberately withheld examples to prevent case-specific fixes.

Classification: investigate ranking and automatic acceptance defects; any intentional priority change must be reported separately. Owners: core intent/default selection, Rime candidate ordering, decoder merge. Preserve explicit selection, incomplete phonetics, private fields, English mode isolation, raw recovery and model provenance. No examples or expected answers may enter production dictionaries or ranking code.

Freeze an independently authored everyday vocabulary before baseline execution. Chinese labels are reasonable common choices, not a claim that every homophone has one correct default. Report rank and alternatives, not only exact first-choice success. English conversational tokens should remain literal in mixed mode unless the user has explicitly established an ambiguity preference; measure that preference separately. Phone UI checks compare Google Zhuyin and MinIME in the synthetic editor. Baseline uses no learning; separate learned-state tests diagnose contamination. Use fresh holdouts after proposing a mechanism. Preserve unsuccessful results.

Acceptance: improve the diagnosed boundary without degrading complete/initial/mixed phrase coverage, literal recovery or English mode. Run core, focused Android and packaged checks. Report native time separately from end-to-end behavior. Restore phone IME and preferences and turn its display off after each session.
