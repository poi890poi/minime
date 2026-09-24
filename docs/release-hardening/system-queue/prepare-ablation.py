"""Diagnostic only: suppress composition popup; preserve every other source byte."""
import hashlib
import json
import shutil
from pathlib import Path

root = Path(__file__).resolve().parents[3]
baseline = root / 'artifacts/system-queue'
out = root / 'artifacts/annotation-ablation'
if out.exists():
    raise ValueError('Refusing to overwrite an experiment')
out.mkdir()
shutil.copytree(baseline / 'overlay', out / 'overlay')
path = out / 'overlay/main/dev/minime/ime/KeyboardView.java'
original = path.read_text(encoding='utf-8')
anchor = '    private void placeAnnotation() {\n'
if original.count(anchor) != 1:
    raise ValueError('Annotation owner changed')
changed = original.replace(anchor,
    '    private boolean diagnosticSuppressAnnotation() {return true;}\n' + anchor +
    '        if(diagnosticSuppressAnnotation()){annotationWindow.dismiss();return;}\n', 1)
path.write_text(changed, encoding='utf-8')
init = (baseline / 'diagnostic.init.gradle').read_text(encoding='utf-8')
(out / 'diagnostic.init.gradle').write_text(init.replace('artifacts/system-queue/overlay/', 'artifacts/annotation-ablation/overlay/'), encoding='utf-8')
differences = []
for source in (baseline / 'overlay').rglob('*'):
    if source.is_file():
        relative = source.relative_to(baseline / 'overlay')
        if source.read_bytes() != (out / 'overlay' / relative).read_bytes():
            differences.append(relative.as_posix())
assert differences == ['main/dev/minime/ime/KeyboardView.java'], differences
pins = dict(source_changes=differences, production_unchanged=True,
            files={str(p.relative_to(root)).replace('\\', '/'): hashlib.sha256(p.read_bytes()).hexdigest()
                   for p in [path, baseline / 'overlay/main/dev/minime/ime/KeyboardView.java',
                             root / 'docs/release-hardening/ANNOTATION-ABLATION-PLAN.md']})
(out / 'source-manifest.json').write_text(json.dumps(pins, indent=2) + '\n', encoding='utf-8')
print(json.dumps(pins, indent=2))
