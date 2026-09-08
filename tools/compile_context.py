"""Build compact context counts from pinned UD training splits only.

Dev/test text never enters exported assets. Derived counts are CC BY-SA 4.0;
upstream corpus notices describe the underlying texts separately.
"""
import gzip,json,hashlib,re,collections
from pathlib import Path
from sources import require_sources
require_sources('ud-context')
ROOT=Path(__file__).resolve().parent.parent
src=ROOT/'third_party/ud'
pins={r['path'].replace('\\','/'):r['sha256'] for r in json.loads((src/'sources.json').read_text(encoding='utf8'))}
counts=collections.Counter(); totals={}
for repo,prefix,lang in [('UD_English-EWT','en_ewt','en'),('UD_Chinese-GSD','zh_gsd','zh')]:
    path=src/repo/(prefix+'-ud-train.conllu.gz')
    raw=gzip.decompress(path.read_bytes())
    if hashlib.sha256(raw).hexdigest()!=pins[path.relative_to(src).as_posix()]:
        raise ValueError('UD training input differs from pinned original: '+str(path))
    text=raw.decode('utf-8')
    sentences=[x[9:] for x in text.splitlines() if x.startswith('# text = ')]
    totals[lang]=len(sentences)
    for sentence in sentences:
        tokens=re.findall(r"[a-z]+(?:'[a-z]+)*|[.!?]",sentence.lower().replace('’',"'")) if lang=='en' else re.findall(r'[\u3400-\u9fff]|[。！？]',sentence)
        previous=[]
        for word in tokens:
            if word in '.!?。！？':previous=[];continue
            counts[lang,'',word]+=1
            for n in range(1,min(2,len(previous))+1):counts[lang,(' '.join(previous[-n:]) if lang=='en' else ''.join(previous[-n:])),word]+=1
            previous.append(word)
            previous=previous[-2:]
out=ROOT/'app/src/main/assets/context.tsv'
with out.open('w',encoding='utf-8',newline='\n') as f:
    for (lang,context,word),count in sorted(counts.items()):
        if not context or count>=2:f.write('{}\t{}\t{}\t{}\n'.format(lang,context,word,count))
report=dict(training_sentences=totals,rows=sum(1 for _ in out.open(encoding='utf-8')),asset_sha256=hashlib.sha256(out.read_bytes()).hexdigest(),scope='UD r2.15 training splits only; no development, test or paired evaluation labels')
(ROOT/'docs/context-report.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report))
