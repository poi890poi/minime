# Frozen proposal experiments

Baseline: 5dd9bb8 (production code d353be4), 2026-09-11. This is benchmark
tooling, not a production change. Isolated source snapshots live in ignored
artifacts; all variants are compared independently with the same baseline.

Experiments frozen before outputs:

1. `span`: honor valid raw UTF-16 prefix boundaries independently of Pinyin syntax;
   retain the existing explicit-selection/default distinction and route partial
   focused choices to the same learning namespace as whole choices.
2. `english-context`: accepted non-supplemental English literal tokens retain
   English context and dispatch continuation/learning on every English-enabled
   board. Corrections/autocorrection and spacing are unchanged: this isolates
   continuation ownership, not the entire proposed English capability migration.
3. `evidence-rank`: let an attested exact English spelling own the default when
   all focused choices are incomplete or kana spelling fallback. Preserve exact
   focused lexical/character choices. This is a policy experiment, not a fix.
4. `dedup`: remove duplicate candidate identities within each source reading list
   before constructing indexes; preserve first occurrence and source tiers.
5. `unit96`: raise only the reading-unit input limit from 32 to the outer 96;
   keep search/state/output budgets unchanged.
6. `prefix-cache`: cache up to 512 English prefix results per immutable dictionary,
   keyed by raw spelling and Latin-context flag. No approximate results or new ranks.

Inputs and labels are frozen and hashed by prepare.py. Existing 3,922 genre/error
conditions are regression data. New SHA256-selected source-entry probes are
retrieval controls, not new independent gold. Additional unselected GUM documents
are document-disjoint from the prior mode corpus, but the source was already
downloaded and is not claimed as a fresh external holdout. No evaluation target,
membership or document identity is exposed to runtime candidate generation.

Acceptance: no unconsumed-input loss; privacy and stale-selection controls retain
their contracts; behavior-preserving variants must retain ordered outputs and
defaults. Ranking/search variants report every gain and loss by language/genre/
condition, rather than accepting pooled improvement. The existing core and pinned
desktop Rime gates run on the baseline; isolated variants receive broad paired
output and mechanical tests. No phone operation or Android build is needed here.

Performance: three separate JVM passes with reversed variant order; uncached
provider/engine work is separate from cached output audits. Report p50/p95/p99/max,
initial loading, retained JVM heap and prefix-cache warm/cold behavior. Native IPC
time is included when measuring Chinese. Desktop time does not certify Android
request-to-applied 20/30 ms or visible 50/80 ms p95/p99 requirements. Physical first
row, touch hit rate, staged-render stability, Android GC and new grammatical
providers remain separate validation work; synthetic slot counts are not pixels.

Decisions: support for implementation, retain for more evidence, or reject the
tested variant. No automatic promotion to production. New Japanese grammar and
Taiwanese context models cannot be benchmarked as existing implementations: no
integrated candidate or independent labeled genre corpus yet exists. Report these
as untested, not as zero-quality or zero-latency results.
