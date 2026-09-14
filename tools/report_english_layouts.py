"""Render the frozen sensitivity experiment; no parameter or layout selection."""
import argparse
import collections
import gzip
import html
import json
from pathlib import Path
import shutil
import numpy as np
from simulate_english_layouts import ROWS, PROFILES, METHODS, geometry, contacts, word_ids, load_inputs


def report(source, out):
    manifest = json.loads((source/'manifest.json').read_text())
    result = json.loads((source/'summary.json').read_text())
    rows = result['results']
    assert len(rows) == 4*2*5*3*4*15
    out.mkdir(parents=True, exist_ok=True)
    docs = collections.Counter(d['genre'] for d in manifest['documents'])
    genres = sorted(docs)
    def select(layout, width, profile, method, genre):
        return [r for r in rows if (r['layout'],r['width_mm'],r['profile'],r['method'],r['genre']) == (layout,width,profile,method,genre)]
    def fraction(rs, field, scale=100):
        return scale*sum(r[field] for r in rs)/sum(r['words'] for r in rs)
    def rate(layout,width,profile,method,genre):
        return fraction(select(layout,width,profile,method,genre),'word_errors')
    lines = ['# Layouts under simulated touch imprecision', '',
             '2026-09-14. This is a letter-only geometry/decoder sensitivity experiment, not a human typing-speed benchmark.', '',
             f"{manifest['eligible_tokens']:,} eligible occurrences across 15 genres and {len(manifest['documents'])} documents; "
             f"{manifest['lexicon_words']:,} dictionary words; {manifest['oov_tokens']} corpus occurrences absent from that dictionary "
             f"({100*manifest['oov_tokens']/manifest['eligible_tokens']:.2f}%). "
             f"Excluded {sum(manifest['excluded_tokens_by_genre'].values())} whole non-ASCII/apostrophe/over-length tokens from the already normalized corpus.", '',
             'Four layouts × two widths × five contact profiles × three matched seeds = 120 conditions. '
             'The corpus and its labels were already used in earlier evaluations; no fresh-holdout claim. '
             'All 7200 genre/decoder/seed rows are retained in `summary.json.gz` and per-document counts in `documents.jsonl.gz`.', '',
             '## Conversations and essays', '',
             '60 mm usable width, 1.5 mm scatter. Percent of words incorrect, averaged across three seeds. '
             '“Valid error” means a wrong literal spelling that exists in the frozen dictionary, including abbreviations and rare entries; it does not certify ordinary English usage. '
             'The forced decoders always choose a dictionary word and are diagnostic, not shipping policies.', '']
    html_tables = []
    for genre in ('en-gum-conversation','en-gum-essay'):
        reference=select('qwerty',60,'scatter-1.5','literal',genre)
        lines += [f"### {genre.removeprefix('en-gum-').title()} — {reference[0]['words']} words, {docs[genre]} document(s)", '',
                  '| Layout | Literal error | Valid error | Spatial only | Spatial + frequency | Conservative |',
                  '| --- | ---: | ---: | ---: | ---: | ---: |']
        table=[]
        for name in ROWS:
            vals=[fraction(select(name,60,'scatter-1.5','literal',genre),'word_errors'),
                  fraction(select(name,60,'scatter-1.5','literal',genre),'raw_valid_word_errors')]
            vals += [rate(name,60,'scatter-1.5',method,genre) for method in METHODS[1:]]
            lines.append('| '+name+' | '+' | '.join(f'{v:.2f}%' for v in vals)+' |')
            table.append([name]+[f'{v:.2f}%' for v in vals])
        html_tables.append((genre,table))
        lines += ['']
    lines += ['## Wider scatter, directional bias, and occasional slips', '',
              'Forced spatial + frequency word-error percentage at 60 mm. Conversations / essays are shown separately.', '',
              '| Layout | 1.5 mm scatter | 2.5 mm scatter | Thumb bias | Occasional slips |',
              '| --- | ---: | ---: | ---: | ---: |']
    for name in ROWS:
        vals=[f"{rate(name,60,p,'spatial-frequency','en-gum-conversation'):.2f}% / {rate(name,60,p,'spatial-frequency','en-gum-essay'):.2f}%" for p in PROFILES[1:]]
        lines.append('| '+name+' | '+' | '.join(vals)+' |')
    lines += ['', '## Width sensitivity', '',
              'At 1.5 mm scatter, forced spatial + frequency errors. A wider simulated board changes geometry; it does not reproduce a different physical grip.', '',
              '| Layout | Conversation 60 → 70 mm | Essay 60 → 70 mm |', '| --- | ---: | ---: |']
    for name in ROWS:
        vals=[f"{rate(name,60,'scatter-1.5','spatial-frequency',g):.2f}% → {rate(name,70,'scatter-1.5','spatial-frequency',g):.2f}%" for g in ('en-gum-conversation','en-gum-essay')]
        lines.append('| '+name+' | '+' | '.join(vals)+' |')
    lines += ['', '## Correction harm and uncertainty', '',
              'Damage below counts previously correct literal words changed to wrong words, per 1,000 total word occurrences. '
              'Recovered valid errors counts cases where the literal was a wrong dictionary word but the forced decoder recovered the intended word. '
              'The conservative policy retains all valid literal words, so it cannot recover those errors.', '',
              '| Genre / layout | Forced damage per 1,000 | Conservative damage per 1,000 | Forced valid errors recovered | Forced error across seeds |',
              '| --- | ---: | ---: | ---: | ---: |']
    for genre in ('en-gum-conversation','en-gum-essay'):
        for name in ROWS:
            rs=select(name,60,'scatter-1.5','spatial-frequency',genre)
            cs=select(name,60,'scatter-1.5','conservative',genre)
            lo=min(100*r['word_errors']/r['words'] for r in rs);hi=max(100*r['word_errors']/r['words'] for r in rs)
            valid=sum(r['raw_valid_word_errors'] for r in rs)
            recovered=sum(r['recovered_valid_word_errors'] for r in rs)
            lines.append(f"| {genre.removeprefix('en-gum-')} / {name} | {fraction(rs,'damaged_correct_words',1000):.2f} | {fraction(cs,'damaged_correct_words',1000):.2f} | {recovered}/{valid} | {lo:.2f}–{hi:.2f}% |")
    lines += ['', 'Seed ranges describe Monte Carlo variation only. Conversation and essay each contain very few source documents; '
              'they cannot support a dependable per-genre population confidence interval. '
              'The paired document bootstrap below is an exploratory corpus-mixture comparison, not a general English or human-performance claim.', '']
    # Keep all noise replicates from a source document in the same resampling cluster.
    docdata=collections.defaultdict(lambda: np.zeros(2))
    with gzip.open(source/'documents.jsonl.gz','rt',encoding='utf-8') as f:
        for line in f:
            r=json.loads(line)
            if r['width_mm']==60 and r['profile']=='scatter-1.5' and r['method']=='spatial-frequency':
                docdata[(r['layout'],r['document'])] += [r['counts'][6],r['counts'][0]]
    baseline=np.array([docdata[('qwerty',d['id'])] for d in manifest['documents']])
    samples=np.random.default_rng(89123).integers(0,len(baseline),size=(2000,len(baseline)))
    lines += ['| Alternative minus QWERTY, 60 mm / 1.5 mm | Word-error difference (percentage points) | Exploratory 95% document bootstrap |',
              '| --- | ---: | ---: |']
    for name in list(ROWS)[1:]:
        alt=np.array([docdata[(name,d['id'])] for d in manifest['documents']])
        assert np.array_equal(alt[:,1],baseline[:,1])
        delta=100*(alt[:,0].sum()-baseline[:,0].sum())/baseline[:,1].sum()
        sim=100*(alt[samples,0].sum(1)-baseline[samples,0].sum(1))/baseline[samples,1].sum(1)
        lo,hi=np.quantile(sim,[.025,.975])
        lines.append(f'| {name} | {delta:+.3f} | {lo:+.3f} to {hi:+.3f} |')
    lines += ['', '## Movement proxies', '',
              '60 mm conversation tokens. Distance is per movement between one thumb’s consecutive letters within a word. '
              'Thumb is assigned by screen half. There is no Space, cross-word travel, or learned human thumb assignment.', '',
              '| Layout | Distance per same-thumb movement | Alternating-thumb fraction |', '| --- | ---: | ---: |']
    for name in ROWS:
        m=next(r for r in result['movement'] if r['layout']==name and r['width_mm']==60 and r['genre']=='en-gum-conversation')
        lines.append(f"| {name} | {m['same_thumb_travel_mm']/m['same_thumb_moves']:.2f} mm | {100*m['alternations']/m['transitions']:.2f}% |")
    # Reconstruct literal events only, with the same length grouping and RNG ordering.
    vocab, documents, items, _, _ = load_inputs()
    buckets=collections.defaultdict(list)
    for word,doc in items:
        buckets[len(word)].append((word,doc))
    confusions={}
    lines += ['', '## Most frequent valid-word confusions', '',
              'Automatically selected by occurrence count, then lexical order, across all 15 genres at 60 mm / 1.5 mm scatter / three seeds. '
              'These illustrate errors only; they supply no production exception or layout weight.', '',
              '| Layout | Intended → literal (occurrences across three noise replicates) |', '| --- | --- |']
    for name in ROWS:
        counts=collections.Counter();centers=geometry(name,60)
        for seed in manifest['seeds']:
            for length,records in sorted(buckets.items()):
                expected=np.array([w for w,_ in records]);ids=word_ids(expected)
                points=contacts(centers[ids],60,'scatter-1.5',seed+length*1000)
                rawids=((points[:,:,None,:]-centers[None,None,:,:])**2).sum(3).argmin(2)
                for intended,idsrow in zip(expected,rawids):
                    raw=''.join(chr(int(c)+97) for c in idsrow)
                    if raw!=intended and raw in vocab:
                        counts[(str(intended),raw)]+=1
        ordered=sorted(counts.items(),key=lambda item:(-item[1],item[0]))
        confusions[name]=[{'intended':pair[0],'literal':pair[1],'count':count,'intended_source_score':vocab.get(pair[0]),'literal_source_score':vocab[pair[1]]} for pair,count in ordered]
        lines.append('| '+name+' | '+'; '.join(f'`{a}` → `{b}` ({count})' for (a,b),count in ordered[:5])+' |')
    (out/'valid-word-confusions.json').write_text(json.dumps(confusions,indent=2)+'\n',encoding='utf-8')
    lines += ['', '## Interpretation limits', '',
              '- Letter-only, centered-row adaptations use nearest-letter Voronoi regions. Punctuation positions are removed; empty margins cannot register non-letter keys. These results do not reproduce a shipping keyboard’s full hit map.',
              '- Scatter, bias and slips are assumed distributions. They exclude motor learning, reach-dependent variance, occlusion, correlated trajectories, gesture collisions, missed/extra contacts and physical latency.',
              '- Word boundaries and length are given. Exhaustive same-length dictionary search is an offline experiment, not the current MinIME decoder and not a measured mobile implementation.',
              '- Fixed source-score scaling and conservative margins are not calibrated probabilities. Context is absent. A forced choice can damage unknown words; clean-center errors expose this rather than hiding it.',
              '- Previously evaluated corpus; 26 document clusters and a specific genre mixture. No fresh holdout or user study. Do not promote a layout based solely on these results.', '',
              '## Reproduce', '', '```powershell',
              'python tools/simulate_english_layouts.py --self-test',
              'python tools/simulate_english_layouts.py --out artifacts/layout-imprecision-new',
              'python tools/report_english_layouts.py artifacts/layout-imprecision-new --out artifacts/layout-report-new', '```', '',
              f"Full simulation desktop runtime: {result['elapsed_seconds']:.1f} seconds. This is experiment execution time, not typing or touch latency.", '']
    (out/'RESULTS.md').write_text('\n'.join(lines),encoding='utf-8')
    # A diagram of the exact geometry used, not a proposed polished keyboard UI.
    diagrams=[]
    for name in ROWS:
        centers=geometry(name,60)
        rects=[]
        for i,(x,y) in enumerate(centers):
            pitch=(60-8 if name=='split-qwerty' else 60)/10
            rects.append(f'<rect x="{(x-pitch/2)*5:.2f}" y="{(y-4.8)*5:.2f}" width="{pitch*5-1:.2f}" height="48" rx="4" fill="#dcebe6"/><text x="{x*5:.2f}" y="{y*5+5:.2f}" text-anchor="middle" font-size="16">{chr(i+65)}</text>')
        diagrams.append(f'<section><h2>{html.escape(name)}</h2><svg viewBox="0 0 300 150" role="img" aria-label="Simulated {name} letter positions">'+''.join(rects)+'</svg></section>')
    tables=[]
    for genre,table in html_tables:
        header=['Layout','Literal error','Valid error','Spatial only','Spatial + frequency','Conservative']
        tables.append('<h2>'+html.escape(genre.removeprefix('en-gum-').title())+'</h2><div class="scroll"><table><thead><tr>'+''.join('<th>'+h+'</th>' for h in header)+'</tr></thead><tbody>'+''.join('<tr>'+''.join('<td>'+html.escape(c)+'</td>' for c in row)+'</tr>' for row in table)+'</tbody></table></div>')
    page='<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>English layout imprecision pilot</title><style>body{font:17px system-ui;max-width:1050px;margin:32px auto;padding:0 18px;color:#213a32;background:#fafbf9}h1{font-size:30px}.layouts{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:24px}section{background:white;padding:18px;border:1px solid #dcebe6;border-radius:12px}svg{width:100%;max-width:400px}th,td{padding:10px;border-bottom:1px solid #dcebe6;text-align:right}th:first-child,td:first-child{text-align:left}.scroll{overflow:auto}.note{padding:16px;background:#f4ecd8;border-radius:10px}</style><h1>English layouts under touch imprecision</h1><p>20,096 word occurrences · 42,025-word dictionary · 120 simulated conditions</p><p class="note">Simulation only. These are letter-only adaptations, not complete keyboards. No human speed or touch-latency claim.</p><div class="layouts">'+''.join(diagrams)+'</div><p>60 mm board, 1.5 mm scatter; three-seed averages. Lower word-error percentages are better. Forced dictionary decoders can damage correctly typed unfamiliar words.</p>'+''.join(tables)+'<p><a href="RESULTS.md">Full results, noise profiles, correction harm and limitations</a></p></html>'
    (out/'index.html').write_text(page,encoding='utf-8')
    shutil.copyfile(source/'manifest.json',out/'manifest.json')
    shutil.copyfile(source/'documents.jsonl.gz',out/'documents.jsonl.gz')
    (out/'summary.json.gz').write_bytes(gzip.compress((source/'summary.json').read_bytes(),mtime=0))
    print(out/'RESULTS.md')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('--out',type=Path,required=True)
    a=p.parse_args();report(a.source,a.out)
