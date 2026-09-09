"""Reproduce source query freeze and verify general extraction, not typing accuracy."""
from pathlib import Path
import csv, collections, hashlib, json, re, unicodedata

ROOT = Path(__file__).resolve().parent.parent

def source_rows():
    rows = list(csv.DictReader((ROOT/'third_party/taihoa/taihoa.csv').open(encoding='utf-8-sig')))
    valid = []; rejected = collections.Counter()
    for row in rows:
        for suffix in ('', 'Others'):
            keys = row['PojInput'+suffix].split('/')
            outputs = row['PojUnicode'+suffix].split('/')
            if len(keys) != len(outputs):
                rejected['unaligned variants'] += 1; continue
            for key, output in zip(keys, outputs):
                key = key.strip().lower(); output = unicodedata.normalize('NFC', output.strip())
                if not key: continue
                if not re.fullmatch('[a-z0-9 -]+', key) or not output or len(key)>96 or len(output)>96:
                    rejected['unsupported or exceeds composition limit'] += 1; continue
                valid.append((row['DictWordID'], key, output))
    return rows, valid, rejected

def main():
    records, valid, rejected = source_rows()
    queries = sorted(set((identity, re.sub('[- 1-9]', '', key), output) for identity,key,output in valid))
    payload = ''.join('\t'.join((identity,'full',key,output))+'\n' for identity,key,output in queries)
    for identity,key,output in sorted(queries,key=lambda q:hashlib.sha256('\t'.join(q).encode()).digest())[:2048]:
        if len(key)>1: payload += '\t'.join((identity,'prefix',key[:max(1,len(key)//2)],output))+'\n'
    assert (ROOT/'docs/two-language-modes/taihoa-inputs.tsv').read_bytes() == payload.encode(), 'Frozen queries drifted'
    expected = set()
    for identity,key,output in valid:
        for alias in (key,re.sub('[1-9]','',key)):
            expected.add(('poj',alias,output,'taihoa:'+identity,'extended_vocabulary'))
    shipped = {tuple(line.split('\t')) for line in (ROOT/'app/src/main/assets/addons.tsv').read_text(encoding='utf8').splitlines() if not line.startswith('#')}
    actual = {row for row in shipped if row[3].startswith('taihoa:')}
    assert actual == expected, 'Taihoa extraction differs from all supported source headwords'
    print('PASS',len(records),'source records;',len(valid),'eligible pairs;',len(actual),'attributed rows;',dict(rejected))

if __name__ == '__main__': main()
