"""Freeze all reference-provided single-glyph readings; no model weights or labels in runtime input."""
import collections
import gzip
import hashlib
import json
from pathlib import Path
from audit_moe_readings import reference

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'artifacts/moe-reading-reference'


def main():
    glyphs, words, unresolved, stats, _ = reference('concise')
    refs = collections.defaultdict(set)
    skipped = 0
    for glyph in glyphs:
        if glyph in unresolved:
            skipped += 1
            continue
        for reading in words.get(glyph, set()):
            refs[reading].add(glyph)
    payload = 'genre\tid\tcondition\traw\ttarget\tglyphs\n'
    for i, raw in enumerate(sorted(refs)):
        payload += f'modern-reading\t{i}\tfull\t{raw}\t\t0\n'
    data = gzip.compress(payload.encode(), mtime=0)
    (OUT / 'inputs.tsv.gz').write_bytes(data)
    reference_data = (json.dumps({k: sorted(v) for k, v in sorted(refs.items())}, ensure_ascii=False, indent=2) + '\n').encode()
    (OUT / 'references.json').write_bytes(reference_data)
    manifest = dict(source_id='moe-dictionaries', role='pronunciation-reference diagnostic; previously inspected source, not fresh holdout',
                    queries=len(refs), reference_pairs=sum(map(len, refs.values())),
                    glyphs=len({g for values in refs.values() for g in values}), skipped_unresolved_glyphs=skipped,
                    inputs_sha256=hashlib.sha256(data).hexdigest(), references_sha256=hashlib.sha256(reference_data).hexdigest(),
                    source_inventory=stats)
    (OUT / 'probe-manifest.json').write_bytes((json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode())
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
