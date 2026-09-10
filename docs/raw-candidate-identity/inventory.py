import pathlib, re, unicodedata, hashlib, json
root=pathlib.Path('artifacts/poj-unmarked')
rows=[s.split('\t') for s in pathlib.Path('app/src/main/assets/addons.tsv').read_text(encoding='utf-8').splitlines() if s.startswith('poj\t')]
tones=set('\u0300\u0301\u0302\u0304\u030d\u0306\u030b\u030c')
outputs={p[2] for p in rows}
unmarked={s for s in outputs if not tones.intersection(unicodedata.normalize('NFD',s))}
collision={(re.sub("[- ']",'',p[1].lower()),p[2]) for p in rows if re.sub("[- ']",'',p[1].lower())==p[2] and re.fullmatch('[a-z]+',p[2])}
controls={(re.sub("[- ']",'',p[1].lower()),p[2]) for p in rows if p[2] not in unmarked and re.fullmatch('[a-z]{1,16}',re.sub("[- ']",'',p[1].lower()))}
control=sorted(controls,key=lambda p:hashlib.sha256(('\t'.join(p)).encode()).digest())[:len(collision)]
(root/'inputs.tsv').write_text(''.join(q+'\t'+o+'\t'+label+'\n' for group,label in [(sorted(collision),'raw_equal'),(control,'marked_control')] for q,o in group),encoding='utf-8')
summary=dict(poj_outputs=len(outputs),unmarked_outputs=len(unmarked),ascii_raw_collision=len(collision),marked_control=len(control),asset_sha256=hashlib.sha256(pathlib.Path('app/src/main/assets/addons.tsv').read_bytes()).hexdigest())
(root/'inventory.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
print(json.dumps(summary))
