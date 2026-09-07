"""Fetch a pinned, fresh multi-genre test split for evaluation only."""
import gzip, hashlib, json, re, urllib.request, collections
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
REV='34d01cb603867d0c085896e8286a0fe01229fa37'
DEST=ROOT/'third_party/ud/UD_English-GUM';DEST.mkdir(exist_ok=True)
sources=[];data=None
for name in ['LICENSE.txt','README.md','en_gum-ud-test.conllu']:
    url='https://raw.githubusercontent.com/UniversalDependencies/UD_English-GUM/'+REV+'/'+name
    payload=urllib.request.urlopen(url,timeout=45).read();sources.append(dict(url=url,sha256=hashlib.sha256(payload).hexdigest()))
    if name.endswith('.conllu'):
        data=payload;(DEST/(name+'.gz')).write_bytes(gzip.compress(payload))
    else:(DEST/name).write_bytes(payload)
rows=[];genre='unknown';excluded=0
for block in data.decode('utf-8').strip().split('\n\n'):
    identity=next((l[12:] for l in block.splitlines() if l.startswith('# sent_id = ')),None)
    if not identity:continue
    parts=identity.split('_');genre=parts[1] if len(parts)>1 else 'unknown'
    text=next((l[9:] for l in block.splitlines() if l.startswith('# text = ')),None)
    assert text is not None
    words=re.findall("[a-z]+(?:'[a-z]+)*",text.lower().replace('’',"'"))
    if not words:excluded+=1;continue
    raw=' '.join(words);rows.append(('en-gum-'+genre,identity,raw,raw))
out=ROOT/'docs/conversation-ranking/corpus';payload=''.join('\t'.join(r)+'\n' for r in rows).encode('utf-8')
(out/'gum-test.tsv').write_bytes(payload)
(out/'gum-manifest.json').write_text(json.dumps(dict(revision=REV,sources=sources,rows=len(rows),excluded_no_alpha=excluded,genres=dict(collections.Counter(r[0] for r in rows)),sha256=hashlib.sha256(payload).hexdigest(),scope='Fresh holdout for capitalized-source vocabulary fix; no GUM data enters training'),indent=2)+'\n',encoding='utf-8')
print('Frozen fresh GUM sentences',len(rows),'SHA256',hashlib.sha256(payload).hexdigest())
