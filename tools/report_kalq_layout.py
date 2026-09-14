"""Report the KALQ extension with explicit word-error denominators."""
import argparse
import collections
import gzip
import hashlib
import html
import json
from pathlib import Path
import shutil
from simulate_kalq_layout import BLOCKS, VARIANTS, geometry

NAMES=['qwerty','kalq-30mm','kalq-40mm']


def report(source,out):
    manifest=json.loads((source/'manifest.json').read_text())
    data=json.loads((source/'summary.json').read_text());rows=data['results']
    assert len(rows)==3*2*5*3*4*15 and len({r['layout'] for r in rows})==3
    prior=json.loads(gzip.decompress(Path('docs/two-thumb-english/simulation/summary.json.gz').read_bytes()))
    old=[r for r in prior['results'] if r['layout']=='qwerty']
    new=[r for r in rows if r['layout']=='qwerty']
    assert old==new, 'QWERTY baseline changed'
    fields=manifest['fields'];totals=collections.defaultdict(lambda:[0]*len(fields))
    with gzip.open(source/'documents.jsonl.gz','rt',encoding='utf-8') as f:
        for line in f:
            r=json.loads(line);key=tuple(r[k] for k in ('layout','width_mm','profile','seed','method','genre'))
            totals[key]=[a+b for a,b in zip(totals[key],r['counts'])]
    for r in rows:
        key=tuple(r[k] for k in ('layout','width_mm','profile','seed','method','genre'))
        assert totals[key]==[r[k] for k in fields]
        assert r['word_errors']==r['raw_word_errors']-r['recovered_errors']+r['damaged_correct_words']
        if r['method']=='literal' and r['profile']=='center':assert r['word_errors']==0
        if r['method']=='conservative':assert r['recovered_valid_word_errors']==0
    def chosen(name,width,profile,method,genre):
        return [r for r in rows if (r['layout'],r['width_mm'],r['profile'],r['method'],r['genre'])==(name,width,profile,method,genre)]
    def counts(name,width,profile,method,genre,field='word_errors'):
        rs=chosen(name,width,profile,method,genre)
        return sum(r[field] for r in rs),sum(r['words'] for r in rs)
    def cell(name,width,profile,method,genre):
        n,d=counts(name,width,profile,method,genre)
        return f'{n:,}/{d:,} ({100*n/d:.2f}%)'
    out.mkdir(parents=True,exist_ok=True)
    lines=['# KALQ under simulated touch imprecision','',
           '**Every error percentage below is incorrect whole words divided by word attempts, multiplied by 100.** '
           'One or more wrong letters makes a word incorrect. This is not an individual touch-miss rate or a speed measurement. '
           'After-correction errors include errors the decoder introduces. Counts combine three matched noise seeds.', '',
           'The original KALQ paper reported 37 WPM after training six participants on a 7-inch tablet. '
           'That is separate from this phone-width simulation. '
           '[Authors’ paper, Figure 1](https://www.pokristensson.com/pubs/OulasvirtaEtAlCHI2013.pdf).', '',
           '## Geometry and test scope','',
           '- QWERTY: original pilot, 30 mm letter-region height and 10 mm vertical pitch.',
           '- KALQ 30 mm: original block/letter positions, four left rows compressed to 7.5 mm vertical pitch.',
           '- KALQ 40 mm: 10 mm vertical pitch, requiring 10 mm more height than the baseline.',
           '- Both KALQ versions have four columns per block and an 8 mm central gap. Space-key holes remain, but space activation is not simulated.',
           '- Same 20,096 eligible word occurrences, 42,025-word dictionary, decoder, widths, error profiles, and seeds. '
           'The QWERTY rerun reproduces all earlier per-genre counts exactly.', '',
           '## Primary comparison: 60 mm width, 1.5 mm scatter','',
           'Each axis has 1.5 mm Gaussian standard deviation. These are assumed contact errors, not measured human behavior. '
           '“Forced spatial + frequency” always selects a dictionary word. “Conservative” can retain the literal input and never replaces a recognized dictionary word.', '']
    htables=[]
    for genre in ('en-gum-conversation','en-gum-essay'):
        n=sum(d['genre']==genre for d in manifest['documents'])
        lines += [f"### {genre.removeprefix('en-gum-').title()} ({n} source document(s))",'',
                  '| Layout | Before correction | Forced spatial + frequency | Conservative |',
                  '| --- | ---: | ---: | ---: |']
        table=[]
        for name in NAMES:
            vals=[cell(name,60,'scatter-1.5',m,genre) for m in ('literal','spatial-frequency','conservative')]
            lines.append('| '+name+' | '+' | '.join(vals)+' |');table.append([name]+vals)
        lines += [''];htables.append((genre,table))
    lines += ['## Contact-profile sensitivity','',
              'Forced spatial + frequency word errors at 60 mm width. Each cell shows conversation / essay percentages. '
              'Denominators remain 4,101 conversation attempts and 3,201 essay attempts per cell across three seeds.', '',
              '| Layout | 1.5 mm scatter | 2.5 mm scatter | Directional bias | Occasional slips |',
              '| --- | ---: | ---: | ---: | ---: |']
    for name in NAMES:
        vals=[]
        for p in ('scatter-1.5','scatter-2.5','thumb-bias','slips'):
            ns=[counts(name,60,p,'spatial-frequency',g) for g in ('en-gum-conversation','en-gum-essay')]
            vals.append(' / '.join(f'{100*n/d:.2f}%' for n,d in ns))
        lines.append('| '+name+' | '+' | '.join(vals)+' |')
    lines += ['','## 70 mm width','',
              '1.5 mm scatter, forced spatial + frequency. Counts are incorrect words / attempts.', '',
              '| Layout | Conversations | Essays |','| --- | ---: | ---: |']
    for name in NAMES:
        lines.append('| '+name+' | '+' | '.join(cell(name,70,'scatter-1.5','spatial-frequency',g) for g in ('en-gum-conversation','en-gum-essay'))+' |')
    lines += ['','## Correction harm','',
              'At 60 mm / 1.5 mm, counts of previously correct literal words changed into wrong words by correction. '
              'These errors are already included in the after-correction word-error totals.', '',
              '| Genre / layout | Forced damage | Conservative damage |','| --- | ---: | ---: |']
    for genre in ('en-gum-conversation','en-gum-essay'):
        for name in NAMES:
            ns=[counts(name,60,'scatter-1.5',m,genre,'damaged_correct_words') for m in ('spatial-frequency','conservative')]
            lines.append('| '+genre.removeprefix('en-gum-')+' / '+name+' | '+' | '.join(f'{n} in {d:,} total attempts' for n,d in ns)+' |')
    lines += ['','## Limits and validation','',
              '- Only letter positions and confusion are tested. Accidental internal-space activation, true segmentation, extra/missing taps, gestures, learned grips and timing are absent.',
              '- Nearest-letter regions can absorb space holes. Consequently this is not a full KALQ-versus-QWERTY usability comparison.',
              '- One essay document and two conversation documents; corpus already evaluated. Three noise seeds are not three independent populations. No fresh-holdout or human-speed claim.',
              '- The 40 mm variant spends extra height; any advantage must be considered with that cost. The 30 mm variant is the equal-height comparison.',
              '- All 90 conditions completed. 5,400 aggregate rows reconcile with per-document counts; clean literal controls and error conservation pass; all original QWERTY counts match.',
              '- An initial wrapper preflight failed before simulation because the reference QWERTY row definition was missing. The corrected wrapper keeps QWERTY and reruns it; no result or threshold was changed in response to KALQ outcomes.', '',
              '```powershell','python tools/simulate_kalq_layout.py --out artifacts/kalq-new',
              'python tools/report_kalq_layout.py artifacts/kalq-new --out artifacts/kalq-report-new','```','',
              'Reproduction also requires the referenced paper at `artifacts/kalq-reference/paper.pdf` for its source hash. '
              'No paper pages are redistributed in this evidence package.', '',
              f"Desktop experiment execution took {data['elapsed_seconds']:.1f} seconds. That is batch runtime, not touch latency.",'']
    (out/'RESULTS.md').write_text('\n'.join(lines),encoding='utf-8')
    diagrams=[]
    for name in NAMES:
        centers=geometry(name,60);height=VARIANTS.get(name,30);px=(60-8)/8 if name in VARIANTS else 6;py=height/4 if name in VARIANTS else 10
        shapes=[]
        for i,(x,y) in enumerate(centers):
            shapes.append(f'<rect x="{(x-px/2)*5:.2f}" y="{(y-py/2)*5:.2f}" width="{px*5-1:.2f}" height="{py*5-1:.2f}" rx="3" fill="#dcebe6"/><text x="{x*5:.2f}" y="{y*5+5:.2f}" text-anchor="middle" font-size="15">{chr(i+65)}</text>')
        diagrams.append(f'<section><h2>{name}</h2><svg viewBox="0 0 300 200" role="img" aria-label="{name} simulation geometry">'+''.join(shapes)+'</svg></section>')
    tables=[]
    for genre,table in htables:
        tables.append('<h2>'+genre.removeprefix('en-gum-').title()+'</h2><div style="overflow:auto"><table><tr><th>Layout</th><th>Before correction</th><th>Forced spatial + frequency</th><th>Conservative</th></tr>'+''.join('<tr>'+''.join('<td>'+html.escape(s)+'</td>' for s in row)+'</tr>' for row in table)+'</table></div>')
    page='<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>KALQ comparison</title><style>body{font:17px system-ui;max-width:1080px;margin:32px auto;padding:0 20px;color:#213a32;background:#fafbf9}.layouts{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:20px}section{padding:16px;background:white;border:1px solid #dcebe6;border-radius:12px}svg{width:100%}td,th{padding:10px;border-bottom:1px solid #ddd;text-align:right}td:first-child{text-align:left}</style><h1>KALQ: phone-width simulation</h1><p>Every percentage is incorrect whole words / attempted words × 100. Counts include three matched noise seeds.</p><p>60 mm width; 1.5 mm scatter. Letter-only simulation: internal space-key activation is excluded. The 40 mm version uses extra height.</p><div class="layouts">'+''.join(diagrams)+'</div>'+''.join(tables)+'<p><a href="RESULTS.md">Full results, denominators and limitations</a></p></html>'
    (out/'index.html').write_text(page,encoding='utf-8')
    shutil.copyfile(source/'manifest.json',out/'manifest.json');shutil.copyfile(source/'documents.jsonl.gz',out/'documents.jsonl.gz')
    (out/'summary.json.gz').write_bytes(gzip.compress((source/'summary.json').read_bytes(),mtime=0))
    print('PASS QWERTY reproduction, 5400 aggregate reconciliations, controls and error conservation; report',out/'RESULTS.md')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('--out',type=Path,required=True)
    args=p.parse_args();report(args.source,args.out)
