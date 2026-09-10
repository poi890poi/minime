"""Verify Japanese output against native source fields, independently of romanization.

No evaluation labels, eligibility lists or generated-reading logic are used here.
An optional baseline proves that other rows and input aliases were preserved.
"""
import argparse, collections, hashlib, json, tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def rows(path):
    return {tuple(line.split('\t')) for line in path.read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--asset', type=Path, default=ROOT/'app/src/main/assets/addons.tsv')
    parser.add_argument('--baseline', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    current = rows(args.asset)
    japanese = {row for row in current if row[0] == 'japanese'}
    needed = {row[3] for row in japanese}
    native = set()
    source_hashes = {}
    for source, name in [('jmdict', 'jmdict-eng-common.json.tgz'), ('jmnedict', 'jmnedict.json.tgz')]:
        archive_path = ROOT/'third_party'/source/name
        source_hashes[str(archive_path.relative_to(ROOT))] = hashlib.sha256(archive_path.read_bytes()).hexdigest()
        with tarfile.open(archive_path) as archive:
            data = json.load(archive.extractfile(next(m for m in archive.getmembers() if m.name.endswith('.json'))))
        for item in data['words']:
            identity = source + ':' + item['id']
            if identity in needed:
                for field in ('kana', 'kanji'):
                    native.update((identity, spelling['text']) for spelling in item[field])
        del data
    unsupported = {row for row in japanese if (row[3], row[2]) not in native}
    assert not unsupported, 'Japanese outputs absent from source kana/kanji fields: ' + str(len(unsupported))
    report = dict(asset_sha256=hashlib.sha256(args.asset.read_bytes()).hexdigest(),
                  japanese_rows=len(japanese), japanese_outputs=len({r[2] for r in japanese}),
                  japanese_reading_keys=len({r[1] for r in japanese}),
                  outputs_absent_from_source_fields=0, source_hashes=source_hashes)
    if args.baseline:
        before = rows(args.baseline)
        removed, added = before-current, current-before
        assert not added, 'Unexpected new rows'
        assert all(r[0]=='japanese' for r in removed), 'Other language rows removed'
        assert all((r[3],r[2]) not in native for r in removed), 'Original source spelling removed'
        old_keys = {(r[0],r[1],r[3],r[4]) for r in before}
        new_keys = {(r[0],r[1],r[3],r[4]) for r in current}
        assert old_keys == new_keys, 'Reading/source/category coverage changed'
        report.update(baseline_sha256=hashlib.sha256(args.baseline.read_bytes()).hexdigest(),
                      baseline_rows=len(before), rows=len(current), removed_rows=len(removed),
                      removed_by_source=dict(collections.Counter(r[3].split(':')[0] for r in removed)),
                      added_rows=0, lost_reading_source_category_keys=0,
                      removed_native_source_spellings=0, other_language_rows_changed=0)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))

if __name__ == '__main__':
    main()
