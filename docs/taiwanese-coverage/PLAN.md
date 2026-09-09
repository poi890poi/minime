# Taiwanese Han coverage correction — frozen plan

Baseline: 9a37682 / 0.7.1. User clarified that missing Han forms may explain the
reported missing everyday words. Scope: source-attested Han alternatives for
already shipped POJ outputs. Preserve phonetic inventory, input aliases, ranking,
keyboard UI and acceptance semantics. The unrelated iTaigi Mandarin-gloss import
filter is recorded as separate debt, not changed in this slice.

Cause established before implementation: only iTaigi candidates can attach pairs;
the beginner source has no Han field and is always excluded. The 99% Mandarin
single-character mass gate is also an unsuitable veto on attested basic Taiwanese
words, even when the phonetic entry already exists. No claim of community spelling
agreement can be inferred from Mandarin frequency.

Source decision: use the existing ChhoeTaigi pinned revision's Taiwan-authored
2002+ 台華線頂對照典 (Taihoa), by 鄭良偉, expanded/edited by 楊允言 and volunteers,
CC BY-SA 4.0. This established source includes explicit POJ/HanLo pairs and is part
of the documented PhahTaigi data family. Import only pairs matching complete
already shipped phonetic outputs. Never use the Mandarin gloss as Han output,
translate, join fragments or manufacture a spelling. Keep primary readings only
where the source Han field is attached to that exact reading; other-reading fields
are not assumed to share it. Preserve the previous 9,385 choices where available.

Eligibility: a single all-Han source field (1–12 glyphs), for an existing complete
phonetic output; retain the frozen familiarity gate for general vocabulary. For
headwords and aligned variants present in the already shipped authored beginner
vocabulary, permit explicit all-Han source forms despite low Mandarin glyph counts.
This is a source/category rule, not a word allowlist or tuning to an evaluation set.
Among eligible new examples retain the existing general preference for greater
minimum character occurrence, then whole-form occurrence, then stable source ID.
A basic reading is evidence of vocabulary scope, not consensus on a Han spelling.
A canonical example identifies a pronunciation; different senses can share it.

Evaluate all existing POJ source outputs by category, all directly comparable MOE
basic handout Han headwords, and all complete/partial queries from the frozen mode
corpus. The handout is evaluation only; its damaged extracted phonetics must not
be used as gold readings or imported. Freeze IDs/headwords before generation;
inspect extraction completeness and retain missing/unsupported cases. Previous
corpora remain regressions; source overlap prevents a fresh generalization claim.
No evaluation labels enter the compiler. Test source identity, alias metadata,
copy/lookup parity and tap/hold/Space in core before Android build. Reuse pinned
Rime desktop and targeted phone integration; restore settings and sleep/verify.
