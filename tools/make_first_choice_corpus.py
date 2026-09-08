"""Freeze syllables, typing imprecision and source-hash-selected partial phrases."""
from pathlib import Path
import collections,hashlib,json
r=Path(__file__).resolve().parent.parent;o=r/'docs/taiwan-quality';o.mkdir(exist_ok=True)
cases=collections.defaultdict(set)
def add(key,group):
    if key and key.isascii() and key.isalpha():cases[key].add(group)
syllables=sorted({s.split('\t')[0] for s in (r/'app/src/main/assets/syllables.tsv').read_text(encoding='utf-8').splitlines()})
for s in syllables:
    add(s,'syllable')
    for i in range(1,len(s)):add(s[:i],'syllable-prefix')
    for i in range(len(s)):add(s[:i]+s[i+1:],'missing-letter')
    for i in range(len(s)-1):add(s[:i]+s[i+1]+s[i]+s[i+2:],'transposed-letters')
    for i,c in enumerate(s):
        board=next((row for row in ['qwertyuiop','asdfghjkl','zxcvbnm'] if c in row),None)
        if board:
            at=board.index(c)
            for neighbor in [at-1,at+1]:
                if 0<=neighbor<len(board):add(s[:i]+board[neighbor]+s[i+1:],'adjacent-key')
source=r/'app/src/main/assets/zh_tw.tsv'
entries=sorted({tuple(s.split('\t')[:3:2]) for s in source.read_text(encoding='utf-8').splitlines() if "'" in s.split('\t')[0]},key=lambda p:hashlib.sha256(('first-choice-064\t'+p[0]+'\t'+p[1]).encode()).digest())[:2000]
for reading,word in entries:
    parts=reading.split("'")
    for key,group in [(''.join(parts),'phrase-full'),(''.join(p[0] for p in parts),'phrase-initial'),(''.join(p if i%2 else p[0] for i,p in enumerate(parts)),'phrase-mixed')]:add(key,group)
    key=''.join(parts)
    for i in range(1,len(key)):add(key[:i],'phrase-prefix')
payload=''.join(k+'\t'+','.join(sorted(v))+'\n' for k,v in sorted(cases.items())).encode()
(o/'first-choice-inputs.tsv').write_bytes(payload)
(o/'first-choice-corpus.json').write_bytes((json.dumps(dict(inputs=len(cases),group_counts=dict(collections.Counter(g for groups in cases.values() for g in groups)),source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),input_sha256=hashlib.sha256(payload).hexdigest(),phrase_selection='First 2000 distinct reading/output pairs by SHA256(first-choice-064 + reading + output); no frequency reference or complaint examples',reference_role='Syllables and dictionary entries generate queries only. MOE/UD references judge output afterward; this is an audit, not independent sentence accuracy.'),indent=2)+'\n').encode())
print(len(cases),dict(collections.Counter(g for groups in cases.values() for g in groups)))
