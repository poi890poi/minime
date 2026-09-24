"""Preserve candidate-page experiment evidence; requires completed guarded sessions."""
from pathlib import Path
import csv,gzip,hashlib,json,math,re,shutil,statistics,subprocess,sys,zipfile
ROOT=Path(__file__).resolve().parents[2];OUT=Path(__file__).resolve().parent
ART=ROOT/'artifacts/release-hardening'
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def environment(tag,destination,session):
    root=ART/(tag+'-environment');record={'session':session}
    for phase in ['before','after']:
        thermal=(root/f'{phase}-thermal.txt').read_text(encoding='utf-8-sig')
        battery=(root/f'{phase}-battery.txt').read_text(encoding='utf-8-sig')
        def value(text,key):return re.search(r'^\s*'+re.escape(key)+r':\s*(.+)$',text,re.M)[1].strip()
        record[phase]=dict(utc=(root/f'{phase}-time.txt').read_text(encoding='utf-8-sig').strip(),
            thermal_status=int(value(thermal,'Thermal Status')),battery_temperature_c=int(value(battery,'temperature'))/10,
            battery_percent=int(value(battery,'level')),usb_powered=value(battery,'USB powered')=='true',
            low_power=(root/f'{phase}-low-power.txt').read_text(encoding='utf-8-sig').strip(),
            cpu0_frequency_khz=int((root/f'{phase}-cpu0-khz.txt').read_text(encoding='utf-8-sig').strip()))
    if (root/'cooldown.jsonl').exists():record['cooldown']=[json.loads(s) for s in (root/'cooldown.jsonl').read_text(encoding='utf-8-sig').splitlines()]
    record['scope']='Before/after snapshots, not a temperature/frequency trajectory or causal attribution.'
    record['cleanup']='Original APK/preferences/IME restored; display OFF verified after final environment collection.'
    (destination/'environment.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
def stats(values):
    x=sorted(values)
    return dict(n=len(x),mean=statistics.mean(x),median=statistics.median(x),p95=x[math.ceil(.95*len(x))-1],maximum=x[-1])

sessions={}
for suffix in ['a1','b1','b2','a2','cost-a','cost-b','b3','a3']:
    tag='candidate-pages-'+suffix;log=ART/(tag+'.txt')
    if not log.exists():continue
    status=log.read_text(encoding='utf-8-sig')
    if not re.search(r'OK \(\d+ tests?\)',status) or 'Final display OFF verified' not in status:continue
    session=re.search(r'Session evidence: .*?([0-9a-f-]{36})\s',status)[1];sessions[suffix]=session
    source=ROOT/'artifacts/device-tests'/session;destination=OUT/tag
    if suffix.startswith('cost'):
        destination.mkdir(exist_ok=True)
        file=source/'candidate-page-cost.tsv';rows=list(csv.DictReader(file.open(encoding='utf-8'),delimiter='\t'))
        assert len(rows)==92
        result={'samples':len(rows),'units':'milliseconds for costs; probe counts are string predicate calls, not time',
                'scope':'Detached measure/layout/bitmap draw; excludes providers, input injection and presented frames. Two passes share font cache. Same frozen 46 Chinese prefixes.', 'rounds':{}}
        for turn in ['0','1']:
            subset=[r for r in rows if r['round']==turn]
            result['rounds'][turn]={k:stats([int(r[k])/1e6 for r in subset]) for k in ['apply_ns','strip_ns','expand_ns','select_ns']}
            result['rounds'][turn]['apply_plus_strip_ms']=stats([(int(r['apply_ns'])+int(r['strip_ns']))/1e6 for r in subset])
            result['rounds'][turn]['probe_totals']={k:sum(int(r[k]) for r in subset) for k in ['apply_probes','strip_probes','expand_probes']}
        (destination/'summary.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
        with (destination/'samples.tsv.gz').open('wb') as f:
            with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as g:g.write(file.read_bytes())
    else:
        subprocess.run([sys.executable,str(ROOT/'tools/report_touch_latency.py'),'--input',str(source/'touch-latency.tsv'),'--output',str(destination),'--build','1ea175a' if suffix.startswith('a') else 'candidate-pages-trial'],check=True,stdout=subprocess.DEVNULL)
    shutil.copyfile(source/'instrumentation.txt',destination/'instrumentation.txt')
    environment(tag,destination,session)

if all(s in sessions for s in ['cost-a','cost-b']):
    rows=[]
    for suffix in ['cost-a','cost-b']:
        with gzip.open(OUT/('candidate-pages-'+suffix)/'samples.tsv.gz','rt',encoding='utf-8') as f:rows.append(list(csv.DictReader(f,delimiter='\t')))
    a,b=rows
    assert [(r['round'],r['query'],r['candidates'],r['sha256']) for r in a]==[(r['round'],r['query'],r['candidates'],r['sha256']) for r in b]
    print('All 92 ordered full-list/default identities match across A/B; deep selection assertions passed.')

with zipfile.ZipFile(ART/'existence-trial.apk') as a,zipfile.ZipFile(ART/'candidate-pages-trial.apk') as b:
    changed=[n for n in sorted(set(a.namelist())|set(b.namelist())) if not n.startswith('META-INF/') and (n not in a.namelist() or n not in b.namelist() or a.read(n)!=b.read(n))]
manifest_path=OUT/'candidate-pages-binaries.json'
previous=json.loads(manifest_path.read_text(encoding='utf-8')) if manifest_path.exists() else None
source_hashes=previous['runtime_source_sha256'] if previous else {n:digest(ROOT/n) for n in ['core/src/main/java/dev/minime/core/CandidatePage.java','core/src/main/java/dev/minime/core/CompositionEngine.java','app/src/main/java/dev/minime/ime/KeyboardView.java']}
manifest=dict(baseline_runtime='1ea175a',sessions=sessions,
    binary_sha256={n:digest(ART/n) for n in ['existence-trial.apk','candidate-pages-trial.apk','layout-tests.apk','candidate-pages-tests3.apk','candidate-pages-cost-tests.apk']},
    runtime_source_sha256=source_hashes,
    changed_nonsignature_apk_entries=changed,
    timing_fixture_sha256=digest(ROOT/'app/src/androidTest/assets/latency-inputs.tsv'))
manifest_path.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
print('Recorded completed sessions:',sessions)
