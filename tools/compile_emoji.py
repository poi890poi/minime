"""Generate the offline palette from pinned official Unicode data (no network)."""
from pathlib import Path
import hashlib, json

root=Path(__file__).resolve().parent.parent
source=root/'third_party/unicode/emoji-test-12.0.txt'
digest=hashlib.sha256(source.read_bytes()).hexdigest()
assert digest=='93b5476b4d3b5381a57336c138bcabb1a53a4288d604249535b0a694a0030db7'
rows=[]
for line in source.read_text(encoding='utf-8').splitlines():
    if line.startswith('# group: '): group=line[len('# group: '):]
    elif line.startswith('# subgroup: '): subgroup=line[len('# subgroup: '):]
    elif '; fully-qualified' in line:
        codes,rest=line.split(';',1)
        text=''.join(chr(int(cp,16)) for cp in codes.split())
        name=rest.split('#',1)[1].strip().split(' ',1)[1]
        rows.append('\t'.join((group,subgroup,text,name)))
out=root/'app/src/main/assets/emoji.tsv'
with out.open('w',encoding='utf-8',newline='\n') as target: target.write('\n'.join(rows)+'\n')
report={'source':'https://www.unicode.org/Public/emoji/12.0/emoji-test.txt',
        'source_sha256':digest,'version':'12.0','entries':len(rows),
        'asset_sha256':hashlib.sha256(out.read_bytes()).hexdigest()}
(root/'docs/emoji-report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(f'Generated {len(rows)} complete emoji sequences')
