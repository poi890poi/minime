"""Rebuild the predeclared Taiwanese/Japanese trial from completed guarded sessions."""
from pathlib import Path
import csv,gzip,hashlib,json,re,sys,zipfile
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
import report_touch_latency as timing

OUT=ROOT/'docs/release-hardening/language-timing'
ART=ROOT/'artifacts/release-hardening'
RUNS=[('language-timing-tw-smoke-a','tw-smoke-a','taiwanese_english','existence-trial.apk'),
      ('lt-tw-b1','tw-b1','taiwanese_english','candidate-pages-trial.apk'),
      ('lt-jp-a1','jp-a1','japanese_english','existence-trial.apk'),
      ('lt-jp-b1','jp-b1','japanese_english','candidate-pages-trial.apk'),
      ('lt-tw-a2','tw-a2','taiwanese_english','existence-trial.apk')]
def sha(data):return hashlib.sha256(data).hexdigest()

def main():
    corpus_path=OUT/'inputs.tsv'
    with corpus_path.open(encoding='utf-8') as f:corpus=list(csv.DictReader(f,delimiter='\t'))
    results={}
    for tag,name,mode,app in RUNS:
        log=(ART/(tag+'.txt')).read_text(encoding='utf-8-sig')
        assert 'OK (1 test)' in log and 'Cleanup verified: preferences, previous IME, display OFF.' in log and 'Final display OFF verified after environment collection' in log,tag
        sid=re.search(r'Session evidence: .*device-tests[\\/]([a-f0-9-]+)',log).group(1)
        session=ROOT/'artifacts/device-tests'/sid
        with (session/'touch-latency.tsv').open(encoding='utf-8') as f:rows=list(csv.DictReader(f,delimiter='\t'))
        build='1ea175a accepted runtime' if app=='existence-trial.apk' else 'held candidate-pages APK'
        result=timing.report(rows,build+' / 393ee56 timing harness')
        result['workload_validation']=timing.validate_workload(rows,corpus,mode,0)
        target=OUT/name;target.mkdir(exist_ok=True)
        with (target/'samples.tsv.gz').open('wb') as f:
            with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as z:z.write((session/'touch-latency.tsv').read_bytes())
        (target/'candidate-work.tsv').write_bytes((session/'candidate-work.tsv').read_bytes())
        (target/'summary.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
        env=ART/(tag+'-environment')
        manifest=dict(session=sid,runtime=build,harness='393ee56',sha256={
            'app':sha((ART/app).read_bytes()),'instrumentation':sha((ART/'language-timing-tests.apk').read_bytes()),'corpus':sha(corpus_path.read_bytes())},environment={})
        for phase in ['before','after']:
            manifest['environment'][phase]=dict(utc=(env/(phase+'-time.txt')).read_text(encoding='utf-8-sig').strip(),
                thermal_status=int(re.search(r'Thermal Status: (\d+)',(env/(phase+'-thermal.txt')).read_text()).group(1)),
                battery_c=int(re.search(r'temperature: (\d+)',(env/(phase+'-battery.txt')).read_text()).group(1))/10,
                low_power=(env/(phase+'-low-power.txt')).read_text().strip(),cpu0_khz=(env/(phase+'-cpu0-khz.txt')).read_text().strip())
        assert manifest['environment']['before']['thermal_status']==0 and manifest['environment']['before']['battery_c']<34
        manifest['restoration']='Original APK/preferences/IME restored; final display OFF verified.'
        (target/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
        results[name]={'environment':manifest['environment'],'groups':result['groups'],'actions':len(rows)}
        for key,cell in result['groups'].items():
            def p95(metric):return round(cell[metric].get('p95_ms',float('nan')),2)
            print(name,key,'candidate/raw/Space p95',p95('candidate_submission'),p95('editor_submission'),p95('space_submission'),
                  'observed',cell['candidate_submission']['observed'],'/',cell['candidate_submission']['n'],'timely',cell['candidate_deadline']['submitted_before_next_up'])
    with zipfile.ZipFile(ART/'existence-trial.apk') as a,zipfile.ZipFile(ART/'candidate-pages-trial.apk') as b:
        names=set(a.namelist())|set(b.namelist())
        differences=[n for n in sorted(names) if not n.startswith('META-INF/') and (n not in a.namelist() or n not in b.namelist() or a.read(n)!=b.read(n))]
    assert differences==['classes.dex','classes3.dex'],differences
    with zipfile.ZipFile(ART/'language-timing-tests.apk') as z:assert z.read('assets/language-timing-inputs.tsv')==corpus_path.read_bytes()
    summary=dict(runs=results,total_actions=sum(r['actions'] for r in results.values()),
                 non_signature_app_entry_differences=differences,corpus_sha256=sha(corpus_path.read_bytes()),
                 limitations=['Two modes, shard 0 only; not corpus completion or release acceptance.',
                              'Conditional candidate latencies must be read with missing/deadline counts.',
                              'Thermal snapshots do not establish constant CPU speed or temperature trajectories.'])
    (OUT/'comparison.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':main()
