from pathlib import Path
import gzip,json,shutil
root=Path(__file__).resolve().parent.parent;out=root/'docs/speculation'
before=json.loads((root/'artifacts/addon-partial-before-summary.json').read_text())
after=json.loads((root/'artifacts/addon-partial-after-summary.json').read_text())
for version in ('before','after'):
    for suffix in ('summary.json','targets.tsv.gz'):
        name='addon-partial-'+version+'-'+suffix;shutil.copyfile(root/'artifacts'/name,out/name)
cases={}
with gzip.open(out/'addon-inputs.tsv.gz','rt',encoding='utf-8') as f:
    for line in f:
        pack,form,raw,target=line.rstrip('\n').split('\t')
        cases.setdefault(pack+'/'+form,{}).setdefault(raw,set()).add(target)
lines=['# Shared add-on partial matching','',
'329,599 unique pack/form/input/output cases, 221,728 unique inputs. Every eligible source row is included; no success-selected examples. Complete aliases and outputs are unchanged (56,869 normalized language-asset records verified equal). All packs use the same bounded prefix index and reading-unit trie as core Chinese matching. Japanese units come from pinned WanaKana conversion tokens; POJ and Chinese boundaries come from source separators.','',
'Top-eight **lookup retrieval**, not real-world language accuracy. All four packs enabled simultaneously. Each source-compatible spelling counts separately, including Kana/Romanization/Kanji outputs. Initials can collide with many words; eight slots cannot contain every target. “Isolated ceiling” assumes an ideal oracle allocating all eight slots to the tested pack.','',
'| Pack/form | Cases | Before top 8 | After top 8, all packs | After top 8, pack alone | Isolated ceiling |',
'|---|---:|---:|---:|---:|---:|']
for group,old in before['groups'].items():
    if group.startswith('isolated/'):continue
    new=after['groups'][group];single=after['groups']['isolated/'+group]
    ceiling=sum(min(8,len(targets)) for targets in cases[group].values())
    pct=lambda v:'{:.2f}%'.format(100*v/old['n'])
    lines.append('| {} | {:,} | {} | {} | {} | {} |'.format(group,old['n'],pct(old['top8']),pct(new['top8']),pct(single['top8']),pct(ceiling)))
lines+=['','Complete-key retrieval does not regress. POJ initial lookup remains substantially ambiguous; the oracle ceiling distinguishes slot collisions from search/ranking losses. Prefix cases also include truncated legacy Chinese initial aliases, which are not prefixes of full readings. The same search supports partial source units; no language-specific minimum input length or runtime expansion list is used.','',
'Desktop component costs: exact-only load {:.0f} ms / {:.1f} MiB retained; indexed load {:.0f} ms / {:.1f} MiB retained. All-pack lookup p50/p95 changed from {}/{} µs to {}/{} µs. Approximate heap after GC; large-run timing overlaps other validation and is not an isolated speed benchmark or Android latency. This is a real resource cost, not a claimed optimization. The Android path therefore runs base and optional lookups on the same worker and publishes one revision-bound result. Existing switches still isolate optional data; loaded indexes remain cached.'.format(before['load_ms'],before['retained_heap_bytes']/2**20,after['load_ms'],after['retained_heap_bytes']/2**20,before['query_p50_us'],before['query_p95_us'],after['query_p50_us'],after['query_p95_us']), '',
'Reproduce with `python tools/make_addon_matching_corpus.py` and `AddonMatchingBenchmark <output-prefix>`. The baseline uses 146bf1e classes with the identical boundary-enriched asset (normalization parity verified), and the shared-index run uses the new matcher. Evaluation labels never enter lookup. Exact aliases are tried before incomplete matches; partial matches use existing omitted-letter penalties and source-order ties. Corpus/asset hashes and raw ranks are retained alongside this report.']
(out/'MATCHING.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('Wrote add-on retrieval and resource report')
