"""Preserve guarded allocation/frame trials; never equate these with typing latency."""
from pathlib import Path
import argparse,csv,gzip,hashlib,io,json,math,re,statistics,zipfile
ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/'artifacts/release-hardening'
OUT=ROOT/'docs/release-hardening/expanded-viewport'

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def stats(values):
    x=sorted(values)
    return dict(n=len(x),mean_ms=statistics.mean(x),median_ms=statistics.median(x),
                p95_ms=x[math.ceil(.95*len(x))-1],p99_ms=x[math.ceil(.99*len(x))-1],maximum_ms=x[-1]) if x else dict(n=0)
def records(path):return list(csv.DictReader(io.StringIO(path.read_text(encoding='utf-8')),delimiter='\t'))
def preserve(path,target):
    with target.open('wb') as f:
        with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as z:z.write(path.read_bytes())
def write(path,value):path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('tags',nargs='+');args=parser.parse_args()
    OUT.mkdir(exist_ok=True)
    reference=records(ROOT/'artifacts/device-tests/91811cae-3279-4dca-85b9-771622c66770/candidate-page-cost.tsv')
    identity={(r['round'],r['query']):(r['candidates'],r['sha256']) for r in reference}
    expected=[r['query'] for r in reference if r['round']=='0'][::4]
    for tag in args.tags:
        assert re.fullmatch(r'expanded-((append|row|page)-)?(cost|frames)-[ab][12]',tag),tag
        log=(ART/(tag+'.txt')).read_text(encoding='utf-8-sig')
        assert re.search(r'OK \(\d+ tests?\)',log) and 'Cleanup verified: preferences, previous IME, display OFF.' in log and 'Final display OFF verified after environment collection' in log,tag
        sid=re.search(r'Session evidence: .*device-tests[\\/]([a-f0-9-]+)',log)[1]
        source=ROOT/'artifacts/device-tests'/sid;target=OUT/tag;target.mkdir(exist_ok=True)
        kind='cost' if '-cost-' in tag else 'frames'
        tests='expanded-viewport-tests3.apk' if kind=='cost' else 'expanded-frame-tests.apk'
        app='existence-trial.apk' if tag.rsplit('-',1)[1][0]=='a' else 'expanded-page-trial.apk' if '-page-' in tag else 'expanded-row-trial.apk' if '-row-' in tag else 'expanded-append-trial.apk' if '-append-' in tag else 'expanded-viewport-trial.apk'
        manifest=dict(session=sid,app=app,app_sha256=sha(ART/app),instrumentation=tests,instrumentation_sha256=sha(ART/tests),
                      runtime_boundary='Accepted 1ea175a versus expanded-grid allocation only; core/dictionary unchanged.',environment={})
        env=ART/(tag+'-environment')
        for phase in ['before','after']:
            manifest['environment'][phase]=dict(utc=(env/(phase+'-time.txt')).read_text(encoding='utf-8-sig').strip(),
                thermal_status=int(re.search(r'Thermal Status: (\d+)',(env/(phase+'-thermal.txt')).read_text())[1]),
                battery_c=int(re.search(r'temperature: (\d+)',(env/(phase+'-battery.txt')).read_text())[1])/10,
                cpu0_khz=(env/(phase+'-cpu0-khz.txt')).read_text(encoding='utf-8-sig').strip(),
                low_power=(env/(phase+'-low-power.txt')).read_text(encoding='utf-8-sig').strip())
        assert manifest['environment']['before']['thermal_status']==0 and manifest['environment']['before']['battery_c']<34
        manifest['cleanup']='Original APK/preferences/IME restored; display OFF verified after environment collection.'
        manifest['environment_scope']='Endpoint snapshots cannot establish a constant thermal/frequency trajectory.'
        write(target/'manifest.json',manifest)
        (target/'instrumentation.txt').write_bytes((source/'instrumentation.txt').read_bytes())
        summary={}
        if kind=='cost':
            file=source/'candidate-page-cost.tsv';rows=records(file)
            assert [(r['round'],r['query'],r['candidates'],r['sha256']) for r in rows]==[(r['round'],r['query'],r['candidates'],r['sha256']) for r in reference]
            preserve(file,target/'samples.tsv.gz')
            for turn in ['0','1']:
                summary[turn]={key:stats([int(r[key])/1e6 for r in rows if r['round']==turn]) for key in ['apply_ns','strip_ns','expand_ns','select_ns']}
            summary['scope']='Detached measure/layout/bitmap draw; selection is a core call, not a visible scrolling operation.'
        else:
            for orientation in ['portrait','landscape']:
                file=source/f'expanded-frames-{orientation}.tsv';rows=records(file)
                episodes=[];groups={};totals=[]
                for turn in ['0','1']:
                    for raw in expected:
                        cell=[r for r in rows if r['round']==turn and r['query']==raw]
                        assert cell and cell[0]['action']=='expand' and cell[-1]['action']=='select'
                        assert all(r['action']=='scroll' for r in cell[1:-1])
                        assert [int(r['step']) for r in cell]==[0]+list(range(1,len(cell)-1))+[len(cell)-2]
                        # Attached replay uses Learning.NONE for independent episodes.
                        # The detached replay learns from its deep selection, so its
                        # second pass deliberately has a different ordering reference.
                        assert all((r['candidates'],r['sha256'])==identity['0',raw] for r in cell),(tag,orientation,turn,raw)
                        assert all(0<int(r['work_ns'])<=int(r['draw_ns'])<=int(r['submit_ns']) for r in cell)
                        episodes.append((turn,raw));totals.append(dict(round=turn,query=raw,scrolls=len(cell)-2,
                            scroll_work_ms=sum(int(r['work_ns'])/1e6 for r in cell[1:-1]),
                            all_action_work_ms=sum(int(r['work_ns'])/1e6 for r in cell)))
                assert set(episodes)==set((r['round'],r['query']) for r in rows) and len(episodes)==24
                for action in ['expand','scroll','select','scroll_append','scroll_no_append']:
                    cell=[r for r in rows if r['action']==action or action.startswith('scroll_') and r['action']=='scroll' and (int(r['after_views'])>int(r['before_views']))==(action=='scroll_append')]
                    groups[action]={key:stats([int(r[key])/1e6 for r in cell]) for key in ['work_ns','draw_ns','submit_ns']}
                preserve(file,target/(orientation+'.tsv.gz'));summary[orientation]=dict(groups=groups,episodes=totals)
            summary['scope']='Sequential settled attached-view action to submitted GPU frame; not fling, physical contact, panel presentation, or language accuracy.'
        write(target/'summary.json',summary)
        print(tag,json.dumps(summary if kind=='cost' else {o:summary[o]['groups'] for o in ['portrait','landscape']},ensure_ascii=False))
    with zipfile.ZipFile(ART/'existence-trial.apk') as a,zipfile.ZipFile(ART/'expanded-viewport-trial.apk') as b:
        differences=[n for n in sorted(set(a.namelist())|set(b.namelist())) if not n.startswith('META-INF/') and (n not in a.namelist() or n not in b.namelist() or a.read(n)!=b.read(n))]
    assert differences==['classes.dex','classes3.dex'],differences
    write(OUT/'payload-differences.json',dict(non_signature_differing_entries=differences))

if __name__=='__main__':main()
