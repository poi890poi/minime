"""Offline, isolated source pilot. Never writes app assets or selects word exceptions."""
import collections,csv,gzip,hashlib,json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
HERE=Path(__file__).resolve().parent
CACHE=ROOT/'artifacts/source-audit/taskmaster'
OUT=ROOT/'artifacts/source-audit/taskmaster-pilot'
TOKEN=re.compile(r"[a-z]+(?:'[a-z]+)*|[.!?]")

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def signature(dialog):
    value=[(t['speaker'],' '.join(t['text'].lower().split())) for t in dialog['utterances']]
    return hashlib.sha256(json.dumps(value,ensure_ascii=False).encode()).hexdigest()
def sequences(text):
    before=[]
    for word in TOKEN.findall(text.lower().replace('’',"'")):
        if word in '.!?':before=[];continue
        yield before,word
        before=(before+[word])[-2:]
def write_json(path,obj):path.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def prepare():
    OUT.mkdir(parents=True,exist_ok=True)
    audit=json.loads((HERE/'manifest.json').read_text(encoding='utf-8'))
    for name,digest in audit['sha256'].items():
        path=HERE/'UPSTREAM-README.md' if name=='README.md' else CACHE/Path(name).name
        assert sha(path)==digest, f'Pinned source changed: {name}'
    data=json.loads((CACHE/'self-dialogs.json').read_text(encoding='utf-8'))
    by_id={d['conversation_id']:d for d in data}
    splits={s:sorted(row[0] for row in csv.reader((CACHE/f'{s}.csv').open(encoding='utf-8')) if row and row[0]) for s in ('train','dev','test')}
    held={signature(by_id[i]) for s in ('dev','test') for i in splits[s]}
    seen=set();kept=[];excluded=[]
    for identity in splits['train']:
        sig=signature(by_id[identity])
        reason='evaluation-dialogue-overlap' if sig in held else 'duplicate-training-dialogue' if sig in seen else None
        if reason:excluded.append(dict(id=identity,reason=reason,signature=sig));continue
        seen.add(sig);kept.append(identity)
    assert not seen & held
    counts=collections.Counter();training_utterances=0
    for identity in kept:
        for turn in by_id[identity]['utterances']:
            training_utterances+=1
            for before,word in sequences(turn['text']):
                counts['',word]+=1
                for n in range(1,len(before)+1):counts[' '.join(before[-n:]),word]+=1
    with (OUT/'context.tsv').open('w',encoding='utf-8',newline='\n') as f:
        for (context,word),count in sorted(counts.items()):
            if not context or count>=2:f.write(f'en\t{context}\t{word}\t{count}\n')
    # Freeze complete inputs before evaluating any model outputs.
    records=[];inputs={};contexts=set();documents=collections.defaultdict(set)
    def add(group,doc,identity,text):
        documents[group].add(doc)
        for index,(before,word) in enumerate(sequences(text)):
            if not before:continue
            context=' '.join(before);contexts.add(context)
            records.append([group,doc,identity+':'+str(index),context,word])
    for split in ('dev','test'):
        for identity in splits[split]:
            dialog=by_id[identity]
            for turn in dialog['utterances']:
                add('taskmaster/'+split+'/'+dialog['instruction_id'],identity,str(turn['index']),turn['text'])
    for split in ('train','dev'):
        path=ROOT/f'artifacts/mode-corpus/en_gum-ud-{split}.conllu';inputs[path.relative_to(ROOT).as_posix()]=sha(path)
        for block in path.read_text(encoding='utf-8').split('\n\n'):
            identity=re.search(r'^# sent_id = (.+)$',block,re.M);text=re.search(r'^# text = (.+)$',block,re.M)
            if not identity or not text:continue
            identity=identity[1];doc=identity.rsplit('-',1)[0];genre=doc.split('_')[1]
            if genre in ('conversation','essay'):add('gum/'+genre,doc,identity,text[1])
    with gzip.open(OUT/'inputs.tsv.gz','wt',encoding='utf-8',newline='') as f:
        writer=csv.writer(f,delimiter='\t',lineterminator='\n');writer.writerow(['group','document','identity','context','target']);writer.writerows(records)
    (OUT/'contexts.txt').write_text('\n'.join(sorted(contexts))+'\n',encoding='utf-8')
    inputs['app/src/main/assets/context.tsv']=sha(ROOT/'app/src/main/assets/context.tsv')
    manifest=dict(source_manifest_sha256=sha(HERE/'manifest.json'),training_conversations=len(kept),training_utterances=training_utterances,excluded=excluded,
        model_sha256=sha(OUT/'context.tsv'),model_bytes=(OUT/'context.tsv').stat().st_size,model_rows=sum(1 for _ in (OUT/'context.tsv').open()),
        inputs_sha256=sha(OUT/'inputs.tsv.gz'),contexts_sha256=sha(OUT/'contexts.txt'),evaluation_tokens=len(records),unique_contexts=len(contexts),
        source_pins=inputs,groups={g:dict(documents=len(ds),tokens=sum(r[0]==g for r in records)) for g,ds in sorted(documents.items())},
        production_changed=False,exposure='Frozen before prediction queries. Taskmaster first-use split evaluation; GUM is previously evaluated source material.')
    write_json(OUT/'manifest.json',manifest)
    print(json.dumps({k:manifest[k] for k in ['training_conversations','training_utterances','excluded','model_rows','model_bytes','evaluation_tokens','unique_contexts']},indent=2))

def summarize():
    manifest=json.loads((OUT/'manifest.json').read_text(encoding='utf-8'))
    assert sha(OUT/'inputs.tsv.gz')==manifest['inputs_sha256'] and sha(OUT/'context.tsv')==manifest['model_sha256']
    assert sha(ROOT/'app/src/main/assets/context.tsv')==manifest['source_pins']['app/src/main/assets/context.tsv']
    predictions={}
    with (OUT/'predictions.tsv').open(encoding='utf-8') as f:
        for row in csv.reader(f,delimiter='\t'):predictions[row[0],row[1]]=row[2:]
    stats=collections.defaultdict(collections.Counter)
    with gzip.open(OUT/'inputs.tsv.gz','rt',encoding='utf-8') as f:
        for row in csv.DictReader(f,delimiter='\t'):
            group=row['group'];groups=[group]
            if group.startswith('taskmaster/'):groups.append('/'.join(group.split('/')[:2]))
            for model in ('baseline','pilot','empty'):
                candidates=[] if model=='empty' else predictions[model,row['context']]
                for group in groups:
                    s=stats[group,model];s['tokens']+=1;s['nonempty']+=bool(candidates)
                    for k in (1,3,8):
                        s[f'hits{k}']+=row['target'] in candidates[:k];s[f'slots{k}']+=len(candidates[:k])
    rows=[]
    for (group,model),s in sorted(stats.items()):
        item=dict(group=group,model=model,**s)
        for k in (1,3,8):item[f'top{k}_percent']=round(100*s[f'hits{k}']/s['tokens'],4)
        rows.append(item)
    reject=any(stats[g,'pilot'][f'hits{k}']<stats[g,'baseline'][f'hits{k}'] for g in ('gum/conversation','gum/essay') for k in (3,8))
    result=dict(decision='reject-as-sole-replacement' if reject else 'continue-full-pipeline-evaluation',metrics=rows,predictions_sha256=sha(OUT/'predictions.tsv'),
                limitations='Next-reference-token recall only, not semantic precision or full typing pipeline. Non-reference slots are not evidence of nonsense. GUM is reused; prompted Taskmaster shares instruction domains across splits.')
    write_json(OUT/'summary.json',result)
    print(result['decision'])
    for r in rows:
        if r['group'].count('/')==1 and r['model']!='empty':print(r['group'],r['model'],r['tokens'],*[r[f'top{k}_percent'] for k in (1,3,8)])

if __name__=='__main__':
    {'prepare':prepare,'summarize':summarize}[sys.argv[1]]()
