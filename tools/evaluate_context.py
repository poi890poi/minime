"""Independent next-token holdout evaluation; never modifies model assets."""
import gzip,json,re,collections
from pathlib import Path
root=Path(__file__).resolve().parent.parent
model=collections.defaultdict(collections.Counter)
for line in (root/'app/src/main/assets/context.tsv').read_text(encoding='utf-8').splitlines():
    lang,context,word,n=line.split('\t');model[lang,context][word]=int(n)
results=[]
for repo,prefix,lang in [('UD_English-EWT','en_ewt','en'),('UD_Chinese-GSD','zh_gsd','zh')]:
    baseline=[w for w,n in model[lang,''].most_common(3)]
    for split in ['dev','test']:
        text=gzip.decompress((root/'third_party/ud'/repo/(prefix+'-ud-'+split+'.conllu.gz')).read_bytes()).decode()
        total=covered=hits=base=0
        for line in text.splitlines():
            if not line.startswith('# text = '):continue
            tokens=re.findall(r"[a-z]+(?:'[a-z]+)*|[.!?]",line[9:].lower().replace('’',"'")) if lang=='en' else re.findall(r'[\u3400-\u9fff]|[。！？]',line[9:])
            previous=[]
            for token in tokens:
                if token in '.!?。！？':previous=[];continue
                if previous:
                    context=(' ' if lang=='en' else '').join(previous[-2:]);scores=model.get((lang,context),model.get((lang,previous[-1]),{}))
                    top=[w for w,n in sorted(scores.items(),key=lambda x:(-x[1],x[0]))[:3]]
                    total+=1;covered+=bool(top);hits+=token in top;base+=token in baseline
                previous=(previous+[token])[-2:]
        results.append(dict(language=lang,split=split,tokens=total,context_coverage=covered,context_top3=hits,unigram_top3=base))
(root/'docs/context-holdout.json').write_text(json.dumps(results,indent=2)+'\n');print(json.dumps(results))
