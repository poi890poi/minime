"""Freeze native Android-bounded records for shared-core A/B replay."""
from pathlib import Path
import json,gzip
root=Path(__file__).resolve().parent.parent
out=root/'docs/construction-confidence'
for role in ('development','reserved'):
    source=out/(role+'-native.jsonl.gz')
    if not source.exists():continue
    lines=[]
    for index,line in enumerate(gzip.decompress(source.read_bytes()).decode().splitlines()):
        row=json.loads(line);raw=row['raw']
        full=[c for c in row['choices'] if c['end']==len(raw)][:24]
        prefix=[c for c in row['choices'] if 0<c['end']<len(raw)][:12]
        choices=full[:3]+prefix[:3]+full[3:]+prefix[3:]
        lines.append(f"{row['condition']}\t{index}\t{raw}\t{row['target']}")
        for c in choices:lines.append(f"{c['end']}\t{'S' if c['kind']=='sentence' else 'L'}\t{c['text']}")
        lines.append('END')
    target=root/'artifacts/native-metadata'/(role+'-replay.tsv.gz')
    target.write_bytes(gzip.compress(('\n'.join(lines)+'\n').encode(),mtime=0))
    print(target)
