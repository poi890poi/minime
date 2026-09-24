"""Freeze new documents before extracting labels or querying grammar proposals."""
import collections,gzip,hashlib,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'artifacts/source-audit/grammar-holdout'


def documents(text):
    result={};current=None
    for line in text.splitlines():
        if line.startswith('# newdoc id = '):
            current=line.split(' = ',1)[1]
            if current in result:raise ValueError('Repeated document identifier')
            result[current]=[]
        elif line.startswith('# text = '):
            if current is None:raise ValueError('Text before document identity')
            result[current].append(line.split(' = ',1)[1])
    return {key:'\n'.join(value) for key,value in result.items()}


def windows(text):
    words=re.findall(r"\w+(?:'\w+)*",text.lower().replace('’',"'"))
    for i in range(len(words)-19):yield hashlib.blake2b('\0'.join(words[i:i+20]).encode('utf-8'),digest_size=16).digest()


def main():
    download=json.loads((OUT/'download.json').read_text())
    fresh_files=[]
    for split in ('train','dev','test'):
        name=f'en_gum-ud-{split}.conllu';item=download['files'][name]
        path=OUT/item['file'];raw=gzip.decompress(path.read_bytes())
        if hashlib.sha256(raw).hexdigest()!=item['sha256']:raise ValueError('New source hash mismatch')
        fresh_files.append((split,raw.decode('utf-8')))
    old_paths=[ROOT/'artifacts/mode-corpus/en_gum-ud-train.conllu',ROOT/'artifacts/mode-corpus/en_gum-ud-dev.conllu',ROOT/'third_party/ud/UD_English-GUM/en_gum-ud-test.conllu.gz']
    old_ids=set();known=set();old_receipts=[]
    for path in old_paths:
        raw=path.read_bytes();plain=gzip.decompress(raw) if path.suffix=='.gz' else raw
        docs=documents(plain.decode('utf-8'));old_ids.update(docs)
        for value in docs.values():known.update(windows(value))
        old_receipts.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(plain).hexdigest(),documents=len(docs)))
    for path in sorted((ROOT/'third_party/ud/UD_English-EWT').glob('*.conllu.gz')):
        raw=gzip.decompress(path.read_bytes());text='\n'.join(line.split(' = ',1)[1] for line in raw.decode('utf-8').splitlines() if line.startswith('# text = '))
        known.update(windows(text));old_receipts.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(raw).hexdigest()))
    for path in sorted((ROOT/'artifacts/source-audit/masc/publisher-texts').rglob('*.txt')):
        raw=path.read_bytes();known.update(windows(raw.decode('utf-8-sig')))
        old_receipts.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(raw).hexdigest()))
    ledger=[];genres=collections.Counter();seen_new=set()
    for split,text in fresh_files:
        for doc,body in documents(text).items():
            if doc in seen_new:raise ValueError('Document repeated across new source splits')
            seen_new.add(doc)
            reason='previous-document-id' if doc in old_ids else 'previous-20-token-overlap' if any(w in known for w in windows(body)) else 'new-document-holdout'
            match=re.match(r'GUM_([^_]+)_',doc);genre=match[1] if match else 'unspecified'
            if reason=='new-document-holdout':genres[genre]+=1
            ledger.append(dict(document=doc,publisher_split=split,genre=genre,status=reason,text_sha256=hashlib.sha256(body.encode()).hexdigest()))
    result=dict(revision=download['revision'],source_files=download['files'],old_inputs=old_receipts,old_document_ids=len(old_ids),overlap_window_hashes=len(known),ledger=ledger,status_counts=dict(collections.Counter(r['status'] for r in ledger)),eligible_genres=dict(genres),role='Frozen evaluation only, no label extraction or prediction inspected')
    (OUT/'document-freeze.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:result[k] for k in ('old_document_ids','overlap_window_hashes','status_counts','eligible_genres')}))


if __name__=='__main__':main()
