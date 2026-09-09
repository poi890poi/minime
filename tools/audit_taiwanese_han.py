"""Audit Han source coverage only; no evaluation text enters production selection."""
from pathlib import Path
from collections import defaultdict,Counter
import csv,json,re,subprocess,unicodedata,hashlib
ROOT=Path(__file__).resolve().parent.parent

def audit():
    def parse(text):return {p[1]:p for l in text.splitlines() for p in [l.split('\t')] if len(p)==4}
    old=parse(subprocess.check_output(['git','show','9a37682:app/src/main/assets/paired-forms.tsv'],cwd=str(ROOT)).decode('utf8'))
    new=parse((ROOT/'app/src/main/assets/paired-forms.tsv').read_text(encoding='utf8'))
    assert all(new[k]==v for k,v in old.items()),'Existing canonical forms changed'
    sourcepairs={};by_han=defaultdict(set)
    for dataset,path in [('itaigi','third_party/itaigi/itaigi.csv'),('taihoa','third_party/taihoa/taihoa.csv')]:
        with (ROOT/path).open(encoding='utf-8-sig') as stream:
            for r in csv.DictReader(stream):
                pair=(unicodedata.normalize('NFC',r['PojUnicode'].strip()),unicodedata.normalize('NFC',r['HanLoTaibunPoj'].strip()))
                sourcepairs[dataset+':'+r['DictWordID']]=pair;by_han[pair[1]].add(pair[0])
    assert all(sourcepairs[p[3]]==(p[1],p[2]) for p in new.values()),'Source identity mismatch'
    groups=defaultdict(set); inventory=[]
    for line in (ROOT/'app/src/main/assets/addons.tsv').read_text(encoding='utf8').splitlines():
        p=line.split('\t')
        if len(p)==5 and p[0]=='poj':groups[p[4]].add(p[2]);inventory.append(p)
    outputs=set().union(*groups.values())
    probes={}
    for p in inventory:
        if p[4]!='everyday_vocabulary' or re.search('[0-9]',p[1]):continue
        if p[2] not in probes or (len(p[1]),p[1])<(len(probes[p[2]][1]),probes[p[2]][1]):probes[p[2]]=p
    lines=[]
    for output,p in sorted(probes.items()):
        units=[v for v in re.split("[- ']+",p[1]) if v];full=''.join(units)
        for condition,raw in [('full',full),('prefix',full[:max(1,(len(full)+1)//2)]),('initials',''.join(u[0] for u in units))]:
            lines.append('\t'.join([p[3],condition,raw,output]))
    frozen=('\n'.join(lines)+'\n').encode('utf8')
    assert frozen==(ROOT/'docs/taiwanese-coverage/retrieval-inputs.tsv').read_bytes(),'Frozen retrieval inventory drift'
    report={'preserved_original_pairs':len(old),'new_pairs':len(new)-len(old),'category_availability':{g:{'outputs':len(v),'old_pair_record':len(v&set(old)),'new_pair_record':len(v&set(new))} for g,v in groups.items()}}
    report['retrieval_inputs']={'queries':len(lines),'outputs':len(probes),'sha256':hashlib.sha256(frozen).hexdigest(),'role':'source-derived regression, not independent accuracy'}
    handout=json.loads((ROOT/'docs/taiwanese-coverage/basic-headwords.json').read_text(encoding='utf8'))
    valid=[r for r in handout['records'] if re.fullmatch('[\u3400-\u9fff]+',r['han'])]
    oldhan={v[2] for v in old.values()};newhan={v[2] for v in new.values()};missing=[]
    for r in valid:
        if r['han'] in newhan:continue
        readings=by_han[r['han']]
        cause='no_exact_source_Han_field' if not readings else 'no_existing_complete_phonetic_output' if not readings&outputs else 'eligibility_or_other_canonical_spelling'
        missing.append({'id':r['id'],'han':r['han'],'stage':cause})
    report['moe_basic_han_inventory']={'numbered':504,'comparable':len(valid),'old_present':sum(r['han'] in oldhan for r in valid),'new_present':sum(r['han'] in newhan for r in valid),'unsupported':[r['id'] for r in handout['records'] if r not in valid],'missing_stages':dict(Counter(r['stage'] for r in missing)),'missing_after':missing}
    (ROOT/'docs/taiwanese-coverage/source-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps({k:v for k,v in report['moe_basic_han_inventory'].items() if k!='missing_after'},ensure_ascii=False,indent=2))

if __name__=='__main__':audit()
