"""Reconcile and display compact-layout regression evidence with denominators."""
import argparse
import collections
import gzip
import html
import json
from pathlib import Path
import shutil
import simulate_compact_layouts as compact

LABELS = {'qwerty':'QWERTY', 'colemak':'Colemak', 'dvorak':'Dvorak',
          'kalq-30mm':'Split KALQ', 'kalq-joined':'Joined KALQ',
          'kalq-reflow':'KALQ reflow 9/8/9', 'compact-aligned':'Aligned 9/8/9',
          'compact-staggered':'Staggered 9/8/9'}


def polygon(target, others, width):
    """Clip the envelope by nearest-center bisectors; diagram matches hit model."""
    poly = [(0.,0.),(width,0.),(width,40.),(0.,40.)]
    for other in others:
        if all(target == other): continue
        a,b = 2*(other-target); c = float(other@other-target@target)
        result=[]
        for start,end in zip(poly,poly[1:]+poly[:1]):
            ds=a*start[0]+b*start[1]-c; de=a*end[0]+b*end[1]-c
            if ds<=1e-9:result.append(start)
            if (ds<0) != (de<0):
                t=ds/(ds-de);result.append((start[0]+t*(end[0]-start[0]),start[1]+t*(end[1]-start[1])))
        poly=result
    return poly


def diagram(name):
    import numpy as np
    targets=np.concatenate([compact.geometry(name,60),compact.controls(name,60)])
    labels=list('ABCDEFGHIJKLMNOPQRSTUVWXYZ')+['Shift',',','Space','.','Del']+['Space']*(len(targets)-31)
    shapes=[]
    for i,t in enumerate(targets):
        pts=' '.join(f'{x*5:.2f},{y*5:.2f}' for x,y in polygon(t,targets,60))
        shapes.append(f'<polygon points="{pts}" fill="{"#deeee5" if i<26 else "#eee0cf"}" stroke="white" stroke-width="1"/>')
        shapes.append(f'<text x="{t[0]*5:.2f}" y="{t[1]*5+4:.2f}" text-anchor="middle" font-size="{13 if i<26 else 9}">{labels[i]}</text>')
    return '<section><h3>'+LABELS[name]+'</h3><svg viewBox="0 0 300 200" role="img" aria-label="'+LABELS[name]+' abstract hit regions">'+''.join(shapes)+'</svg></section>'


def report(source,out):
    m=json.loads((source/'manifest.json').read_text())
    data=json.loads((source/'summary.json').read_text()); rows=data['results']
    assert m['extension_code_sha256']==compact.pilot.sha(Path(compact.__file__))
    assert m['extension_plan_sha256']==compact.pilot.sha(out/'PLAN.md')
    old=json.loads(gzip.decompress(Path('docs/two-thumb-english/simulation/summary.json.gz').read_bytes()))
    kalq=json.loads(gzip.decompress(Path('docs/two-thumb-english/kalq/summary.json.gz').read_bytes()))
    for name,prior in [('qwerty',old),('kalq-30mm',kalq)]:
        assert [r for r in rows if r['layout']==name]==[r for r in prior['results'] if r['layout']==name], name
    fields=m['fields']; totals=collections.defaultdict(lambda:[0]*len(fields))
    keys=('layout','width_mm','profile','seed','method','genre')
    with gzip.open(source/'documents.jsonl.gz','rt',encoding='utf-8') as f:
        for line in f:
            r=json.loads(line); k=tuple(r[x] for x in keys)
            totals[k]=[a+b for a,b in zip(totals[k],r['counts'])]
    assert len(rows)==6*2*5*3*4*15
    for r in rows:
        assert totals[tuple(r[x] for x in keys)]==[r[x] for x in fields]
        assert r['word_errors']==r['raw_word_errors']-r['recovered_errors']+r['damaged_correct_words']
    rows += [r for r in old['results'] if r['layout'] in ('colemak','dvorak')]
    movement=data['movement']+[r for r in old['movement'] if r['layout'] in ('colemak','dvorak')]
    def count(name,genre,profile='scatter-1.5',method='conservative',field='word_errors',width=60):
        rs=[r for r in rows if (r['layout'],r['genre'],r['profile'],r['method'],r['width_mm'])==(name,genre,profile,method,width)]
        assert len(rs)==3
        return sum(r[field] for r in rs),sum(r['words'] for r in rs)
    def cell(*args,**kwargs):
        n,d=count(*args,**kwargs);return f'{n:,}/{d:,} ({100*n/d:.2f}%)'
    lines=['# Compact phone layout comparison','',
           'Every word-error percentage means incorrect whole words / attempted words × 100. '
           'One wrong letter makes a word wrong. Counts combine three matched noise seeds. '
           'These are simulated outcomes, not measured typing speed or human accuracy.','',
           'All layouts use a 30 mm letter-region envelope. Primary condition: 60 mm wide, '
           'Gaussian scatter with 1.5 mm standard deviation on each axis. QWERTY and split KALQ '
           'were rerun and reproduce prior results exactly. Colemak and Dvorak reuse the pinned prior run.','',
           'Raw means nearest letter. Conservative correction replaces only unknown literal words when '
           'the fixed confidence criteria pass; it leaves recognized words unchanged. Forced correction '
           'always chooses a dictionary word. Neither is the Android decoder.','']
    for genre in ('en-gum-conversation','en-gum-essay'):
        lines += ['## '+genre.removeprefix('en-gum-').title(),'',
                  '| Layout | Raw wrong words | Conservative wrong words | Forced wrong words |',
                  '| --- | ---: | ---: | ---: |']
        for name in LABELS:
            lines.append('| '+LABELS[name]+' | '+' | '.join(cell(name,genre,method=method) for method in ('literal','conservative','spatial-frequency'))+' |')
        lines += ['','Valid-word confusion means a wrong literal word that nevertheless exists in the source dictionary; '
                  'membership does not certify that it is common English. Damage means correction spoiled a previously correct word.', '',
                  '| Layout | Valid-word confusions / attempts | Conservative damage / attempts | Same-thumb travel per move |',
                  '| --- | ---: | ---: | ---: |']
        for name in LABELS:
            mv=next(x for x in movement if (x['layout'],x['genre'],x['width_mm'])==(name,genre,60))
            lines.append('| '+LABELS[name]+' | '+cell(name,genre,field='raw_valid_word_errors')+' | '+cell(name,genre,field='damaged_correct_words')+f" | {mv['same_thumb_travel_mm']/mv['same_thumb_moves']:.2f} mm |")
        lines+=['','## '+genre.removeprefix('en-gum-').title()+' stress conditions','',
                'Conservative wrong words / attempts. Travel above uses fixed left/right thumb assignment; no timing model.', '',
                '| Layout | 2.5 mm scatter, 60 mm | Inward bias, 60 mm | Slips, 60 mm | 1.5 mm scatter, 70 mm |',
                '| --- | ---: | ---: | ---: | ---: |']
        for name in LABELS:
            lines.append('| '+LABELS[name]+' | '+' | '.join([cell(name,genre,profile=p) for p in ('scatter-2.5','thumb-bias','slips')]+[cell(name,genre,width=70)])+' |')
        lines+=['']
    control=json.loads((source/'controls.json').read_text())
    assert len(control)==6*2*5*3*15
    literal={tuple(r[k] for k in ('layout','genre','width_mm','profile','seed')):r for r in rows if r['method']=='literal'}
    for r in control:
        ref=literal[tuple(r[k] for k in ('layout','genre','width_mm','profile','seed'))]
        assert r['letter_attempts']==ref['characters'] and r['space_attempts']==ref['words']
        assert 0<=r['letters_hit_controls']<=r['letter_attempts']
        assert 0<=r['spaces_hit_letters']<=r['space_errors']<=r['space_attempts']
    lines += ['## Separate control-boundary audit','',
              'Common bottom control row adds 10 mm: total 40 mm height. The diagrams show abstract '
              'nearest-center hit regions, not proposed Android key shapes. Internal KALQ Space keys '
              'also participate. Letter-to-control and intended-Space errors are not included in the '
              'word-decoder figures, which assume correct word boundaries.','',
              '| Layout / genre | Letter taps that hit controls / letter taps | Space taps that hit letters / Space taps |',
              '| --- | ---: | ---: |']
    for name in compact.NAMES:
        for genre in ('en-gum-conversation','en-gum-essay'):
            rs=[r for r in control if (r['layout'],r['genre'],r['width_mm'],r['profile'])==(name,genre,60,'scatter-1.5')]
            cells=[]
            for n,d in [('letters_hit_controls','letter_attempts'),('spaces_hit_letters','space_attempts')]:
                a=sum(r[n] for r in rs);b=sum(r[d] for r in rs);cells.append(f'{a:,}/{b:,} ({100*a/b:.2f}%)')
            lines.append('| '+LABELS[name]+' / '+genre.removeprefix('en-gum-')+' | '+' | '.join(cells)+' |')
    lines += ['','## Scope and reproducibility','',
              '- 20,096 eligible word occurrences from 26 documents / 15 genres; 42,025 dictionary words. '
              'Conversations: 1,367 occurrences from two documents, 4,101 attempts per condition. '
              'Essays: 1,067 occurrences from one document, 3,201 attempts. All genres remain in raw results.',
              '- Same inspected GUM corpus as earlier experiments. No fresh holdout or human trial. '
              'Layout differences were frozen before results, with no corpus-driven search or weight tuning.',
              '- Apostrophe-containing tokens, non-ASCII and over-20-letter tokens are excluded. '
              '738 eligible occurrences are outside the dictionary. Forced correction cannot retain those spellings.',
              '- Nearest-center geometry may absorb otherwise blank regions. The separate control audit is '
              'not a complete segmentation, deletion, insertion, correction-effort, fatigue or learning model.',
              '- Staggered packing means stretched hexagonal Voronoi interiors, not regular hexagons or Typewise. '
              'KALQ reflow changes thumb assignments and is not the published layout.',
              '- 10,800 new aggregate rows reconcile with per-document telemetry; both historical baselines match. '
              'Clean letter and control taps pass. All raw conditions, including negative results, are preserved.',
              f"- Batch simulation took {data['elapsed_seconds']:.1f} seconds on desktop; this is not typing latency.", '',
              'Run `python tools/simulate_compact_layouts.py --out artifacts/compact-new`, then '
              '`python tools/report_compact_layouts.py artifacts/compact-new --out docs/two-thumb-english/compact`.','']
    (out/'RESULTS.md').write_text('\n'.join(lines),encoding='utf-8')
    aggregated=[]
    for name in LABELS:
        for genre in ('en-gum-conversation','en-gum-essay'):
            for width in (60,70):
                for profile in compact.pilot.PROFILES:
                    for method in ('literal','conservative','spatial-frequency'):
                        n,d=count(name,genre,profile,method,width=width)
                        aggregated.append([name,genre,width,profile,method,n,d])
    page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Compact phone layouts</title>
<style>body{font:16px system-ui;max-width:1100px;margin:24px auto;padding:0 20px;background:#fafbf9;color:#203a31}.layouts{display:grid;grid-template-columns:repeat(auto-fit,minmax(270px,1fr));gap:16px}section{border:1px solid #cdded4;border-radius:12px;padding:12px;background:white}svg{width:100%}select{padding:8px;margin:5px}table{border-collapse:collapse;width:100%}td,th{padding:10px;border-bottom:1px solid #cdded4;text-align:right}td:first-child,th:first-child{text-align:left}</style>
<h1>Compact, continuous phone layouts</h1><p>All letter regions: 30 mm high. Diagrams: 60 mm wide plus a common 10 mm control row. Shapes show modeled nearest-center hit regions, not finished key designs.</p>
<div class="layouts">'''+''.join(diagram(n) for n in compact.NAMES)+'''</div>
<h2>Simulated wrong whole words</h2><p>Each percentage = incorrect whole words / attempted words × 100. Three matched noise seeds; previously evaluated corpus. Not human speed or touch latency. Word decoding excludes control errors.</p>
<label>Text <select id="genre"><option value="en-gum-conversation">Conversations</option><option value="en-gum-essay">Essays</option></select></label>
<label>Width <select id="width"><option>60</option><option>70</option></select> mm</label>
<label>Touch <select id="profile"><option value="scatter-1.5">1.5 mm scatter</option><option value="scatter-2.5">2.5 mm scatter</option><option value="thumb-bias">Inward bias</option><option value="slips">Occasional slips</option><option value="center">Perfect centers</option></select></label>
<table><thead><tr><th>Layout</th><th>Raw</th><th>Conservative correction</th><th>Forced correction</th></tr></thead><tbody id="results"></tbody></table>
<p>Primary denominators: 4,101 conversation word attempts; 3,201 essay word attempts. Reusing the same words across conditions does not add independent language evidence.</p><p><a href="RESULTS.md">Full results and separate control-error audit</a> · <a href="DECISION.md">Decision and limitations</a></p><script>
const rows='''+json.dumps(aggregated)+''', names='''+json.dumps(LABELS)+''';
function render(){const genre=document.getElementById('genre').value,width=+document.getElementById('width').value,profile=document.getElementById('profile').value;document.getElementById('results').innerHTML=Object.entries(names).map(([name,label])=>'<tr><td>'+label+'</td>'+['literal','conservative','spatial-frequency'].map(m=>{const r=rows.find(r=>r[0]===name&&r[1]===genre&&r[2]===width&&r[3]===profile&&r[4]===m);return '<td>'+r[5].toLocaleString()+'/'+r[6].toLocaleString()+' ('+(100*r[5]/r[6]).toFixed(2)+'%)</td>'}).join('')+'</tr>').join('')};document.querySelectorAll('select').forEach(e=>e.addEventListener('change',render));render();</script></html>'''
    (out/'index.html').write_text(page,encoding='utf-8')
    # Standalone diagram for visual verification and a directly viewable artifact.
    from PIL import Image, ImageDraw, ImageFont
    import numpy as np
    picture=Image.new('RGB',(1080,1270),'#fafbf9'); draw=ImageDraw.Draw(picture)
    font_path='C:/Windows/Fonts/segoeui.ttf'
    title=ImageFont.truetype(font_path,29); normal=ImageFont.truetype(font_path,20)
    keyfont=ImageFont.truetype(font_path,21); small=ImageFont.truetype(font_path,15)
    draw.text((30,20),'Compact phone layouts: equal width and height',fill='#203a31',font=title)
    draw.text((30,65),'60 mm wide; 30 mm letters + 10 mm controls. Abstract hit regions.',fill='#203a31',font=normal)
    for index,name in enumerate(compact.NAMES):
        ox=30+(index%2)*530; oy=120+(index//2)*365
        draw.text((ox,oy),LABELS[name],fill='#203a31',font=normal)
        targets=np.concatenate([compact.geometry(name,60),compact.controls(name,60)])
        labels=list('ABCDEFGHIJKLMNOPQRSTUVWXYZ')+['Shift',',','Space','.','Del']+['Space']*(len(targets)-31)
        for i,t in enumerate(targets):
            vertices=[(ox+x*8,oy+40+y*8) for x,y in polygon(t,targets,60)]
            draw.polygon(vertices,fill='#deeee5' if i<26 else '#eee0cf',outline='white',width=2)
            draw.text((ox+t[0]*8,oy+40+t[1]*8),labels[i],anchor='mm',fill='#203a31',font=keyfont if i<26 else small)
    draw.text((30,1230),'These are simulation geometries, not finished keyboard designs.',fill='#203a31',font=normal)
    picture.save(out/'layouts.png')
    for name in ('manifest.json','documents.jsonl.gz'):
        shutil.copyfile(source/name,out/name)
    for name in ('summary','controls'):
        (out/(name+'.json.gz')).write_bytes(gzip.compress((source/(name+'.json')).read_bytes(),mtime=0))
    print('PASS both baseline reproductions, 10800 document reconciliations; report generated')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('source',type=Path);p.add_argument('--out',type=Path,required=True)
    a=p.parse_args();report(a.source,a.out)
