"""Source attestation and exhaustive shipped-reading retrieval inventory, not accuracy."""
import collections, csv, gzip, hashlib, io, json, re, unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT/'docs/poj-tones'

def write_targets(path, records):
    buffer=io.BytesIO()
    with gzip.GzipFile(fileobj=buffer,mode='wb',mtime=0) as stream:
        stream.write(''.join('\t'.join(row)+'\n' for row in records).encode('utf-8'))
    path.write_bytes(buffer.getvalue())


def rows(path):
    return {tuple(line.split('\t')) for line in path.read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')}


def main():
    before = rows(ROOT/'artifacts/poj-tones/baseline.tsv')
    after = rows(ROOT/'app/src/main/assets/addons.tsv')
    added, removed = after-before, before-after
    assert not removed, 'Existing entries removed'
    assert all(row[0]=='poj' for row in added), 'Other languages changed'
    native = set()
    for source, path in [('itaigi','itaigi/itaigi.csv'), ('taihoa','taihoa/taihoa.csv'), ('taiwanese-basic','taiwanese_basic/vocabulary.csv')]:
        for item in csv.DictReader((ROOT/'third_party'/path).open(encoding='utf-8-sig')):
            for suffix in ('','Others'):
                native.update((source+':'+item['DictWordID'],unicodedata.normalize('NFC',out.strip()))
                              for out in item.get('PojUnicode'+suffix,'').split('/'))
            if source=='taiwanese-basic':
                native.update((source+':'+item['DictWordID']+':example',unicodedata.normalize('NFC',out.strip().rstrip('.!?')))
                              for out in item['LekuPoj'].split('/'))
    assert all((r[3],r[2]) in native for r in added), 'Unattested output generated'
    targets = sorted({(re.sub("[- ']+",'',r[1]),r[2],r[4]) for r in before|after if r[0]=='poj'})
    OUT.mkdir(parents=True,exist_ok=True)
    write_targets(OUT/'targets.tsv.gz',targets)
    old_words = {r[2] for r in before if r[0]=='poj'}
    new_words = {r[2] for r in after if r[0]=='poj'}-old_words
    # Freeze before observing ranks: all recovered outputs with tone-free full
    # input, plus a hash-selected source-wide control. The exhaustive 170k target
    # inventory is separate; querying every target was stopped as impractical.
    probes = [(key,word,category) for key,word,category in targets
              if not any(c.isdigit() for c in key) and
              (word in new_words or int(hashlib.sha256(key.encode()).hexdigest()[:8],16)%128==0)]
    write_targets(OUT/'probes.tsv.gz',probes)
    # Fixed 1/8 query sample for partial lookup, independent of result rank.
    partials=[(key[:max(1,len(key)//2)],word,category+'/half') for key,word,category in probes
              if len(key)>1 and int(hashlib.sha256(key.encode()).hexdigest()[:8],16)%8==0]
    write_targets(OUT/'partial-probes.tsv.gz',partials)
    report = dict(baseline_sha256=hashlib.sha256((ROOT/'artifacts/poj-tones/baseline.tsv').read_bytes()).hexdigest(),
                  asset_sha256=hashlib.sha256((ROOT/'app/src/main/assets/addons.tsv').read_bytes()).hexdigest(),
                  added_rows=len(added),removed_rows=len(removed),new_spellings=len(new_words),
                  added_by_source=dict(collections.Counter(r[3].split(':')[0] for r in added)),
                  other_language_changes=0,unattested_added_outputs=0,targets=len(targets),runtime_probes=len(probes),
                  exposure='seen-source exhaustive retrieval; not language accuracy',
                  examples=[list(r) for r in sorted(added) if r[2] in new_words and r[4]=='everyday_vocabulary'][:12])
    (OUT/'inventory.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))


if __name__=='__main__':
    main()
