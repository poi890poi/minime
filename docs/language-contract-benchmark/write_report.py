"""Render the measured aggregate tables; no method selection or parameter tuning."""
from pathlib import Path
import json,collections,statistics,gzip
OUT=Path(__file__).resolve().parent
r=json.loads((OUT/'results.json').read_text(encoding='utf8'))
c=json.loads((OUT/'conversations/results.json').read_text(encoding='utf8'))
m=json.loads((OUT/'conversations/manifest.json').read_text(encoding='utf8'))
def rate(g,k):return f"{100*g[k]/g['n']:.1f}% ({g[k]}/{g['n']})"
def total(groups,prefix='',parts=None):
    n=collections.Counter()
    for k,v in groups.items():
        if k.startswith(prefix) and (parts is None or len(k.split('/'))==parts):n.update(v)
    return n
names={'real-persona-chat':'Japanese casual chat','asdc':'Japanese service dialogue','suisiann-thousand':'Modern Taiwanese situations','taiwanese-basic':'Historical Taiwanese examples'}
roles={s:'holdout' if s!='taiwanese-basic' else 'regression' for s in names}
lines=['# Conversation benchmark: stronger references, visible phrase gaps','',
'Baseline: 5dd9bb8 (app code d353be4 / 0.8.2). This is an evaluation change. No vocabulary, ranking weight, production code or Android package was changed.','',
'The corpus adds **61,941 controlled input conditions from 7,775 source references**. Japanese uses 192 human conversations: 96 casual chats and 96 accommodation-service role-plays. We retain all 4,848 turns from those conversations. Taiwanese adds 1,063 modern authored aligned examples from 18 files, plus 1,024 historical authored examples; these are not spontaneous multi-turn conversations. Sampling words and clauses yields more references than original examples.','',
'Sources, licenses, pinned revisions, reading conversion and exclusions are in [CONVERSATION_DATA.md](../CONVERSATION_DATA.md), [manifest.json](manifest.json) and [NOTICE.md](NOTICE.md). No entries were handpicked. Inputs and references regenerated identically from the pinned sources; gzip content was compared after decompression.','',
'## Complete input: initial holdout and historical regression','',
'Exact target matching after NFC and case folding. Tone marks, spacing, hyphens, kana/kanji and inflection remain significant. Top-three/eight count ordinal suggestion slots excluding raw input; they are not measured screen rows/pages. Only complete-consumption matches count. Japanese reading annotations for kanji are silver, and grammar or alternate valid wording is not judged.','',
'| Source / evaluation role | Unit | References | Default | Top 3 | Top 8 |',
'|---|---|---:|---:|---:|---:|']
for s in names:
    for unit in (['word','two-token','clause'] if s in ['real-persona-chat','asdc'] else ['word','clause']):
        g=c['coverage']['baseline'][f'{s}/{roles[s]}/{unit}/full']
        lines.append(f"| {names[s]} / {roles[s]} | {unit} | {g['n']} | {rate(g,'fold_default')} | {rate(g,'fold_top3')} | {rate(g,'fold_top8')} |")
lines+=['','A good word score does not establish usable phrase composition. Complete-clause retrieval is especially weak for modern Taiwanese. These failures do not prove that every returned alternative is meaningless, and they do not measure completing the same sentence through multiple selections. Stateful composition and human acceptability need additional evaluation.','',
'## Prefixes and imprecision','',
'| Source | Unit | Condition | Top 8 |','|---|---|---|---:|']
for s in names:
    for unit in ['word','clause']:
        for cond in ['half','three-quarter','initials','mixed','transpose','omission','neighbor']:
            key=f'{s}/{roles[s]}/{unit}/{cond}'
            if cond in ['transpose','omission','neighbor']:key+='/input:changed'
            g=c['coverage']['baseline'].get(key)
            if g:lines.append(f"| {names[s]} | {unit} | {cond} | {rate(g,'fold_top8')} |")
lines+=['','Error rows above exclude controls that leave the original input unchanged. Japanese incomplete units are source morphological tokens; Taiwanese units are supplied-reading syllables. These are diagnostic controls, not observed gesture frequencies. The reference JSON retains all controls and their strata.','',
'## Taiwanese explicit-tone strata','',
'Unmarked means no explicit combining tone mark in the target, not a linguistic assertion of neutral tone. All input is mechanically derived plain POJ. This separates the missing-unmarked-word concern without assuming every unmarked form is neutral.','',
'| Source | Word target | Top 8, complete input |','|---|---|---:|']
for s in ['suisiann-thousand','taiwanese-basic']:
    for tone in ['marked','unmarked']:
        g=c['coverage']['baseline'].get(f'{s}/{roles[s]}/word/full/tone:{tone}')
        if g:lines.append(f"| {names[s]} | {tone} | {rate(g,'fold_top8')} |")
lines+=['','## Paired proposal effects on the new corpus','',
'| Isolated variant | Default changes | Case-folded default gains / losses | Top-8 gains / losses | Any-slot gains / losses | Order changes |',
'|---|---:|---:|---:|---:|---:|']
for v,groups in c['comparisons'].items():
    g=total(groups,parts=4)
    lines.append(f"| {v} | {g['default_changed']} | {g['fold_default_gain']} / {g['fold_default_loss']} | {g['fold_top8_gain']} / {g['fold_top8_loss']} | {g['fold_any_gain']} / {g['fold_any_loss']} | {g['order_changed']} |")
lines+=['','No prototype was retuned after these results. Full gains and losses by source, split, unit, condition, annotation status, explicit tone mark, effective error and exact addon-text overlap are in [results.json](results.json). Changed cases are retained in `changed-outputs.tsv.gz`; their labels never feed generation.','',
'## Independence and limitations','',
'- Japanese splits are 32 development and 64 initial-holdout conversations per source, with schema-inspected dialogues excluded. Speakers may recur. Modern Taiwanese splits are 6 development and 12 initial-holdout source files; related authors and vocabulary remain shared.',
f"- {m['holdout_targets_also_in_development']} distinct held-out target strings also occur in development. Natural recurring greetings are retained, and no phrase-disjoint claim is made. The initial holdout is now consumed; it cannot be called fresh in a later tuning run.",
'- Japanese surface forms come from actual source turns. Kana-only references and Sudachi-generated kanji readings are scored separately. OOV tokens and readings that fail independent romanization round trips are excluded and counted, so hard/unknown vocabulary is underrepresented.',
'- Taiwanese readings come from the authors. Modern Tâi-lô is converted to POJ using pinned Taiwanese tools, retaining CH/chh. Failed conversions are excluded, not guessed. Historical examples share production-source lineage and remain regression data.',
'- Exact `addons.tsv` target overlap is a diagnostic, not proof that non-overlapping text has independent upstream lineage. It excludes generative kana output and other core vocabulary.',
'- Whole ordered turns are retained, but this run evaluates isolated compositions with no preceding conversation context. It does not benchmark sustained multi-turn learning, topic coherence or physical touch.',
'- Japanese accommodation dialogue and historical Taiwanese teaching sentences remain separate from casual chat. The Taiwanese spontaneous-conversation source remains on access/rights hold.','',
'## Verification','',
'Pinned reference-tool versions, source download hashes, deterministic regeneration, input SHA-256, row alignment, nonempty references, and conversation/file split disjointness were checked. All seven isolated variants completed all 61,941 conditions. See the parent [proposal report](../REPORT.md) for core/Rime regression gates and timing boundaries.','']
(OUT/'conversations/REPORT.md').write_text('\n'.join(lines),encoding='utf8')

lines=['# Shared suggestion-contract experiments','',
'**Decision: implement the acceptance and English-context contracts in separate production changes; reject the tested English-default and deduplication variants; retain the remaining prototypes for more evidence.** This turn lands benchmark tooling and evaluation data only. No combined variant was tested.','',
'Baseline **5dd9bb8**, production code **d353be4**, MinIME **0.8.2**. Six one-variable source snapshots were frozen before evaluation. The original 21,598 conditions produce 27,136 mode-condition evaluations; the new conversation corpus adds 61,941 conditions per variant. Read [PLAN.md](PLAN.md) and [REPRODUCE.md](REPRODUCE.md) for causal boundaries and reproduction.','',
'## Decisions by prototype','',
'| Prototype | Original 27,136 comparisons | Decision |','|---|---|---|',
'| `span` | Zero candidate-order/default changes; acceptance controls below improve | Support a production fix with an explicit raw-span contract and regression tests |',
'| `english-context` | Zero typing-order/default changes; continuation improves on all English-enabled boards | Support a separate context/learning dispatch change |',
'| `evidence-rank` | 537 default gains, 36 losses; candidate lists unchanged | Reject this fallback-based default rule; it sacrifices intended focused-language defaults |',
'| `dedup` | 83 order changes; no reference default/top-3/top-8 gain or loss | Reject as a behavior-preserving change: new corpus also loses six previously retrievable targets |',
'| `unit96` | One additional complete match, no measured loss | Retain: small targeted benefit, not a phrase-coverage solution; long-input latency still unmeasured |',
'| `prefix-cache` | Exact order/default parity | Retain: median English work improves, but tail speedup is inconsistent |','',
'The new-corpus paired outcomes and baseline coverage are in [Conversation report](conversations/REPORT.md). The default policy adds just one correct default and loses 100. Dedup changes 900 orders and loses six previously retrievable historical Taiwanese targets beyond the first eight; it is not behavior-preserving. Their bounded-index interaction needs a separate causal investigation before reconsideration. The corpus exposes a much larger word-to-phrase gap than source-entry lookup controls. Do not use pooled gains to approve the ranking policy.','',
'## Acceptance and continuation','',
'Acceptance uses 256 independently generated kana-oracle strings, four input forms, tap/space/stale-selection actions, and normal/private fields: **6,144 mechanical controls**. Baseline passes **3,124/6,144**; `span` passes **6,144/6,144**. For explicit prefix taps, uppercase, title case and hyphenated inputs lose their suffix in all **1,536/1,536** baseline controls; the prototype preserves it in all. Stale-selection controls already pass and remain passing. These are mechanical checks, not language accuracy.','',
'Normal partial choices move into the focused-language learning namespace; private controls make zero learning calls. This narrow prototype still uses existing Pinyin-oriented filtering and separator trimming elsewhere. A production change needs language-provider span metadata and shared validation, not an assertion that every composition contract is already universal.','',
'For **532 natural English context/target pairs per board**, the existing provider has alternatives for 458 and contains the next source word in its first eight choices for **118 (22.2%)**. Baseline exposes these only in English mode. The context prototype exposes the same results in Chinese/English, Taiwanese/English and Japanese/English, taking each mixed board from **0/532 to 118/532** target hits. English mode remains 118/532. Private fields retain zero learning calls. This is dispatch improvement, not improved prediction probability; persistence, correction, autocorrection and spacing were not redesigned.','',
'The default-policy experiment gains 537 English defaults but loses one English, 21 Japanese and 14 Taiwanese defaults on the original corpus. It uses the kana-fallback score as a prototype evidence marker and changes only the preferred slot. Neither that score sentinel nor an English-first rule is a suitable shared production evidence contract.','',
'## Desktop typing latency','',
'Three separate JVM runs, reversed variant order in the middle pass. Table entries are the median of each run’s **repeat-pass p95**, milliseconds per typed key. Each mode uses 192 hash-selected full inputs, every prefix, two cycles, uncached providers. Exact per-sample timings and stage decomposition are committed; output-audit cache timings are excluded.','',
'| Variant | Chinese/EN | English | Taiwanese/EN | Japanese/EN |','|---|---:|---:|---:|---:|']
modes=['chinese','english','taiwanese_english','japanese_english']
for v,files in r['latency_ms'].items():
    vals=[statistics.median(g[mode+'/repeat-pass']['total_ns']['p95'] for g in files.values()) for mode in modes]
    lines.append('| '+v+' | '+' | '.join(f'{v:.3f}' for v in vals)+' |')
lines+=['','Baseline repeat-pass distribution (median of three per-run statistics):','',
'| Mode | p50 | p95 | p99 | Maximum |','|---|---:|---:|---:|---:|']
for mode in modes:
    vals=[statistics.median(g[mode+'/repeat-pass']['total_ns'][key] for g in r['latency_ms']['baseline'].values()) for key in ['p50','p95','p99','max']]
    lines.append('| '+mode+' | '+' | '.join(f'{v:.3f}' for v in vals)+' |')
lines+=['','The English cache reduces median repeat work from roughly 0.054 to 0.028 ms, but p95 is roughly 0.260 versus 0.266 ms. The hypothesized tail-latency benefit does not reproduce. Its filled-cache heap and multithread contention were not measured; there is no basis to ship it as a responsiveness fix.','',
'Chinese includes native IPC. First-pass files retain startup outliers separately; load files retain model-loading time and JVM heap. This harness loads all add-ons plus geography, unlike an active-mode-only budget. Corpus preparation overlapped portions of the timing run, so small differences remain inconclusive. Desktop timing excludes Android debounce, queueing, rendering and touch; it does not certify the 20/30 ms applied or 50/80 ms visible p95/p99 requirements. Long-input latency beyond the 24-character timing sample is not measured.','',
'## Cache working-set tradeoff','',
'A separate workload toggles Chinese and Japanese six times, then changes focused language nine times. Three JVM repetitions compare keeping all loaded add-on packs with keeping Chinese plus the last focused pack. Forced-GC retained heap excludes the base model and geography.','',
'| Policy / phase | Cache misses per run | Median mean load, ms | Median peak add-on heap, MiB |','|---|---:|---:|---:|']
for policy in ['keep-all','warm-pair']:
    for phase in ['fast-toggle','focus-changes']:
        g=[v[phase] for k,v in r['working_set'].items() if k.startswith('working-set-'+policy)]
        if g:lines.append(f"| {policy} / {phase} | {statistics.median(x['misses'] for x in g):g} | {statistics.median(x['load_ms']['mean'] for x in g):.2f} | {statistics.median(x['peak_addon_mib'] for x in g):.2f} |")
lines+=['','Retain this as a resource-policy proposal: a smaller warm set can save memory but reloads discarded focus packs. Loading must remain asynchronous and preserve the current visible snapshot. This desktop experiment does not certify Android mode-switch responsiveness or native-library memory.','',
'## Remaining evidence and gates','',
'Staged publication/cancellation, pixel-level stability, raw-span adapters for every provider, Japanese grammar conversion, and Taiwanese contextual decoding are not integrated prototypes here. Their performance and quality are **untested**, not assumed equivalent to a shared lexical ranker. Dictionary-only mechanisms cannot be promoted into universal language models from these results.','',
'Baseline `tools/test-core.ps1` and the pinned `tools/test-desktop.ps1` results are recorded in `verification.json`. Source-ledger validation and deterministic corpus checks cover this test/data change. Android builds and phone tests were unnecessary; the phone was not touched.','']
(OUT/'REPORT.md').write_text('\n'.join(lines),encoding='utf8')
print('Wrote proposal and conversation reports')
