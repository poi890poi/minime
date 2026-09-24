"""Structural audit only; official archive, no prediction queries or production writes."""
import collections,hashlib,json,re,tarfile,unicodedata
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'artifacts/source-audit/personachat'
OUT=Path(__file__).resolve().parent
PIN='507cf8641d333240654798870ea584d854ab5261071c5e3521c20d8fa41d5622'

def main():
    path=CACHE/'personachat.tgz'
    assert hashlib.sha256(path.read_bytes()).hexdigest()==PIN
    results={};signatures={};inventory=[]
    with tarfile.open(path) as archive:
        for member in archive:
            inventory.append(dict(path=member.name,bytes=member.size))
        for split in ('train','valid','test'):
            name=f'personachat/{split}_none_original.txt'
            member=archive.getmember(name);assert member.isfile()
            raw=archive.extractfile(member).read();text=raw.decode('utf-8',errors='strict')
            dialogs=[];current=[];previous=0;stats=collections.Counter();punctuation=collections.Counter()
            for line in text.splitlines():
                columns=line.split('\t');assert len(columns)==4 and columns[2]==''
                index,first=columns[0].split(' ',1);index=int(index)
                if index==1:
                    if current:dialogs.append(current)
                    current=[];previous=0
                assert index==previous+1;previous=index
                second=columns[1];assert first.strip() and second.strip()
                current.extend([first,second]);stats['turn_pairs']+=1
                stats['distractor_fields_ignored']+=bool(columns[3])
                for value in (first,second):
                    stats['utterances']+=1
                    punctuation.update(c for c in value if unicodedata.category(c).startswith('P'))
                    stats['utterances_with_spaced_apostrophe']+=bool(re.search(r"\w\s+'\s+\w",value))
                    stats['utterances_with_internal_apostrophe']+=bool(re.search(r"\w'\w",value))
            if current:dialogs.append(current)
            signatures[split]=collections.Counter(hashlib.sha256(json.dumps([' '.join(v.lower().split()) for v in d]).encode()).hexdigest() for d in dialogs)
            results[split]=dict(file=name,sha256=hashlib.sha256(raw).hexdigest(),conversations=len(dialogs),distinct_dialogues=len(signatures[split]),punctuation=dict(sorted(punctuation.items())),**stats)
    overlap={a+'/'+b:len(set(signatures[a])&set(signatures[b])) for a,b in [('train','valid'),('train','test'),('valid','test')]}
    result=dict(archive_sha256=PIN,repository_revision=(CACHE/'revision.txt').read_text(encoding='utf-8-sig').strip(),inventory=inventory,splits=results,whole_dialogue_overlap=overlap,
        scope='Original no-persona variant, only two utterance columns; distractors, other perspectives and revised files excluded structurally. No language model built or queried.',
        exposure='Three first training lines inspected for schema. Full structural audit only; split predictions not evaluated.',
        limitations='Persona-conditioned role-play, tokenized orthography, shared short utterances/personas possible. ID disjointness is not semantic independence.')
    (OUT/'personachat-manifest.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(splits=results,whole_dialogue_overlap=overlap),indent=2))

if __name__=='__main__':main()
