# Priority-list parser audit

The pinned upstream parser overwrites earlier same-tier readings for three
glyphs. Replacing its scalar value with a set fixes that representation defect
in an isolated compiler copy. This does not yet admit the resulting model to
MinIME production.

Generated fixtures reproduce six score changes when list order is reversed.
The set parser is order invariant, preserves every same-tier reading, and gives
identical output for the single-reading control. An unmodified full rebuild
matches the pinned original `data.txt` SHA-256 byte for byte, including CRLF.
Both builds retain all 174,996 source identities. Four single-glyph scores change;
no multi-glyph score changes. See `prior-parser/summary.json` for compiler/input
pins and generated-fixture receipts. No upstream source list was edited.

Relative-prior comparison, corrected parser versus original parser:

| Probe | Before | After |
|---|---:|---:|
| Broad whole target among first eight, 24,244 labelled cases | 11,527 | 11,531 |
| Broad target-compatible first-eight slots | 30,944 | 30,948 |
| Chat whole target among first eight, 4,608 cases | 610 | 610 |
| Complete single-glyph reading pairs reachable | 25,100 | 25,100 |

The broad run changes 152 ordered outputs and the chat run ten. Neither changes
Space defaults, candidate identities or consumption spans. This small correction
cannot establish the editorial quality of the priority lists. Three tertiary
glyphs missing a secondary-tier entry and mixed logarithm units remain separate
untested hypotheses.

The first harness attempt incorrectly normalized upstream output to LF before
checking an original-byte pin. That attempt was rejected; the corrected harness
preserves raw bytes and requires the original pin. Reproduction: run
`tools/audit_priority_parser.py --output artifacts/<fresh-directory>`, then the
existing weight exporter and shared-core evaluators. All raw model copies and
per-query changes stay in ignored artifacts. Aggregate receipts only are retained
here. This is a tooling/diagnostic change with no app, data or preference impact.
