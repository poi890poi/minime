"""Publish the exhaustive baseline and the explicitly limited counterfactual."""
from pathlib import Path
import gzip,hashlib,json,shutil
root=Path(__file__).resolve().parent.parent
out=root/'docs/speculation';art=root/'artifacts'
sources={}
for name in ['speculation-core-summary.json','speculation-native-summary.json','speculation-parity-before-summary.json','speculation-parity-after-summary.json']:
    shutil.copyfile(art/name,out/name)
for name in ['speculation-core-targets.tsv.gz','speculation-native-targets.tsv.gz']:
    shutil.copyfile(art/name,out/name)
for path in sorted(art.glob('speculation-*-candidates.jsonl.gz')):
    sources[path.name]={'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'location':'local artifacts; reproducible using SpeculationBenchmark'}
(out/'raw-artifacts.json').write_bytes((json.dumps(sources,indent=2)+'\n').encode())
core=json.loads((out/'speculation-core-summary.json').read_text())
native=json.loads((out/'speculation-native-summary.json').read_text())
def pct(n,d):return '{:.2f}%'.format(100*n/d)
lines=['# Speculative candidate benchmark','',
'Baseline: 146bf1e (0.6.2); provenance-only commit 679e668. All eligible CC-CEDICT entries, no handpicked cases. 108,895 unique dictionary targets, 108,981 reading pairs; 25,023 natural multi-token spans; 670,020 labeled cases / 373,181 distinct inputs. Attestation vocabulary: 292,038 entries. Labels and membership are used only after decoding.','',
'## Origin and attestation','',
'| Retained path | Candidate occurrences | Vocabulary-attested |','|---|---:|---:|',
'| Core multi-unit paths alone | {:,} | {} |'.format(core['composed_occurrences'],pct(core['attested_composed_occurrences'],core['composed_occurrences'])),
'| Extra core multi-unit paths appended to Rime | {:,} | {} |'.format(native['composed_occurrences'],pct(native['attested_composed_occurrences'],native['composed_occurrences'])),
'| Native Rime results (provenance unclassified) | {:,} | {} |'.format(native['native_occurrences'],pct(native['native_attested_occurrences'],native['native_occurrences'])),'',
'Membership is **not** semantic accuracy. Real sentences are often absent from dictionaries; an attested word can be the wrong interpretation. Native prefix choices are included in occurrence counts. Native results cannot be classified as lexical or composed through the public desktop API.','',
'## Intended target retrieval','',
'Percentages use each group\'s complete case count. Top-eight position includes prefix choices, matching the actual candidate list; a target must consume the full input. “No extra combinations” removes only proven core multi-unit additions, not Rime hypotheses.','',
'| Group | Cases | Rime+core top 1 | Top 8 | No extra combinations top 8 | Any rank | No extra combinations any rank |',
'|---|---:|---:|---:|---:|---:|---:|']
for group,s in native['groups'].items():
    if group.startswith('native-only/'):continue
    lines.append('| {} | {:,} | {} | {} | {} | {} | {} |'.format(group,s['n'],pct(s['top1'],s['n']),pct(s['top8'],s['n']),pct(s['without_composed_top8'],s['n']),pct(s['any'],s['n']),pct(s['without_composed_any'],s['n'])))
lines += ['', '## Decision and limitations','',
'Reject deleting composition from the fallback decoder. On natural-test full input, core-only top-eight retrieval falls from 81.65% to 6.10% without it. Instead, when the primary decoder supplies a whole-token result, retain its hypotheses and supplement only stored core entries. If native decoding is unavailable or supplies only prefix choices, retain the complete fallback. No frequency weights, entries or per-word exceptions change.','',
'For the measured Rime+core lists, removing extra core combinations changes no first choice and loses 5 top-eight hits across 59,825 natural-test cases. Deep-list retrieval declines substantially (see table); the decision favors a smaller candidate list over those low-ranked recoveries. The candidate-origin trace supports blaming the extra fallback combinations, not calling every Rime/reference mismatch nonsense.','',
'Natural spans are held-out annotated token windows, not a curated conversation corpus. Readings come from dictionary entries, and polyphonic alternatives can produce readings that are wrong in sentence context (for example the alternate readings of 家/會). Therefore these are controlled input-to-target retrieval measurements, **not** an estimate of real-world sentence accuracy. Chinese variant spellings are also not normalized away. Existing conversation corpora are an additional required gate. No absolute natural-language accuracy is claimed.','',
'Core and native large runs overlap in wall time. Their timings include allocation and, for native mode, IPC; do not compare them as a speedup or as Android latency. Provenance parity independently verified 3,829 input-hash-selected queries and 121,624 candidates, with identical text/score/order signatures.','',
'Reproduce: `python tools/make_speculation_corpus.py`; compile core/tests; run `SpeculationBenchmark artifacts/speculation-core 3`, then with `-Dminime.benchmark.native=true` for native mode. Native mode requires the pinned desktop bridge/Rime DLL and isolated synthetic user directories. Raw per-target results and input/source hashes are retained here; large candidate traces remain under artifacts with hashes in raw-artifacts.json.','',
'Derived CC-CEDICT data is CC BY-SA 4.0 (MDBG/CC-CEDICT contributors); UD Chinese GSD is covered by its vendored source licence; McBopomofo data retains its vendored attribution. See third_party/cedict, third_party/ud and third_party/mcbopomofo. No evaluation corpus is bundled into the app.']
(out/'RESULTS.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('Wrote benchmark report and per-target evidence')
