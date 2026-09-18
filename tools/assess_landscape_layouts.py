"""Frozen landscape geometry/tap sensitivity study, outside production code."""
import collections, csv, gzip, hashlib, io, json, math, re
from pathlib import Path
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/landscape-layout-study'
SIZES=[(640,320),(760,360),(880,400)]
NAMES=['compact','split-low','split-tall','side-panels']
PROFILES=['center','scatter-6dp','scatter-10dp','inward-bias','slips']
SEEDS=[20260918,20260919,20260920]
TITLE=dict(zip(NAMES,['Compact joined','Split · same height','Split · taller keys','Full-height sides']))
source=(ROOT/'core/src/main/java/dev/minime/core/JoinedKalq.java').read_text(encoding='utf-8')
ROWS=json.loads('['+re.search(r'ROWS=\{(.*?)\}',source).group(1)+']')
def java_string(name):return json.loads(re.search(name+r'=("(?:\\.|[^"\\])*")',source).group(1))
ascii_symbols,chinese_symbols=java_string('ASCII'),java_string('CHINESE')
SYMBOLS={ch:[ascii_symbols[i],chinese_symbols[i]] for i,ch in enumerate(''.join(ROWS).replace(' ',''))}

def layout(name,w,h):
    rowh=25.5 if name in ('compact','split-low') else 44 if name=='split-tall' else (h-88)/4
    footer=34 if name in ('compact','split-low') else 40
    total=48+4*rowh+footer
    top=h-total
    block=w/2 if name=='compact' else 240 if name!='side-panels' else 192
    cells=[]
    def add(label,x,y,cw,ch,kind='control',symbol=None):
        cells.append(dict(label=label,x=x,y=y,w=cw,h=ch,kind=kind,symbol=symbol))
    add('Candidates',0,top,w,48)
    for r in range(4):
        texts=[ROWS[r][:4],ROWS[r][4:]] if r<3 else ['^^dn','fv<<']
        for side,text in enumerate(texts):
            left=0 if side==0 else w-block
            for col,ch in enumerate(text):
                if r==3 and ch in '^<':
                    if (ch=='^' and col==0) or (ch=='<' and col==2):add('Shift' if ch=='^' else 'Delete',left+col*block/4,top+48+r*rowh,block/2,rowh)
                    continue
                add('Space' if ch==' ' else ch,left+col*block/4,top+48+r*rowh,block/4,rowh,'space' if ch==' ' else 'letter',SYMBOLS.get(ch))
    y=h-footer
    if name=='compact':
        specs=[('Symbols',1.6),(',',1),('Mode',.9),('Space',4),('.',1),('Enter',1.5)]
        total_weight=sum(v for _,v in specs);x=0
        for label,weight in specs:
            cw=w*weight/total_weight;add(label,x,y,cw,footer,'space' if label=='Space' else 'control');x+=cw
    else:
        for side,specs in enumerate([[('Symbols',1),(',',.75),('Mode',.75),('Space',1.5)],[('Space',2),('.',.75),('Enter',1.25)]]):
            x=0 if side==0 else w-block
            for label,weight in specs:
                cw=block*weight/4;add(label,x,y,cw,footer,'space' if label=='Space' else 'control');x+=cw
    # All rectangles fit and have unique, disjoint ownership.
    assert sorted(c['label'] for c in cells if c['kind']=='letter')==list('abcdefghijklmnopqrstuvwxyz')
    for i,a in enumerate(cells):
        assert min(a['x'],a['y'])>=-1e-7 and a['x']+a['w']<=w+1e-7 and a['y']+a['h']<=h+1e-7
        for b in cells[i+1:]:
            assert min(a['x']+a['w'],b['x']+b['w'])<=max(a['x'],b['x'])+1e-7 or min(a['y']+a['h'],b['y']+b['h'])<=max(a['y'],b['y'])+1e-7
    return dict(name=name,title=TITLE[name],width=w,height=h,top=top,block=block,row_height=rowh,footer=footer,cells=cells)

def load():
    rows=[];excluded=collections.Counter();sources={}
    def data(path):
        b=(ROOT/path).read_bytes();sources[path]=hashlib.sha256(b).hexdigest();return b
    for line in data('docs/conversation-ranking/corpus/gum-test.tsv').decode().splitlines():
        genre,identity,text,_=line.split('\t')
        filtered=''.join(c for c in text.lower() if c in 'abcdefghijklmnopqrstuvwxyz ')
        excluded[genre]+=len(text)-len(filtered)
        if filtered:rows.append((genre,identity.rsplit('-',1)[0],filtered))
    raw=gzip.decompress(data('docs/chinese-recovery/corpus.tsv.gz')).decode()
    for r in csv.DictReader(io.StringIO(raw),delimiter='\t'):
        if r['genre'] not in ('essay-regression','authored-conversation-regression'):continue
        genre='zh-'+r['genre']+'/'+r['condition'];text=r['raw']
        filtered=''.join(c for c in text if c in 'abcdefghijklmnopqrstuvwxyz')
        excluded[genre]+=len(text)-len(filtered)
        if filtered:rows.append((genre,r['id'].split(':')[0],filtered))
    data('core/src/main/java/dev/minime/core/JoinedKalq.java')
    return rows,sources,dict(excluded)

def hit(points,cells):
    result=np.full(len(points),-1,dtype=np.int16)
    for i,c in enumerate(cells):
        mask=(points[:,0]>=c['x'])&(points[:,0]<c['x']+c['w'])&(points[:,1]>=c['y'])&(points[:,1]<c['y']+c['h'])
        result[mask]=i
    return result

def intended(rows,design):
    cells=design['cells'];coords=np.array([[c['x']+c['w']/2,c['y']+c['h']/2] for c in cells])
    letters={c['label']:i for i,c in enumerate(cells) if c['kind']=='letter'}
    spaces=[i for i,c in enumerate(cells) if c['kind']=='space'];ids=[];groups=[];docs=[];travel=[]
    for genre,doc,text in rows:
        previous=None;last_thumb={};distance=0.;moves=0
        for ch in text:
            at=letters[ch] if ch!=' ' else min(spaces,key=lambda i:np.linalg.norm(coords[i]-(previous if previous is not None else np.array([design['width']/2,design['height']]))))
            center=coords[at];side=int(center[0]>=design['width']/2)
            if side in last_thumb:distance+=float(np.linalg.norm(center-last_thumb[side]));moves+=1
            last_thumb[side]=center;previous=center;ids.append(at);groups.append(genre);docs.append(doc)
        travel.append((genre,distance,moves))
    return np.array(ids),coords[np.array(ids)],np.array(groups),np.array(docs),travel

def main():
    OUT.mkdir(exist_ok=True);rows,sources,excluded=load()
    manifest=dict(baseline='88c32af',sources=sources,plan_sha256=hashlib.sha256((OUT/'PLAN.md').read_bytes()).hexdigest(),
        code_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),numpy=np.__version__,seeds=SEEDS,profiles=PROFILES,
        dimensions=SIZES,input_rows=len(rows),excluded_characters=excluded,role='Reused corpus, synthetic geometric sensitivity; not human accuracy or WPM')
    (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    results=[];designs=[];document_rows=[]
    for w,h in SIZES:
        for name in NAMES:
            design=layout(name,w,h);designs.append(design)
            cells=design['cells'];ids,centers,groups,docs,travel=intended(rows,design)
            kinds=np.array([c['kind'] for c in cells]);is_letter=kinds[ids]=='letter'
            reference=hit(centers,cells);assert np.array_equal(reference,ids),'Center identity failed'
            genre_names=sorted(set(groups));masks={g:groups==g for g in genre_names};masks['all']=np.ones(len(ids),dtype=bool)
            for seed in SEEDS:
                rng=np.random.default_rng(seed);z=rng.standard_normal(centers.shape);slip=rng.random(len(ids))<.1
                for profile in PROFILES:
                    sigma=0 if profile=='center' else 10 if profile=='scatter-10dp' else 6
                    delta=z*(np.where(slip,20.,6.)[:,None] if profile=='slips' else sigma)
                    if profile=='inward-bias':delta=delta+np.column_stack((np.where(centers[:,0]<w/2,4.,-4.),np.full(len(ids),2.)))
                    observed=hit(centers+delta,cells);correct=observed==ids;miss=observed<0
                    observed_kind=np.where(miss,'miss',kinds[np.maximum(0,observed)])
                    # Any Space target produces the same action.
                    correct|=(~is_letter)&(observed_kind=='space')
                    for genre,mask in masks.items():
                        letter=mask&is_letter;space=mask&~is_letter
                        results.append(dict(layout=name,width=w,height=h,profile=profile,seed=seed,genre=genre,
                            letters=int(letter.sum()),letter_hits=int((letter&correct).sum()),letter_substitutions=int((letter&~correct&(observed_kind=='letter')).sum()),
                            accidental_space=int((letter&(observed_kind=='space')).sum()),controls=int((letter&(observed_kind=='control')).sum()),misses=int((letter&miss).sum()),
                            spaces=int(space.sum()),space_hits=int((space&correct).sum())))
                    # Retain document-cluster evidence for the main moderate condition.
                    if profile=='scatter-6dp':
                        for genre in genre_names:
                            for doc in sorted(set(docs[masks[genre]])):
                                m=masks[genre]&(docs==doc)&is_letter
                                document_rows.append(dict(layout=name,width=w,height=h,seed=seed,genre=genre,document=doc,letters=int(m.sum()),hits=int((m&correct).sum())))
            letters=[c for c in cells if c['kind']=='letter']
            occupied=sum(c['w']*c['h'] for c in cells)
            design['metrics']=dict(letter_width=letters[0]['w'],letter_height=letters[0]['h'],reserved_bottom_height=None if name=='side-panels' else h-design['top'],
                clear_above_height=design['top'],uncovered_percent=100*(1-occupied/(w*h)),center_gap=w-2*design['block'],
                mean_inward_reach_dp=float(np.mean(np.minimum(centers[is_letter,0],w-centers[is_letter,0]))),
                mean_same_thumb_travel_dp=sum(v[1] for v in travel)/sum(v[2] for v in travel))
            print(w,name,'contacts',len(ids),flush=True)
    payload=json.dumps(dict(designs=designs,results=results),ensure_ascii=False,separators=(',',':'))
    (OUT/'results.json.gz').write_bytes(gzip.compress(payload.encode(),mtime=0))
    (OUT/'documents.jsonl.gz').write_bytes(gzip.compress(('\n'.join(json.dumps(r) for r in document_rows)+'\n').encode(),mtime=0))
    # Browser preview consumes the exact same rectangles as the evaluation.
    summary=[]
    for design in designs:
        relevant=[r for r in results if r['layout']==design['name'] and r['width']==design['width'] and r['genre']=='all']
        for profile in PROFILES:
            rr=[r for r in relevant if r['profile']==profile];hits=sum(r['letter_hits'] for r in rr);total=sum(r['letters'] for r in rr)
            summary.append(dict(layout=design['name'],width=design['width'],profile=profile,hits=hits,attempts=total,percent=100*hits/total,
                range=[min(100*r['letter_hits']/r['letters'] for r in rr),max(100*r['letter_hits']/r['letters'] for r in rr)],
                accidental_space=sum(r['accidental_space'] for r in rr),substitutions=sum(r['letter_substitutions'] for r in rr),controls=sum(r['controls'] for r in rr),misses=sum(r['misses'] for r in rr)))
    browser=dict(designs=designs,summary=summary)
    (OUT/'preview-data.js').write_text('window.landscapeStudy='+json.dumps(browser,ensure_ascii=False)+';\n',encoding='utf-8')
    (OUT/'summary.json').write_text(json.dumps(browser,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('PASS disjoint geometry and center identity for all 12 designs; results are simulation, not human accuracy.')

if __name__=='__main__':main()
