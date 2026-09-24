"""Reuse the checked queue overlay on the admitted single-window sources."""
import difflib
import hashlib
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'artifacts/post-window-stages'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    if OUT.exists():
        raise ValueError('Refusing to overwrite a diagnostic identity')
    producer = ROOT / 'docs/release-hardening/queue-stages/prepare.py'
    spec = importlib.util.spec_from_file_location('queue_overlay', producer)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    active = {str(p.relative_to(ROOT)): sha(p) for base in
              ['app/src/main/java', 'app/src/androidTest/java']
              for p in (ROOT / base).rglob('*.java')}
    OUT.mkdir(parents=True)
    module.OUT = OUT
    module.main()
    init = OUT / 'diagnostic.init.gradle'
    text = init.read_text(encoding='utf-8')
    if text.count('artifacts/queue-stages/overlay/') != 2:
        raise ValueError('Changed source-set anchors')
    init.write_text(text.replace('artifacts/queue-stages/overlay/',
                                'artifacts/post-window-stages/overlay/'), encoding='utf-8')
    test = OUT / 'overlay/test/dev/minime/ime/TouchLatencyTest.java'
    before = test.read_text(encoding='utf-8')
    after = module.replace_once(before, 'try {runReplay(false,mode,0);}',
                                'try {runReplay(true,mode,0);}')
    test.write_text(after, encoding='utf-8')
    (OUT / 'combined-observers.patch').write_text(''.join(difflib.unified_diff(
        before.splitlines(True), after.splitlines(True),
        fromfile='queue-only/TouchLatencyTest.java',
        tofile='combined/TouchLatencyTest.java')), encoding='utf-8')
    for path, digest in active.items():
        if sha(ROOT / path) != digest:
            raise ValueError('Active sources changed: ' + path)
    differences = []
    for base, overlay in [('app/src/main/java', 'main'), ('app/src/androidTest/java', 'test')]:
        for source in (ROOT / base).rglob('*.java'):
            relative = source.relative_to(ROOT / base)
            target = OUT / 'overlay' / overlay / relative
            if sha(source) != sha(target):
                differences.append(str(Path(overlay) / relative).replace('\\', '/'))
    if sorted(differences) != ['main/dev/minime/ime/AsyncDecoder.java',
                               'test/dev/minime/ime/TouchLatencyTest.java']:
        raise ValueError('Unexpected overlay delta: ' + repr(differences))
    manifest = dict(active_java_sha256=active, active_sources_unchanged=True,
        changed_existing_files=differences, added_main_files=['dev/minime/ime/QueueTrace.java'],
        inputs={str(p.relative_to(ROOT)): sha(p) for p in [producer,
            Path(__file__), ROOT / 'docs/release-hardening/POST-WINDOW-STAGES-PLAN.md',
            ROOT / 'docs/release-hardening/queue-stages/QueueTrace.java',
            ROOT / 'docs/release-hardening/language-timing/inputs.tsv']})
    (OUT / 'combined-source-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print('Prepared current-source overlay; only decoder instrumentation and timing test differ')


if __name__ == '__main__':
    main()
