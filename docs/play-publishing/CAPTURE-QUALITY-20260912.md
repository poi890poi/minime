# Capture refresh and whole-input candidate quality

Request: recapture Play images without meaningless suggested sequences; treat
those outputs as quality-control defects, not screenshot styling problems.

Baseline 61018b0 reproduces a remaining merge defect in the shared core:
`jianianduanshe` yields 加年端社, 加捻, 加拈, 家年度安設; shorter
`jianianduan` yields 加捻, 加年端社, 加拈, 家碾斷. These are diagnostic
examples only and must not become production exceptions. 加捻 is a lexical prefix
match; the diagnosis is relevance/consumed coverage, not a claim that it can never
be a word. The native sentence is a generated path. Neither is needed as a
fallback when a dictionary-backed candidate already covers all typed input.

Type: shared-core candidate relevance fix and intentional fallback visibility
change, followed by screenshot/release-material refresh. Preserve source data,
native decoding, exact-match ordering, explicit confirmations, English/private
fields and prefix acceptance when no whole-input Han entry is available.

Rejected candidate rule, frozen before comparison: if an attested Han choice covers all
typed input (including a source-backed completion), omit unverified construction
and shorter consumed-span fallbacks. Otherwise keep the previous construction
policy. Explicitly confirmed construction retains user intent. Do not remove
source entries, blacklist text, fabricate pixels, or change the geography demo
to hide the defect. This is a structural relevance gate, not a semantic oracle.

Replay the existing 11220 variants with Taiwan/geography add-ons off/on before and
after. They are consumed regression evidence, not fresh holdout data. Report lost
and gained reference coverage and defaults; inspect any removed correct reference.
Add behavioral tests for gate/absence, lexical duplicate evidence, explicit choice,
secure fields and prefix-only recovery. Run core and pinned desktop before Android
build and phone capture. No claim that essays measure natural conversation quality.

Screenshot inputs remain mingtian, hello, liho, arigatou and jianianduan. Rebuild
the app/test APKs from the verified core, capture real phone UI, visually inspect
all five and reject captures containing irrelevant fallbacks. Restore prior IME,
preferences and viewport and verify display OFF under an acknowledged phone lease.
Refresh provenance, archive hashes and the already corrected Productivity category.
Signed release binaries remain separate and must not be mislabeled as refreshed.

Initial negative result: the stricter gate removed 不對 in an existing Zhuyin
regression. Java had both a relaxed whole dictionary match and a higher-scoring
segmented path; text deduplication retained only the latter's composed flag.
Preserve any independently matched lexical evidence for the same text/span before
deduplication. This repairs provenance generally, without a tone or word exception.

Decision: reject suppression. The broad regression removed correct constructions
including 中山高 and 新北市政府 in favor of longer source completions. A lexical
completion is not proof that every other construction is meaningless. Raw negative
results are retained in docs/whole-input-quality/*-rejected-filter.json. Prefix
acceptance tests remain unchanged after rejection; no expected phrase was removed
from the regression corpus to make the experiment pass.

Intermediate rule, superseded by the source-only change: when Chinese is the active chosen interpretation, stably place all
attested whole-input Han matches ahead of shorter-prefix recovery and unverified
construction. Preserve every candidate, explicit confirmations, English defaults
and a dedicated language's focus. No blacklist or source-data refresh. Semantic
quality of construction where no reliable whole match exists remains unresolved;
a clean marketing capture must not be described as closing that broad defect.

The capture quality check records visible accessibility candidate labels and
requires all visible geography choices for the unchanged jianianduan input to
come from matching Rudy source entries, including the requested 加年端社. This
is a screenshot regression, not a corpus-accuracy metric or a runtime word list.

The user's subsequent instruction was to simplify generation itself. The final
experiment removed native sentence assembly and both Java joining paths.
It was then rejected when the user required recall and precision to improve
together: full-spelling recall dropped substantially. Production changes were
reverted; see ../whole-input-quality/RECALL-PRECISION.md for the current decision.

Phone session 692c1c7e-4b3c-4385-a84b-9ecfe497508e: all three source-only native
integration tests passed. The screenshot assertion failed because its collector
counted the accessibility scroll container “Candidate list” as a word. The
collector now inspects actionable leaves and separately records literal input.
No failed capture was published. Preferences, Samsung IME, viewport and display
OFF were verified during cleanup; the phone reservation was explicitly released.

Restoration session 03a5dbe8-ed25-414f-aa3c-a7282e84e002 installed the baseline
debug APK after the source-only experiment was rejected. Baseline shared-core
checks passed (306,540 assertions), the pinned desktop evaluator completed all
13,014 probes, the local Android build passed, and all three phone native
integration tests passed. Preferences and prior Samsung IME were restored and
display OFF verified. The phone lease was explicitly released. These checks
establish restoration and integration, not resolution of candidate quality.
