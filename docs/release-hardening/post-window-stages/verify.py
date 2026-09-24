"""Verify the diagnostic payload and unchanged production source boundary."""
import hashlib
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'artifacts/post-window-stages'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    manifest = json.loads((OUT / 'combined-source-manifest.json').read_text(encoding='utf-8'))
    for path, digest in manifest['active_java_sha256'].items():
        if sha(ROOT / path) != digest:
            raise ValueError('Active source changed: ' + path)
    baseline = ROOT / 'artifacts/single-window/trial.apk'
    with zipfile.ZipFile(OUT / 'diagnostic.apk') as app, zipfile.ZipFile(baseline) as old:
        assets = {n: app.read(n) for n in app.namelist() if n.startswith('assets/') and not n.endswith('/')}
        original = {n: old.read(n) for n in old.namelist() if n.startswith('assets/') and not n.endswith('/')}
        if assets != original or len(assets) != 24:
            raise ValueError('Language assets differ')
        # META-INF also contains non-signature build metadata. Exclude only the
        # known JAR signature records, retaining revision and other resources.
        signature_records = {'META-INF/MANIFEST.MF', 'META-INF/ANDROIDD.SF', 'META-INF/ANDROIDD.RSA'}
        names = (set(app.namelist()) | set(old.namelist())) - signature_records
        differences = sorted(n for n in names if n not in app.namelist() or n not in old.namelist() or app.read(n) != old.read(n))
        if differences != ['META-INF/version-control-info.textproto', 'classes.dex']:
            raise ValueError('Unexpected package delta: ' + repr(differences))
        dex = b''.join(app.read(n) for n in app.namelist() if n.endswith('.dex'))
        if b'Ldev/minime/ime/QueueTrace;' not in dex:
            raise ValueError('Missing diagnostic observer')
    with zipfile.ZipFile(OUT / 'tests.apk') as tests:
        corpus = tests.read('assets/language-timing-inputs.tsv')
        if corpus != (ROOT / 'docs/release-hardening/language-timing/inputs.tsv').read_bytes():
            raise ValueError('Timing inventory changed')
    android = (OUT / 'manifest.txt').read_text(encoding='utf-8-sig')
    if any(s in android for s in ['android:debuggable', 'android.permission.INTERNET', 'profileable']):
        raise ValueError('Unexpected diagnostic manifest capability')
    if 'EditorTestActivity' not in android:
        raise ValueError('Missing test-only editor')
    signature = (OUT / 'signature.txt').read_text(encoding='utf-8-sig')
    if 'edf08f77fe28853ffe8afe6bfbb2c1c83ba0b60c346c27c7d5650ff0b5edd4c7' not in signature:
        raise ValueError('Unexpected signing identity')
    result = dict(active_sources_unchanged=True, equal_language_assets=24,
        changed_non_signature_entries=differences, build_revision_metadata_changed=True, non_debuggable=True,
        no_internet=True, no_profileable=True, test_only_editor=True,
        corpus_sha256=hashlib.sha256(corpus).hexdigest(),
        baseline_sha256=sha(baseline), files={name: sha(OUT / name) for name in
            ['diagnostic.apk', 'tests.apk', 'decoder.patch', 'combined-observers.patch',
             'combined-source-manifest.json', 'diagnostic.init.gradle', 'manifest.txt', 'signature.txt']})
    (OUT / 'binaries.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
