"""Isolated set-preserving McBopomofo compiler experiment; never edits sources/assets."""
import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
COMPILER = Path('curation/compilers/main_compiler.py')


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def patch(source):
    for tier in (1, 2, 3):
        old = f'bpmf_phon{tier}[elements[0]] = elements[1]'
        new = f'bpmf_phon{tier}.setdefault(elements[0], set()).add(elements[1])'
        assert source.count(old) == 1, 'Unexpected pinned parser'
        source = source.replace(old, new)
        old = f'str(bpmf_phon{tier}[mykey]) == r'
        assert source.count(old) == 1, 'Unexpected pinned membership test'
        source = source.replace(old, f'r in bpmf_phon{tier}[mykey]')
    return source


def run(module, cwd, **arguments):
    command = [sys.executable, '-X', 'utf8', '-m', module]
    for key, value in arguments.items():
        command.extend(['--' + key, str(value.resolve())])
    result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, encoding='utf-8')
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)
    return result.stdout + result.stderr


def compile_model(module_root, inputs, output):
    return run('curation.compilers.main_compiler', module_root,
        heterophony1=inputs / 'heterophony1.list', heterophony2=inputs / 'heterophony2.list',
        heterophony3=inputs / 'heterophony3.list', phrase_freq=inputs / 'PhraseFreq.txt',
        bpmf_mappings=inputs / 'BPMFMappings.txt', bpmf_base=inputs / 'BPMFBase.txt',
        punctuations=inputs / 'BPMFPunctuations.txt', symbols=inputs / 'Symbols.txt',
        macros=inputs / 'Macros.txt', output=output)


def rows(path):
    result = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        reading, rest = line.split(' ', 1)
        word, score = rest.rsplit(' ', 1)
        result[reading, word] = float(score)
    return result


def fixtures(destination, multiple, reverse):
    destination.mkdir(parents=True, exist_ok=True)
    readings = ['ㄅ', 'ㄆ', 'ㄇ', 'ㄈ']
    glyphs = [chr(0x4e00 + i) for i in range(4)]
    base = ''.join(f'{g} {r} a1 _ big5\n' for g in glyphs for r in readings)
    (destination / 'BPMFBase.txt').write_text(base, encoding='utf-8', newline='\n')
    (destination / 'PhraseFreq.txt').write_text(''.join(f'{g} -2.0\n' for g in glyphs), encoding='utf-8', newline='\n')
    for name in ('BPMFMappings.txt', 'BPMFPunctuations.txt', 'Symbols.txt', 'Macros.txt'):
        (destination / name).write_bytes(b'')
    tier_rows = {i: [] for i in (1, 2, 3)}
    for index, g in enumerate(glyphs[:3]):
        for tier in (1, 2, 3):
            selected = [readings[tier - 1]]
            if multiple and tier == index + 1:
                selected.append(readings[3])
            tier_rows[tier].extend(f'{g} {r}' for r in selected)
    for tier, values in tier_rows.items():
        if reverse:
            values.reverse()
        (destination / f'heterophony{tier}.list').write_text('\n'.join(values) + '\n', encoding='utf-8', newline='\n')
    return glyphs, readings


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', type=Path, default=ROOT / 'artifacts/mcbopomofo-audit/upstream')
    parser.add_argument('--output', type=Path, default=ROOT / 'artifacts/priority-parser')
    args = parser.parse_args()
    upstream, out = args.upstream.resolve(), args.output.resolve()
    assert (ROOT / 'artifacts').resolve() in out.parents, 'Output must stay in ignored artifacts'
    assert not out.exists(), 'Use a fresh experiment directory'
    out.mkdir(parents=True)
    expected = json.loads((ROOT / 'docs/chinese-recovery/weights-manifest.json').read_text())
    assert sha(upstream / 'data.txt') == expected['upstream_sha256']
    module_roots = {}
    for variant in ('original', 'sets'):
        target = out / variant
        shutil.copytree(upstream / 'curation', target / 'curation', ignore=shutil.ignore_patterns('__pycache__', '*.pyc'))
        if variant == 'sets':
            (target / COMPILER).write_bytes(patch((upstream / COMPILER).read_text(encoding='utf-8')).encode('utf-8'))
        module_roots[variant] = target

    results = {}
    for multiple in (False, True):
        for reverse in (False, True):
            fixture = out / ('fixture-%s-%s' % (multiple, reverse))
            glyphs, readings = fixtures(fixture, multiple, reverse)
            for variant, module_root in module_roots.items():
                path = fixture / (variant + '.txt')
                compile_model(module_root, fixture, path)
                results[multiple, reverse, variant] = rows(path)
    assert results[False, False, 'original'] == results[False, True, 'original'] == results[False, False, 'sets'] == results[False, True, 'sets']
    old, old_reversed = results[True, False, 'original'], results[True, True, 'original']
    fixed = results[True, False, 'sets']
    assert old != old_reversed, 'Original failure not reproduced'
    assert fixed == results[True, True, 'sets'], 'Set parser remains order dependent'
    for index, glyph in enumerate(glyphs[:3]):
        tier = index + 1
        wanted = -2.0 - (tier - 1) * 0.69314718055994
        for reading in (readings[tier - 1], readings[3]):
            assert abs(fixed[reading, glyph] - wanted) < 0.000001
    summary = dict(type='isolated compiler parser experiment, no production admission',
        fixture_original_order_dependent_rows=sum(old[k] != old_reversed[k] for k in old),
        fixture_fixed_order_invariant=True, fixture_all_same_tier_readings_preserved=True,
        fixture_single_reading_control_identical=True, source_revision=expected['revision'],
        input_hashes={str(p.relative_to(upstream)).replace('\\', '/'): sha(p)
                      for p in sorted(upstream.rglob('*.py')) if '__pycache__' not in str(p)}, variants={})
    for name in ('heterophony1.list', 'heterophony2.list', 'heterophony3.list', 'PhraseFreq.txt',
                 'BPMFMappings.txt', 'BPMFBase.txt', 'BPMFPunctuations.txt', 'Symbols.txt', 'Macros.txt', 'Postprocess.txt'):
        summary['input_hashes'][name] = sha(upstream / name)
    for variant, module_root in module_roots.items():
        raw, final = module_root / 'data-raw.txt', module_root / 'data.txt'
        log = compile_model(module_root, upstream, raw)
        log += run('curation.compilers.postprocess', module_root,
                   input=raw, directive=upstream / 'Postprocess.txt', output=final)
        # Preserve the pinned build's actual bytes, including native line endings.
        (module_root / 'compile.log').write_text(log, encoding='utf-8')
        if variant == 'original':
            assert sha(final) == expected['upstream_sha256'], 'Original rebuild does not reproduce pinned model'
        summary['variants'][variant] = dict(compiler_sha256=sha(module_root / COMPILER), raw_sha256=sha(raw), final_sha256=sha(final))
    old, fixed = rows(module_roots['original'] / 'data.txt'), rows(module_roots['sets'] / 'data.txt')
    assert old.keys() == fixed.keys(), 'Changed source identities'
    changes = [dict(reading=k[0], text=k[1], before=old[k], after=fixed[k]) for k in sorted(old) if old[k] != fixed[k]]
    (out / 'changes.json').write_bytes((json.dumps(changes, ensure_ascii=False, indent=2) + '\n').encode())
    summary['model_rows'] = len(old)
    summary['changed_model_rows'] = len(changes)
    summary['changed_multi_glyph_rows'] = sum(len(r['text']) > 1 for r in changes)
    summary['changes_sha256'] = sha(out / 'changes.json')
    (out / 'summary.json').write_bytes((json.dumps(summary, ensure_ascii=False, indent=2) + '\n').encode())
    print(json.dumps({k: v for k, v in summary.items() if k != 'input_hashes'}, indent=2))


if __name__ == '__main__':
    main()
