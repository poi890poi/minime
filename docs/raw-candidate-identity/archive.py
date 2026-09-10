import pathlib,re,collections,hashlib,json,gzip,io,csv
root=pathlib.Path('app/src/main/assets'); out=pathlib.Path('docs/raw-candidate-identity'); result={}
for name in ['addons.tsv','geography.tsv','zh_tw.tsv','japanese-basic.tsv','en_us.tsv']:
 path=root/name; rows=[line.split('\t') for line in path.read_text(encoding='utf-8').splitlines() if line and not line.startswith('#')]
 groups=collections.defaultdict(list)
 for p in rows:
  if name in ['addons.tsv','geography.tsv']: groups[p[0]].append((p[1],p[2]))
  elif name=='zh_tw.tsv': groups['chinese'].append((p[0],p[2]));groups['zhuyin'].append((p[1],p[2]))
  elif name=='japanese-basic.tsv':groups['japanese-character'].append((p[1],p[2]))
  else:groups['english-literal'].append((p[0],p[0]))
 result[name]={'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'groups':{pack:{'rows':len(pairs),'distinct_raw_equal_outputs':len({v for k,v in pairs if re.sub("[- ']",'',k.lower())==v})} for pack,pairs in groups.items()}}
(out/'language-inventory.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
for src,dest in [('before-results.tsv','before-results.tsv.gz'),('results.tsv','after-results.tsv.gz'),('inputs.tsv','inputs.tsv.gz')]:
 with (out/dest).open('wb') as f:
  with gzip.GzipFile(fileobj=f,mode='wb',mtime=0) as z:z.write((pathlib.Path('artifacts/poj-unmarked')/src).read_bytes())
for name in ['inventory.json','summary.json','core-red.txt','desktop-log.txt']:
 (out/name).write_bytes((pathlib.Path('artifacts/poj-unmarked')/name).read_bytes())
print(json.dumps(result,indent=2))
