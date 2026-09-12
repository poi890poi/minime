"""Compare shared Android serialization against frozen native research telemetry."""
from pathlib import Path
import os,subprocess,gzip,json,time
root=Path(__file__).resolve().parent.parent
env=os.environ.copy();env['PATH']=str(root/'.tools/rime-evaluation/msvc/dist/lib')+os.pathsep+env['PATH']
exe=root/'artifacts/native-metadata/probe.exe'
subprocess.run([str(exe),'--self-test'],env=env,check=True)
user=root/'artifacts/native-metadata/transport-user';user.mkdir(exist_ok=True)
p=subprocess.Popen([str(exe),str(root/'.tools/rime-evaluation/msvc/dist/lib/rime.dll'),str(root/'app/src/main/rimeAssets/rime'),str(user)],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,encoding='utf-8',env=env)
assert p.stdout.readline().strip()=='READY 1.16.1'
for invalid in ('','ni hao','UPPER','a'*97,'ㄋㄧˇ'):
    p.stdin.write(invalid+'\n');p.stdin.flush();assert p.stdout.readline().strip()=='END'
n=0;records=0
try:
    for role in ('development','reserved'):
        rows=gzip.decompress((root/'artifacts/native-metadata'/f'{role}-replay.tsv.gz').read_bytes()).decode().splitlines();it=iter(rows)
        for line in it:
            raw=line.split('\t')[2];expected=[]
            for record in it:
                if record=='END':break
                expected.append(record)
            p.stdin.write(raw+'\n');p.stdin.flush();actual=[]
            while True:
                record=p.stdout.readline()
                if not record:raise RuntimeError('Transport process ended')
                if record.strip()=='END':break
                actual.append(record.rstrip('\r\n'))
            assert expected==actual,(role,n,raw,expected[:2],actual[:2])
            n+=1;records+=len(actual)
            if n%2000==0:print('Transport parity',n,flush=True)
finally:
    p.stdin.close();assert p.wait(timeout=15)==0
report=dict(inputs=n,candidate_records=records,result='exact endpoint, origin, text and order parity',scope='Shared C++ Android serializer + pinned desktop DLL; not device/JNI execution')
(root/'docs/construction-confidence/transport-parity.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(report)
