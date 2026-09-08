from pathlib import Path
import collections,gzip,hashlib,json
root=Path(__file__).resolve().parent.parent;out=root/'docs/glyph-ranking'
def rows(name):return [json.loads(line) for line in (out/name).read_text(encoding='utf-8').splitlines()]
def index(data):return {(r['input'],r['mode']):r['candidates'] for r in data}
def reversals(data):
    idx=index(data);found=[]
    for raw,mode in idx:
        if mode!='no-packs':continue
        a=[c[0] for c in idx[(raw,mode)] if len(c[0])==1 and c[2]==0]
        b=[c[0] for c in idx[(raw,'all-packs')] if len(c[0])==1 and c[2]==0]
        for i,g in enumerate(a):
            for h in a[i+1:]:
                if g in b and h in b and b.index(g)>b.index(h):found.append([raw,g,h])
    return found
old=rows('release-062.jsonl');new=rows('candidates.jsonl');before=index(old);after=index(new)
assert len(old)==len(new)==499*5
a,b=reversals(old),reversals(new)
assert not b,b[:10]
unchanged={mode:all(before[k]==after[k] for k in before if k[1]==mode) for mode in ('native',)}
summary=json.loads((out/'summary.json').read_text(encoding='utf-8'));reference=json.loads((out/'reference.json').read_text(encoding='utf-8'))
evidence={'baseline':'146bf1e / 0.6.2','inputs':499,'complete_syllables':424,'layer_snapshots':len(new),'baseline_addon_glyph_order_reversals':len(a),'affected_inputs':len({r[0] for r in a}),'final_addon_glyph_order_reversals':len(b),'native_lists_unchanged':unchanged['native'],'baseline_reversals':a}
manifest={}
for name in ('release-062.jsonl','before.jsonl','duplicate-fixed.jsonl','candidates.jsonl'):
    path=out/name;data=path.read_bytes()
    with (out/(name+'.gz')).open('wb') as f:
        with gzip.GzipFile(filename='',fileobj=f,mode='wb',mtime=0) as z:z.write(data)
    manifest[name]={'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}
evidence['raw']=manifest
(out/'comparison.json').write_bytes((json.dumps(evidence,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
def pos(idx,raw,text):
    return next((i+1 for i,c in enumerate(idx[(raw,'all-packs')]) if c[0]==text),None)
# Report the strongest source-frequency inversions caused by duplicate promotion,
# excluding comparisons to alternate readings. Selection never changes runtime data.
pairs=[]
def primary(glyph,raw):
    weights=reference['rime_reading_weights'].get(glyph,{})
    def value(s):
        if s=='preset/default':return 1.0
        try:return float(s[:-1])/100 if s.endswith('%') else float(s)
        except ValueError:return 0.0
    return raw in weights and value(weights[raw])==max(map(value,weights.values()))
for raw,common,promoted in a:
    cm=reference['moe'].get(common);pm=reference['moe'].get(promoted)
    if cm and pm and cm['count']>pm['count'] and primary(common,raw) and primary(promoted,raw):
        pairs.append((cm['count']/pm['count'],raw,common,promoted))
examples=[];seen=set()
for ratio,raw,common,promoted in sorted(pairs,reverse=True):
    if raw in seen:continue
    seen.add(raw);examples.append((raw,common,promoted))
    if len(examples)==5:break
lines=['# Systematic rare/common glyph investigation','',
'Probed **424 distinct supported syllables and every proper prefix: 499 inputs**, at five layers (core, native, merge, composition without packs, composition with all packs). Fresh native sessions; no user history. The independent references are the MOE school-corpus table (4,343 common-standard glyphs) and held-out UD GSD counts (2,365 observed Han glyphs). All source/hash metadata and raw candidate lists are retained.','',
'## Confirmed application defect','',
'The released 0.6.2 add-on promotion rule creates **{:,} relative-order reversals** between existing whole-input glyph candidates across all 499 inputs. The corrected rule creates **0**. Native lists are byte-for-value unchanged. This is a direct mechanical invariant, not a claimed universal language-accuracy percentage.'.format(len(a)),'',
'An exact add-on duplicate previously jumped ahead solely because it appeared in another dictionary. Protecting only the leading base candidate left later common homophones exposed; English completion interleaving could hide even the leading Chinese candidate from that check. Existing exact base entries and intrinsic single-glyph whole-input results now retain their base rank. Novel dictionary entries and explicit personal choices remain separate. No character-frequency override or word exception was introduced.','',
'Positions below exclude the separate raw-input buffer and include other-language suggestions. Examples are selected deterministically from measured reversals by MOE frequency ratio, not used to tune production.','',
'| Input | Reversed pair | 0.6.2 positions (rare / common) | Final positions (rare / common) |','|---|---|---:|---:|']
for raw,common,promoted in examples:lines.append('| `{}` | {} before {} | {} / {} | {} / {} |'.format(raw,promoted,common,pos(before,raw,promoted),pos(before,raw,common),pos(after,raw,promoted),pos(after,raw,common)))
lines+=['','## Separate data issues, not silently patched','',
'Rime’s own source frequencies differ from the independent references. For `an`, it ranks 俺 before 安; its essay counts are 17,692 versus 12,474, while MOE records 5 versus 1,121 and held-out UD records 0 versus 57. This is a source-distribution mismatch, not the duplicate-promotion bug. Other primary-reading inversions and source weights are listed in summary.json. No native dictionary weights changed.','',
'The fallback assigns zero source frequency to **{}** glyphs present in the MOE reference. 臺 is the sole one within MOE’s top 1,000 (rank 285); the fallback places it at position 55 for `tai`. The importer uses the same whole-glyph count for every pronunciation and defaults missing counts to zero. A future data change should distinguish missing counts from measured zero and retain pronunciation-specific weights; it must be evaluated on a fresh independent corpus rather than trained and scored on this diagnostic table.'.format(len(summary['core_zero_frequency_moe'])),'',
'A naive frequency-only reorder is rejected as an unsupported remedy. For example, 的 is globally frequent mainly under `de`, so its lower position under `di` is not evidence of a bug. The report additionally identifies whether both compared pronunciations have the highest source reading weight. MOE school usage is not universal adult conversation usage, and absence from either reference is not proof that a glyph is rare.','',
'## Presentation and verification','',
'The first shared-partial presentation experiment inserted up to eight incomplete matches together. Broad conversation testing showed that this displaced common choices; that design is rejected and its raw evidence is retained under docs/speculation. Final presentation previews one incomplete match early and keeps the rest after the first eight displayed alternatives. All lookup results remain reachable. This changes visibility, not dictionary coverage or source frequencies.','',
'Generic regressions cover duplicate homophones under all four pack names, English-overlap interleaving, native glyphs absent from the fallback source, Space/visible-choice ownership, stale callbacks, and private fields. No probe example supplies production scores or eligibility. The immutable core/native data and independent reference hashes make the diagnosis reproducible.','',
'Source: [MOE table 18](https://language.moe.gov.tw/001/Upload/files/SITE_CONTENT/M0001/PRIMARY/shrest2-18.htm), extracted by the pinned SHINE AAC generator; UD, McBopomofo and Rime source archives retain their repository licences. These references and evaluation files are not shipped in the APK.']
(out/'RESULTS.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in evidence.items() if k not in ('baseline_reversals','raw')},indent=2))
