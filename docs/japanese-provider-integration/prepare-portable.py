"""One-variable follow-up to the observed desktop/libc++ beam tie difference."""
from pathlib import Path
import shutil,difflib
HERE=Path(__file__).resolve().parent;WORK=HERE.parents[1]/'artifacts/japanese-engine-benchmark'
source=WORK/'kazuma-indexed-stable';target=WORK/'kazuma-portable'
shutil.copytree(source,target,dirs_exist_ok=True)
relative='src/path_algorithm/find_path.cpp';before=(source/relative).read_text(encoding='utf8')
old='std::sort(nodes.begin(), nodes.end(),';assert before.count(old)==1
after=before.replace(old,'std::stable_sort(nodes.begin(), nodes.end(),')
(target/relative).write_text(after,encoding='utf8',newline='\n')
(HERE/'portable-beam.patch').write_text(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),'a/'+relative,'b/'+relative)),encoding='utf8',newline='\n')
print('Prepared stable beam ties; all costs and beam size preserved')
