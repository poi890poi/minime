"""Native-origin diagnostic, not a language-accuracy or fresh holdout benchmark.

Freeze hash-selected existing Rudy and conversation inputs before querying Rime.
Absence from the exported lexical inventory is an attribution signal, not proof
that a sentence is meaningless. No production entries/weights are changed.
"""
import hashlib,json,subprocess,zipfile,re
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'docs/native-sentence-origin';OUT.mkdir(exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
geo=ROOT/'app/src/main/assets/geography.tsv'
corpus=ROOT/'docs/conversation-ranking/corpus/inputs.tsv'
vocabulary=set()
for row in (ROOT/'app/src/main/assets/zh_tw.tsv').read_text(encoding='utf-8').splitlines():
    fields=row.split('\t')
    if len(fields)>2:vocabulary.add(fields[2])
archives=sorted((ROOT/'third_party/rime/data-sources').glob('*.zip'))
for archive in archives:
    with zipfile.ZipFile(archive) as z:
        for name in z.namelist():
            if name.endswith(('.dict.yaml','/essay.txt')):
                for row in z.read(name).decode('utf-8-sig').splitlines():
                    fields=row.split('\t')
                    if len(fields)>1 and re.fullmatch(r'[\u3400-\u9fff]+',fields[0]):vocabulary.add(fields[0])
names={}
for row in geo.read_text(encoding='utf-8').splitlines():
    p=row.split('\t')
    if len(p)<3:continue
    syllables=p[1].split("'")
    if 3<=len(syllables)<=7 and all(re.fullmatch('[a-z]+',s) for s in syllables):
        names.setdefault(p[2],p[1])
pick=lambda items:sorted(items,key=lambda x:hashlib.sha256(x.encode('utf-8')).digest())[:256]
inputs=[]
for word in pick(names):
    syllables=names[word].split("'")
    inputs.extend([dict(group='rudy-full',raw=''.join(syllables),reference=word),dict(group='rudy-prefix',raw=''.join(syllables[:-1]),reference=word)])
rows=[r for r in corpus.read_text(encoding='utf-8').splitlines() if r and not r.startswith('en-')]
for row in pick(rows):
    p=row.split('\t');inputs.append(dict(group='existing-zh-audit',raw=p[2],reference=p[3]))
diagnostics=[dict(group='reported',raw=r,reference='加年端社') for r in ['jianianduan','jianianduanshe']]
inputs+=diagnostics
frozen={'scope':'origin and lexical-attestation diagnostic; existing production/evaluated data, not a fresh holdout',
        'selection':'first 256 names/rows by SHA-256 ordering; Rudy names 3–7 explicit syllables; full and omit-last-syllable inputs',
        'sources':{str(p.relative_to(ROOT)):sha(p) for p in [geo,corpus,*archives]},'inputs':inputs}
(OUT/'inputs.json').write_text(json.dumps(frozen,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
user=ROOT/'artifacts/native-sentence-origin-user';user.mkdir(exist_ok=True)
p=subprocess.Popen([str(ROOT/'artifacts/desktop-rime.exe'),str(ROOT/'.tools/rime-evaluation/msvc/dist/lib/rime.dll'),str(ROOT/'app/src/main/rimeAssets/rime'),str(user),'--audit-all'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,encoding='utf-8')
assert p.stdout.readline().strip()=='READY 1.16.1'
results=[]
try:
    for item in inputs:
        p.stdin.write(item['raw']+'\n');p.stdin.flush();choices=[]
        while True:
            line=p.stdout.readline()
            if not line:raise RuntimeError('Rime exited')
            if line.strip()=='END':break
            end,text=line.rstrip('\n').split('\t',1);choices.append(dict(end=int(end),text=text,lexically_attested=text in vocabulary))
        results.append(dict(**item,choices=choices[:8]))
finally:
    p.stdin.close();assert p.wait(timeout=15)==0
groups={}
for group in dict.fromkeys(r['group'] for r in results):
    part=[r for r in results if r['group']==group]
    groups[group]={'inputs':len(part),'no_native_choices':sum(not r['choices'] for r in part),
        'first_not_in_lexical_inventory':sum(bool(r['choices']) and not r['choices'][0]['lexically_attested'] for r in part),
        'first_equals_reference':sum(bool(r['choices']) and r['choices'][0]['text']==r['reference'] for r in part)}
report={'revision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'rime':'1.16.1',
        'inputs_sha256':sha(OUT/'inputs.json'),'lexical_inventory_size':len(vocabulary),'groups':groups,
        'limitations':['native provider only; excludes MinIME addon merge/ranking','lexical absence is not semantic invalidity','contains existing production/evaluated data; no accuracy claim'], 'results':results}
(OUT/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(groups,indent=2))
