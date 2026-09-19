"""Isolated mode-priority experiment; never edit production source."""
from pathlib import Path
import difflib, hashlib, json

root = Path(__file__).resolve().parents[1]
source = root / 'core/src/main/java/dev/minime/core/CompositionEngine.java'
original = source.read_text(encoding='utf-8')
needle = '''            for(int i=0;i<Math.max(latin.size(),han.size());i++) {
                if(i<latin.size())candidates.add(latin.get(i));
                if(i<han.size())candidates.add(han.get(i));
            }'''
replacement = '''            boolean chineseFirst=inputMode==InputMode.CHINESE && !afterLatin
                && dictionary!=null && !dictionary.isEnglish(raw,false);
            for(int i=0;i<Math.max(latin.size(),han.size());i++) {
                if(chineseFirst && i<han.size())candidates.add(han.get(i));
                if(i<latin.size())candidates.add(latin.get(i));
                if(!chineseFirst && i<han.size())candidates.add(han.get(i));
            }'''
assert original.count(needle) == 1
changed = original.replace(needle, replacement)
out = root / 'artifacts/candidate-usefulness/chinese-first-src'
out.mkdir(exist_ok=True)
(out / 'CompositionEngine.java').write_text(changed, encoding='utf-8')
evidence = root / 'docs/candidate-usefulness'
(evidence / 'chinese-first.patch').write_text(''.join(difflib.unified_diff(
    original.splitlines(True), changed.splitlines(True), fromfile='a/core/src/main/java/dev/minime/core/CompositionEngine.java',
    tofile='b/core/src/main/java/dev/minime/core/CompositionEngine.java')), encoding='utf-8')
(evidence / 'chinese-first-manifest.json').write_text(json.dumps(dict(
    baseline='9d3f650 runtime', original_sha256=hashlib.sha256(original.encode()).hexdigest(),
    experiment_sha256=hashlib.sha256(changed.encode()).hexdigest(),
    decision='Experimental class override only; evaluate Chinese and English before landing.'), indent=2)+'\n', encoding='utf-8')
