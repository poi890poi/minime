"""Add cooperative cancellation to a separate audited indexed source copy."""
from pathlib import Path
import difflib, shutil, sys

HERE = Path(__file__).resolve().parent
WORK = HERE.parents[1] / 'artifacts/japanese-engine-benchmark'

def replace(text, old, new):
    assert text.count(old) == 1, (old, text.count(old))
    return text.replace(old, new)

def main():
    sys.path.insert(0, str(HERE.parent / 'japanese-predictive-review'))
    import index_postings
    index_postings.patch()  # Re-verify the upstream pin and reproduce the baseline patch.
    source = WORK / 'kazuma-indexed'
    target = WORK / 'kazuma-bounded'
    if target.exists():
        assert target.resolve().parent == WORK.resolve()
        shutil.rmtree(target)
    shutil.copytree(source, target)
    shutil.copyfile(HERE / 'Budget.hpp', target / 'src/dictionary_builder/louds_builder/Budget.hpp')
    patches = []
    path = 'src/dictionary_builder/louds_builder/louds/louds_utf16_reader.cpp'
    original = (target / path).read_text(encoding='utf8')
    text = '#include "Budget.hpp"\n' + original
    # Only the predictive method and its recursive enumeration are changed.
    begin = text.index('void LOUDSReaderUtf16::collectWords(')
    end = text.index('// Omission-aware common prefix search', begin)
    part = text[begin:end]
    part = replace(part, '    if (pos < 0)', '    PredictionBudget::check();\n    if (pos < 0)')
    part = replace(part, '        const int labelIndex', '        PredictionBudget::check();\n        const int labelIndex')
    part = replace(part, '        n = traverse(n, c);', '        PredictionBudget::check();\n        n = traverse(n, c);\n        PredictionBudget::check();')
    text = text[:begin] + part + text[end:]
    (target / path).write_text(text, encoding='utf8')
    patches.extend(difflib.unified_diff(original.splitlines(True), text.splitlines(True), 'a/'+path, 'b/'+path))
    path = 'cli/kana_kanji/astar_bunsetsu_cli.cpp'
    original = (target / path).read_text(encoding='utf8')
    text = '#include "Budget.hpp"\n' + original
    begin = text.index('static void print_prediction(')
    end = text.index('\nint main(', begin)
    part = text[begin:end]
    part = replace(part, '        if (!starts_with_u16', '        PredictionBudget::check();\n        if (!starts_with_u16')
    part = replace(part, '            std::u16string surface;', '            PredictionBudget::check();\n            std::u16string surface;')
    part = replace(part, '                  if (a.score', '                  PredictionBudget::check();\n                  if (a.score')
    part = replace(part, '    if (rows.empty())', '    PredictionBudget::check();\n    if (rows.empty())')
    text = text[:begin] + part + text[end:]
    (target / path).write_text(text, encoding='utf8')
    patches.extend(difflib.unified_diff(original.splitlines(True), text.splitlines(True), 'a/'+path, 'b/'+path))
    (HERE / 'cooperative-budget.patch').write_text(''.join(patches), encoding='utf8')
    print('Prepared isolated bounded lexical predictor')

if __name__ == '__main__': main()
