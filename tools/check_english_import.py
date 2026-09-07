"""Source-integrity gate, not a language-quality score."""
import gzip,json,re
from pathlib import Path
root=Path(__file__).resolve().parent.parent
actual={p[0]:int(p[1]) for p in (l.split('\t') for l in (root/'app/src/main/assets/en_us.tsv').read_text(encoding='utf-8').splitlines())}
expected={}
for line in gzip.decompress((root/'third_party/aosp/en_US_wordlist.combined.gz').read_bytes()).decode('utf-8').splitlines():
    m=re.match(r' word=([^,]+),f=(\d+)',line)
    if m and int(m[2])>=70 and re.fullmatch("[A-Za-z]+(?:'[A-Za-z]+)*",m[1]):expected[m[1]]=int(m[2])
missing=[w for w in expected if w not in actual];changed=[w for w in expected if w in actual and actual[w]!=expected[w]]
print(json.dumps(dict(source_eligible=len(expected),imported=len(actual),missing=len(missing),frequency_mismatches=len(changed),first_missing=missing[:12])))
assert not missing and not changed and set(actual)==set(expected)
